package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 知识库数据集（kb_dataset）：顶层容器，持有嵌入模型/向量库/检索配置。
 *
 * @see com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.KnowledgePluginConfiguration
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_dataset")
public class KbDataset extends MpBaseEntity<String> {

    /** 数据集名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 索引技术：high_quality/economy（economy 预留） */
    private String indexingTechnique;

    /** 配置的嵌入模型资源 key（langchain4j embedding-model） */
    private String embeddingModelKey;

    /** 上次成功索引实际使用的嵌入模型 key；检索嵌入用此值保证与向量一致；切换配置后提示重建索引，重建完成后更新 */
    private String embeddingModelActive;

    /** 向量库资源 key（langchain4j embedding-store） */
    private String embeddingStoreKey;

    /** rerank 模型资源 key（scoring-model），空则不重排 */
    private String scoringModelKey;

    /** 向量库 collection 名，按数据集隔离；空则按约定生成 */
    private String collectionName;

    /** 默认召回数量 */
    private Integer retrievalTopK;

    /** 默认分数阈值 */
    private BigDecimal retrievalScoreThreshold;

    /** 分段最大长度（字符） */
    private Integer segmentMaxLength;

    /** 分段重叠长度（字符） */
    private Integer segmentOverlap;

    /** 自定义分段分隔符，空则仅按递归规则 */
    private String segmentDelimiter;

    /** 分段模式：general 通用（分段直接嵌入）/parent-child 父子（父段落表不嵌入，子段嵌入带 parent_id）；qa 预留 */
    private String segmentMode;

    /** 状态：active/disabled */
    private String status;

    /** 最近错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 逻辑删除：0 否 1 是 */
    private String delFlag;
}
