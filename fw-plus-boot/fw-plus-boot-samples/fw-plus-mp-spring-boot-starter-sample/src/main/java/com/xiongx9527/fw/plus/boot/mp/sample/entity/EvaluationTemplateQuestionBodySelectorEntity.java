package com.xiongx9527.fw.plus.boot.mp.sample.entity;
import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 评估模板题干选项表
 * </p>
 *
 * @author yanli
 * @since 2025-06-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("evaluation_template_question_body_selector")
@Schema(description="评估模板题干选项表")
@JsonIgnoreProperties(value = {"createTime","updateTime"},allowSetters = true)
public class EvaluationTemplateQuestionBodySelectorEntity extends MpBaseEntity  {

    @Schema(description = "评估模板id")
    private Long evaluationTemplateId;

    @Schema(description = "模板题干id")
    private Long evaluationTemplateQuestionBodyId;

    @Schema(description = "题干id")
    private Long evaluationQuestionBodyId;

    @Schema(description = "选项id")
    private Long evaluationQuestionBodySelectorId;

    @Schema(description = "排序, 升序排列")
    private Long sort;

    @Schema(description = "得分")
    private BigDecimal score;

    @Schema(description = "选项是否只能单独被选")
    private Boolean chooseOnlyFlag;

    @Schema(description = "配置")
    private String config;

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
