package com.school.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.teaching.entity.JwtBlacklist;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JwtBlacklistMapper extends BaseMapper<JwtBlacklist> {

    @Select("SELECT COUNT(*) FROM jwt_blacklist WHERE jti = #{jti} AND expires_at > NOW()")
    int existsByJti(@Param("jti") String jti);

    @Delete("DELETE FROM jwt_blacklist WHERE expires_at <= NOW()")
    int deleteExpired();
}
