package com.school.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.teaching.entity.QuestionBank;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface QuestionBankMapper extends BaseMapper<QuestionBank> {

    @Update("UPDATE question_bank SET question_text = #{questionText} WHERE id = #{id}")
    int updateQuestionText(@Param("id") Long id, @Param("questionText") String questionText);
}
