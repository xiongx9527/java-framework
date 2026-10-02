package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 知识库分段（kb_segment）：检索最小单元，id 同时作为向量库 vector id。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_segment")
public class KbSegment extends MpBaseEntity<String> {

    /** 所属数据集 id */
    private String datasetId;

    /** 所属文档 id */
    private String documentId;

    /** 分段序号 */
    private Integer position;

    /** 分段文本（持久化，重嵌入无需重解析原文） */
    private String content;

    /** QA 模式的答案；通用/父子分段为空 */
    private String answer;

    /** 分段关键词（逗号分隔，管理端可编辑） */
    private String keywords;

    /** 词数（近似） */
    private Integer wordCount;

    /** token 估计值（KbTokenCounter 字符近似） */
    private Integer tokens;

    /** 向量库节点 id（嵌入成功后回填） */
    private String indexNodeId;

    /** 嵌入内容 hash（幂等重建去重） */
    private String indexNodeHash;

    /** 分段状态：waiting/indexing/completed/error */
    private String status;

    /** 启用：1 是 0 否（禁用不参与检索） */
    private String enabled;

    /** 父子分段：父分段 id；父分段自身为 NULL（父段落表不嵌入） */
    private String parentId;

    /** 召回命中计数 */
    private Long hitCount;

    /** 嵌入失败原因 */
    private String errorMsg;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 全文检索相关度（非表字段；FULLTEXT MATCH AGAINST 返回，调用方归一化为 0~1 分数） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Double relevance;
}
