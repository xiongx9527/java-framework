package com.xiongx9527.fw.plus.boot.mp.sample.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
/**
 * <p>
 * 评估指标选项表
 * </p>
 *
 * @author yanli
 * @since 2025-06-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("evaluation_question_body_selector")
@Schema(description="评估指标选项表")
@JsonIgnoreProperties(value = {"createTime","updateTime"},allowSetters = true)
public class EvaluationQuestionBodySelectorEntity extends MpBaseEntity  {

    @Schema(description = "选项code")
    private String code;

    @Schema(description = "指标id")
    private Long evaluationQuestionBodyId;

    @Schema(description = "父答案id")
    private Long parentId;

    @Schema(description = "答案内容")
    private String answerContent;

    @Schema(description = "子选项题干id")
    private Long childrenEvaluationQuestionBodyId;

    @Schema(description = "排序")
    private Long sort;

    @Schema(description = "选项是否只能单独被选")
    private Boolean chooseOnlyFlag;

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
