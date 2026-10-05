package com.student.platform.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.qa.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会话Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 查询用户的所有活跃会话（按更新时间倒序）
     */
    @Select("SELECT * FROM conversation WHERE user_id = #{userId} AND status = 'ACTIVE' ORDER BY updated_at DESC")
    List<Conversation> selectActiveConversationsByUserId(@Param("userId") Long userId);

    /**
     * 根据sessionId查询会话
     */
    @Select("SELECT * FROM conversation WHERE session_id = #{sessionId} AND status = 'ACTIVE'")
    Conversation selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据sessionId和userId查询会话（权限验证）
     */
    @Select("SELECT * FROM conversation WHERE session_id = #{sessionId} AND user_id = #{userId} AND status = 'ACTIVE'")
    Conversation selectBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    /**
     * 统计用户的活跃会话数量
     */
    @Select("SELECT COUNT(*) FROM conversation WHERE user_id = #{userId} AND status = 'ACTIVE'")
    int countActiveConversations(@Param("userId") Long userId);
}