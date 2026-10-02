package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 知识库文档（kb_document）：一次上传/文本入库的载体，携带索引状态机与原始文件引用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document")
public class KbDocument extends MpBaseEntity<String> {

    /** 所属数据集 id */
    private String datasetId;

    /** 文档名称（原始文件名） */
    private String name;

    /** 原始文件存储路径（KbOriginalFileStore 落盘后回写；create-by-text 时为空） */
    private String filePath;

    /** 文件扩展名 */
    private String fileExt;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 来源：upload/create-by-text */
    private String dataSourceType;

    /** 词数（近似） */
    private Integer wordCount;

    /** 索引耗时（毫秒） */
    private Long latencyMs;

    /** 索引状态：waiting/parsing/splitting/indexing/completed/error */
    private String indexingStatus;

    /** 索引失败原因 */
    private String errorMsg;

    /** 启用：1 是 0 否（禁用不参与检索） */
    private String enabled;

    /** 归档：1 是 0 否（归档不参与检索，与启禁用独立） */
    private String archived;

    /** 文档自定义元数据 JSON（键值对，检索过滤用） */
    private String metadata;

    /** 内容 SHA-256，数据集内去重 */
    private String docHash;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
