package com.school.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.teaching.entity.SignRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SignRecordMapper extends BaseMapper<SignRecord> {

    @Select("SELECT * FROM sign_records WHERE student_id = #{studentId} ORDER BY sign_date DESC LIMIT 1")
    SignRecord findLastSign(@Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM sign_records WHERE student_id = #{studentId} AND sign_date = CURDATE()")
    int isSignedToday(@Param("studentId") Long studentId);

    @Select("SELECT * FROM sign_records WHERE student_id = #{studentId} AND sign_date >= #{startDate} AND sign_date <= #{endDate} ORDER BY sign_date DESC")
    List<SignRecord> getSignRecordsByRange(@Param("studentId") Long studentId,
                                                                       @Param("startDate") String startDate,
                                                                       @Param("endDate") String endDate);
}
