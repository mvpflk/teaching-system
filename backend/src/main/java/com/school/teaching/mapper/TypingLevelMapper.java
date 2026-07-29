package com.school.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.teaching.entity.TypingLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TypingLevelMapper extends BaseMapper<TypingLevel> {

    /** 原子增加经验值 */
    @Update("UPDATE typing_levels SET exp = exp + #{exp}, updated_at = NOW() WHERE student_id = #{studentId}")
    int addExpAtomic(@Param("studentId") Long studentId, @Param("exp") int exp);

    /** 行级锁查询（配合 @Transactional 使用，防止并发升级竞态） */
    @org.apache.ibatis.annotations.Select("SELECT * FROM typing_levels WHERE student_id = #{studentId} FOR UPDATE")
    TypingLevel selectForUpdate(@Param("studentId") Long studentId);
}
