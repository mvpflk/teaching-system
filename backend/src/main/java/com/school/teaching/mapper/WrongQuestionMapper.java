package com.school.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.teaching.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    /** 批量 upsert：同 (student_id, question_id) 则 wrong_count + 1 */
    int batchUpsert(java.util.List<WrongQuestion> list);
}
