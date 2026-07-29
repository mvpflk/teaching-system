package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.dto.RubricDetailDTO;
import com.school.teaching.dto.RubricScoreDTO;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.RubricScoreMapper;
import com.school.teaching.mapper.TaskSubmissionMapper;
import com.school.teaching.service.RubricScoringService;
import com.school.teaching.service.RubricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RubricScoringServiceImpl implements RubricScoringService {

    private static final Logger log = LoggerFactory.getLogger(RubricScoringServiceImpl.class);
    private static final ObjectMapper om = new ObjectMapper();

    private final RubricService rubricService;
    private final RubricScoreMapper rubricScoreMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final com.school.teaching.mapper.TaskMapper taskMapper;

    public RubricScoringServiceImpl(RubricService rubricService, RubricScoreMapper rubricScoreMapper,
                                     TaskSubmissionMapper submissionMapper, com.school.teaching.mapper.TaskMapper taskMapper) {
        this.rubricService = rubricService;
        this.rubricScoreMapper = rubricScoreMapper;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
    }

    @Override
    public RubricDetailDTO getTaskRubric(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getRubricId() == null)
            throw new BusinessException(404, "该任务未绑定评分量规");
        Rubric rubric = rubricService.getById(task.getRubricId());
        if (rubric == null) throw new BusinessException(404, "评分量规不存在");
        List<RubricDimension> dimensions = rubricService.getDimensions(rubric.getId());
        if (dimensions.isEmpty()) throw new BusinessException(404, "该量规未配置评分维度");

        RubricDetailDTO dto = new RubricDetailDTO();
        dto.setRubricId(rubric.getId());
        dto.setRubricName(rubric.getName());
        List<RubricDetailDTO.Dimension> dimList = new ArrayList<>();
        for (RubricDimension dim : dimensions) {
            RubricDetailDTO.Dimension d = new RubricDetailDTO.Dimension();
            d.setDimensionId(dim.getId());
            d.setName(dim.getName());
            d.setWeight(dim.getWeight());
            d.setDescription(dim.getDescription());
            d.setLevels(parseLevels(dim.getLevelsJson()));
            dimList.add(d);
        }
        dto.setDimensions(dimList);
        return dto;
    }

    private List<RubricDetailDTO.Level> parseLevels(String levelsJson) {
        if (levelsJson == null || levelsJson.isBlank()) return List.of();
        try {
            return om.readValue(levelsJson, new TypeReference<List<RubricDetailDTO.Level>>() {});
        } catch (Exception e) {
            log.warn("量规等级JSON解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    @Transactional
    public RubricScoreDTO saveRubricScores(Long submissionId, RubricScoreDTO scores) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交记录不存在");
        Rubric rubric = rubricService.getById(scores.getRubricId());
        if (rubric == null) throw new BusinessException(404, "评分量规不存在");
        List<RubricDimension> dimensions = rubricService.getDimensions(rubric.getId());

        Map<Long, RubricDimension> dimMap = new HashMap<>();
        for (RubricDimension d : dimensions) dimMap.put(d.getId(), d);

        Map<Long, Integer> maxLevelMap = new HashMap<>();
        for (RubricDimension d : dimensions) {
            List<RubricDetailDTO.Level> levels = parseLevels(d.getLevelsJson());
            maxLevelMap.put(d.getId(), levels.stream().mapToInt(RubricDetailDTO.Level::getLevel).max().orElse(4));
        }

        if (scores.getDimensions() == null || scores.getDimensions().isEmpty())
            throw new BusinessException(400, "请提供至少一个维度的评分");

        LambdaQueryWrapper<RubricScore> delW = new LambdaQueryWrapper<>();
        delW.eq(RubricScore::getSubmissionId, submissionId);
        rubricScoreMapper.delete(delW);

        BigDecimal maxPerDim = new BigDecimal("10");
        BigDecimal totalScore = BigDecimal.ZERO;
        List<RubricScoreDTO.DimensionScore> resultDims = new ArrayList<>();

        for (RubricScoreDTO.DimensionScore ds : scores.getDimensions()) {
            RubricDimension dim = dimMap.get(ds.getDimensionId());
            if (dim == null) throw new BusinessException(400, "无效的维度ID: " + ds.getDimensionId());
            int maxLvl = maxLevelMap.getOrDefault(ds.getDimensionId(), 4);
            if (ds.getLevel() < 0 || ds.getLevel() > maxLvl)
                throw new BusinessException(400, String.format("维度[%s]等级%d超出范围0-%d", dim.getName(), ds.getLevel(), maxLvl));

            BigDecimal weight = dim.getWeight() != null ? dim.getWeight() : BigDecimal.ZERO;
            BigDecimal dimScore = maxPerDim.multiply(weight).multiply(BigDecimal.valueOf(ds.getLevel()))
                .divide(BigDecimal.valueOf(maxLvl), 2, RoundingMode.HALF_UP);

            RubricScore rs = new RubricScore();
            rs.setSubmissionId(submissionId);
            rs.setRubricId(scores.getRubricId());
            rs.setDimensionId(ds.getDimensionId());
            rs.setLevel(ds.getLevel());
            rs.setScore(dimScore);
            rs.setComment(ds.getComment());
            rubricScoreMapper.insert(rs);

            totalScore = totalScore.add(dimScore);
            ds.setDimensionName(dim.getName());
            ds.setWeight(weight);
            ds.setScore(dimScore);
            resultDims.add(ds);
        }

        sub.setRubricTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
        sub.setRubricScoredAt(LocalDateTime.now());
        submissionMapper.updateById(sub);

        scores.setDimensions(resultDims);
        scores.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
        return scores;
    }
}
