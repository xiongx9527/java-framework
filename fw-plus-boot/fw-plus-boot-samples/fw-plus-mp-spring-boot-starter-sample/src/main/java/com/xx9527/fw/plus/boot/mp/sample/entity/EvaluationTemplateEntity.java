package com.xx9527.fw.plus.boot.mp.sample.entity;
import com.xx9527.fw.plus.mp.base.MpBaseEntity;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 评估模板信息表
 * </p>
 *
 * @author yanli
 * @since 2025-06-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("evaluation_template")
@Schema(description="评估模板信息表")
@JsonIgnoreProperties(value = {"createTime","updateTime"},allowSetters = true)
public class EvaluationTemplateEntity extends MpBaseEntity  {

    @Schema(description = "id")
    @TableId(value = "id")
    private Long id;

    @Schema(description = "评估类型,1: 老年人能力评估")
    private Integer type;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板code")
    private String templateCode;

    @Schema(description = "模板总分")
    private BigDecimal score;

    @Schema(description = "Ai类型；待定")
    private String aiType;

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
