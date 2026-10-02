package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 知识库元数据字段定义（kb_metadata_field）：数据集级 metadata 字段 schema，
 * 文档 metadata 赋值前需先定义字段（对齐 Dify 元数据字段管理）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_metadata_field")
public class KbMetadataField extends MpBaseEntity<String> {

    /** 所属数据集 id */
    private String datasetId;

    /** 字段名（数据集内唯一） */
    private String name;

    /** 字段类型：string/number/time */
    private String type;

    /** 字段说明 */
    private String description;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
