package com.xx9527.fw.plus.boot.mp.sample.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

import com.xx9527.fw.plus.mp.base.MpBaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 评估题干表
 * </p>
 *
 * @author yanli
 * @since 2025-06-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("evaluation_question_body")
@Schema(description="评估题干表")
@JsonIgnoreProperties(value = {"createTime","updateTime"},allowSetters = true)
public class EvaluationQuestionBodyEntity extends MpBaseEntity  {

    @Schema(description = "评估题干名称")
    private String name;

    @Schema(description = "编号")
    private String code;

    @Schema(description = "评估题干类型 ： 1：选择题 , 2: 填空题")
    private Integer type;

    @Schema(description = "题干-指标jsonPath")
    private String jsonPath ;

    @Schema(description = "是否多选，当题干类型为选择题的时候生效，默认单选")
    private Boolean multiSelectFlag;

    @Schema(description = "父题干id")
    private Long parentId;

    @Schema(description = "下一道子题干ID")
    private Long nextEvaluationQuestionBodyId;

    @Schema(description = "1：已删除，0：未删除")
    private Boolean deleteFlag;

    @Schema(description = "排序")
    private Long sort;

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
