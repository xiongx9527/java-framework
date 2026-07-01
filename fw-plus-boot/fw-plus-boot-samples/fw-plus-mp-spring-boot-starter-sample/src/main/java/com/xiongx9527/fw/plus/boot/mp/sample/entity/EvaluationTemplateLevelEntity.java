package com.xiongx9527.fw.plus.boot.mp.sample.entity;

import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <p>
 * 评估模板等级表
 * </p>
 *
 * @author yanli
 * @since 2025-06-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("evaluation_template_level")
@Schema(description="评估模板等级表")
@JsonIgnoreProperties(value = {"createTime","updateTime"},allowSetters = true)
public class EvaluationTemplateLevelEntity extends MpBaseEntity   {

    @Schema(description = "模板id")
    private Long evaluationTemplateId;

    @Schema(description = "最小得分")
    private BigDecimal minScore;

    @Schema(description = "最大得分")
    private BigDecimal maxScore;

    @Schema(description = "等级名称")
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
