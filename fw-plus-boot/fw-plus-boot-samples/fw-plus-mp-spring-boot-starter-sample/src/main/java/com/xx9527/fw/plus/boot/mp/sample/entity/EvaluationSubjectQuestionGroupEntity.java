package com.xx9527.fw.plus.boot.mp.sample.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.xx9527.fw.plus.mp.base.MpBaseEntity;
/**
 * <p>
 * 科目题目分组
 * </p>
 *
 * @author yanli
 * @since 2025-06-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("evaluation_subject_question_group")
@Schema(description="科目题目分组")
@JsonIgnoreProperties(value = {"createTime","updateTime"},allowSetters = true)
public class EvaluationSubjectQuestionGroupEntity extends MpBaseEntity  {

    @Schema(description = "科目id")
    private Long evaluationSubjectId;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "1：已删除，0：未删除")
    private Boolean deleteFlag;

    private Date deleteTime;

    @Schema(description = "删除人")
    private String deleteUser;

    private Date updateTime;

    @Schema(description = "创建人")
    private String createUser;

    private Date createTime;

    @Schema(description = "修改人")
    private String updateUser;


}
