package com.student.platform.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.qa.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 消息Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 根据会话ID查询所有消息（按创建时间正序）
     */
    @Select("SELECT * FROM message WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<Message> selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID分页查询消息（按创建时间正序）
     */
    @Select("SELECT * FROM message WHERE session_id = #{sessionId} ORDER BY created_at ASC LIMIT #{offset}, #{limit}")
    List<Message> selectBySessionIdWithPagination(@Param("sessionId") String sessionId, 
                                                  @Param("offset") int offset, 
                                                  @Param("limit") int limit);

    /**
     * 根据会话ID统计消息数量
     */
    @Select("SELECT COUNT(*) FROM message WHERE session_id = #{sessionId}")
    int countBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID删除所有消息（物理删除）
     */
    @Update("DELETE FROM message WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 查询会话的第一条消息
     */
    @Select("SELECT * FROM message WHERE session_id = #{sessionId} ORDER BY created_at ASC LIMIT 1")
    Message selectFirstBySessionId(@Param("sessionId") String sessionId);

    /**
     * 查询会话的最后一条消息
     */
    @Select("SELECT * FROM message WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT 1")
    Message selectLastBySessionId(@Param("sessionId") String sessionId);
}