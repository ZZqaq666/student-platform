package com.student.platform.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.learning.entity.Note;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习笔记 Mapper
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
