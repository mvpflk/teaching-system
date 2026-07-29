package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.service.TrainingAiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TrainingAiServiceImpl implements TrainingAiService {
    private static final Logger log = LoggerFactory.getLogger(TrainingAiServiceImpl.class);
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private com.school.teaching.service.impl.DeepSeekGateway deepSeekGateway;

    @Override
    public List<Map<String, Object>> generateSteps(Long userId, String subject, List<Long> nodeIds, int stepCount) {
        // 查询知识节点获取内容
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectBatchIds(nodeIds);
        if (nodes.isEmpty()) {
            return buildDefaultSteps(subject, stepCount);
        }

        // 构建节点内容摘要
        StringBuilder nodeContent = new StringBuilder();
        for (KnowledgeNode node : nodes) {
            nodeContent.append("- ").append(node.getName());
            if (node.getContent() != null && !node.getContent().isBlank()) {
                String content = node.getContent();
                // 截取内容前200字符作为摘要
                if (content.length() > 200) content = content.substring(0, 200) + "...";
                nodeContent.append(": ").append(content);
            }
            nodeContent.append("\n");
        }

        // 尝试通过 DeepSeek AI 生成步骤
        if (deepSeekGateway != null) {
            try {
                String aiResult = deepSeekGateway.generateContent(Map.of(
                    "subject", subject,
                    "prompt", buildStepGenPrompt(subject, nodeContent.toString(), stepCount),
                    "maxTokens", 2000,
                    "temperature", 0.7,
                    "stageHint", "职高"
                ));
                if (aiResult != null && !aiResult.isBlank()) {
                    List<Map<String, Object>> parsed = parseAiSteps(aiResult, stepCount);
                    if (!parsed.isEmpty()) return parsed;
                }
            } catch (Exception e) {
                log.warn("AI步骤生成失败，回退到默认模板: {}", e.getMessage());
            }
        }

        // 回退到默认模板
        return buildDefaultSteps(subject, Math.min(stepCount, 5));
    }

    /** 构建 AI 生成实训步骤的 Prompt */
    private String buildStepGenPrompt(String subject, String nodeSummary, int stepCount) {
        return String.format("""
            你是%s课程的实训教师。请根据以下知识点，设计%d个实训步骤。
            每个步骤包含：步骤标题(title)、步骤类型(type)、步骤说明(description)、满分值(scoreMax)。

            步骤类型可选：text(文字论述)、file(文件提交)、sim(仿真操作)、choice(选择题)、
            office(Word文档)、excel(Excel表格)、ppt(PPT演示)、web(网页制作)、sql(SQL查询)。

            知识点：
            %s

            请以JSON数组格式返回，每个元素包含 title/type/description/scoreMax 字段。
            示例：[{"title":"步骤标题","type":"text","description":"步骤说明","scoreMax":20}]
            只返回JSON数组，不要其他文字。""",
            subject, stepCount, nodeSummary);
    }

    /** 解析 AI 返回的步骤 JSON */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAiSteps(String aiResult, int maxSteps) {
        try {
            // 提取 JSON 数组
            String json = aiResult.trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) json = json.substring(start, end + 1);

            List<Map<String, Object>> steps = objectMapper.readValue(json, List.class);
            List<Map<String, Object>> result = new ArrayList<>();
            Set<String> validTypes = Set.of("text", "file", "sim", "choice", "office", "excel", "ppt", "web", "sql");

            for (int i = 0; i < Math.min(steps.size(), maxSteps); i++) {
                Map<String, Object> s = new LinkedHashMap<>(steps.get(i));
                String type = String.valueOf(s.getOrDefault("type", "text"));
                if (!validTypes.contains(type)) s.put("type", "text");
                s.putIfAbsent("scoreMax", 20);
                s.putIfAbsent("description", "");
                s.putIfAbsent("title", "步骤" + (i + 1));
                result.add(s);
            }
            return result;
        } catch (Exception e) {
            log.warn("AI步骤JSON解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> importFromText(Long userId, String text) {
        if (text == null || text.isBlank()) return List.of();

        // 按空行或序号分割文本，每段作为一个步骤
        String[] parts = text.split("\\n\\s*\\n|(?=\\n\\d+[.、)])");
        List<Map<String, Object>> steps = new ArrayList<>();

        for (int i = 0; i < parts.length && i < 10; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;

            Map<String, Object> step = new LinkedHashMap<>();
            step.put("title", extractTitle(part));
            step.put("type", "text");
            step.put("description", part);
            step.put("score", Map.of("method", "manual", "max", 20));
            steps.add(step);
        }

        return steps;
    }

    private List<Map<String, Object>> buildDefaultSteps(String subject, int count) {
        List<Map<String, Object>> steps = new ArrayList<>();
        String[] defaultTitles;
        if (subject.contains("网络")) {
            defaultTitles = new String[]{"认识网络概念", "理解协议原理", "配置网络参数", "验证连通性", "总结网络架构"};
        } else if (subject.contains("办公") || subject.contains("Office")) {
            defaultTitles = new String[]{"创建文档", "设置格式", "使用核心功能", "检查与修正", "总结操作要点"};
        } else if (subject.contains("Access") || subject.contains("数据库")) {
            defaultTitles = new String[]{"分析数据需求", "设计表结构", "编写SQL语句", "验证查询结果", "总结数据库设计"};
        } else if (subject.contains("信息")) {
            defaultTitles = new String[]{"认识硬件组成", "理解系统原理", "执行操作步骤", "验证操作结果", "总结知识点"};
        } else {
            defaultTitles = new String[]{"任务分析", "方案设计", "操作实施", "结果验证", "总结反思"};
        }
        for (int i = 0; i < Math.min(count, defaultTitles.length); i++) {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("title", defaultTitles[i]);
            step.put("type", i == 2 ? "file" : "text");
            step.put("description", subject + " - " + defaultTitles[i]);
            step.put("score", Map.of("method", "manual", "max", 20));
            steps.add(step);
        }
        return steps;
    }

    private String extractTitle(String text) {
        String firstLine = text.split("\\n")[0].trim();
        // 移除序号前缀: "1. "、"1、"、"1)"、"一、"
        firstLine = firstLine.replaceFirst("^[\\d一二三四五六七八九十]+[.、)）]\\s*", "");
        if (firstLine.length() > 30) firstLine = firstLine.substring(0, 30) + "...";
        return firstLine;
    }
}
