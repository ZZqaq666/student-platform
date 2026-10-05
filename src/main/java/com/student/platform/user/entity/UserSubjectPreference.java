package com.student.platform.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_subject_preference")
public class UserSubjectPreference {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String subjectId;

    private String subjectName;

    private Integer preferenceScore;

    private Integer answerCount;

    private Integer correctCount;
}
