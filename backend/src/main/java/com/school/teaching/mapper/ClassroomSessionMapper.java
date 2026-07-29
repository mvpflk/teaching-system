package com.school.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.teaching.entity.ClassroomSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClassroomSessionMapper extends BaseMapper<ClassroomSession> {

    /**
     * 原子投票：使用 MySQL JSON_SET 在数据库层完成计票 +1，
     * 避免 Java 层 read-modify-write 的并发丢失问题。
     * 返回受影响行数（0 表示会话已关闭或不存在）。
     */
    int incrementPollVote(@Param("sessionId") Long sessionId,
                          @Param("optionIndex") int optionIndex);
}
