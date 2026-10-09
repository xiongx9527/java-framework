package com.xiongx9527.fw.plus.dify.dataset;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.dify.dataset.entity.*;
import com.xiongx9527.fw.plus.dify.dataset.params.ParamUpdateDataset;
import com.xiongx9527.fw.plus.dify.dataset.types.*;
import com.xiongx9527.fw.plus.dify.SimpleHttpClient;
import com.xiongx9527.fw.plus.dify.dataset.heros.DatasetHero;
import com.xiongx9527.fw.plus.dify.dataset.heros.DocumentHero;
import com.xiongx9527.fw.plus.dify.dataset.params.ParamDataset;
import com.xiongx9527.fw.plus.dify.dataset.params.ParamDocument;
import com.xiongx9527.fw.plus.dify.types.DifyPage;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.io.File;
import java.util.List;

public interface IDatasetClient {
    /**
     * 获取一个SimpleHttpClient对象
     */
    SimpleHttpClient httpClient();

    /**
     * 快捷构建一个Dataset对象
     * @param datasetId 知识库ID
     */
    DatasetHero ofDataset(String datasetId);

    /**
     * 快捷创建一个Document对象
     * @param documentId 文档ID
     */
    DocumentHero ofDocument(String datasetId, String documentId);

    /**
     * 获取嵌入模型列表
     */
    List<JSONObject> selectTextEmbedding();

    /**
     * 查询知识库列表
     * @param page 页码
     * @param limit 返回条数，默认 20，范围 1-100
     */
    DifyPage<Dataset> list(int page, int limit);

    /**
     * 查询知识库列表（带筛选）
     * <p>对应 GET /datasets；使用限定范围的知识库 API 密钥时该接口返回 403 forbidden
     * （路径不含 dataset_id，需可访问所有知识库的密钥）。</p>
     *
     * @param page      页码（默认 1）
     * @param limit     每页条目数（默认 20，服务器上限 100）
     * @param keyword   按名称筛选的搜索关键词（可选）
     * @param includeAll 是否包含所有知识库，无论权限如何（可选，默认 false）
     * @param tagIds    用于筛选的标签 ID 列表（可选）
     * @return DifyPage<Dataset> 分页知识库列表（data/has_more/limit/page/total）
     */
    DifyPage<Dataset> listDatasets(int page, int limit, String keyword, Boolean includeAll, List<String> tagIds);

    /**
     * Create an Empty Knowledge Base. 创建空知识库
     * @param data ParamDataset
     */
    DatasetHero create(ParamDataset data);

    /**
     * 查看知识库详情
     * @param datasetId 知识库ID
     */
    DatasetHero get(String datasetId);

    /**
     * 修改知识库详情
     * @param datasetId 知识库ID
     * @param data ParamUpdateDataset
     */
    DatasetHero update(String datasetId, ParamUpdateDataset data);

    /**
     * 删除知识库
     * @param datasetId 知识库ID
     */
    void delete(String datasetId);

    /**
     * 通过文本创建文档
     * @param datasetId 知识库ID
     * @param doc ParamDocument
     */
    DocumentResult insertDocByText(String datasetId, ParamDocument doc) ;

    /**
     * 通过文本创建文档
     * @param datasetId 知识库ID
     * @param doc ParamDocument
     * @param retrievalModel RetrievalModel
     * @param embeddingModel Embedding 模型名称
     * @param embeddingModelProvider Embedding 模型供应商
     */
    DocumentResult insertDocByText(
            String datasetId,
            ParamDocument doc,
            RetrievalModel retrievalModel,
            String embeddingModel,
            String embeddingModelProvider
    );

    /**
     * 通过文件创建文档
     * @param datasetId 知识库ID
     * @param file File
     * @param data ParamDocument
     */
    DocumentResult insertDocByFile(String datasetId, File file, ParamDocument data) ;

    /**
     * 通过文件创建文档
     * @param datasetId 知识库ID
     * @param file File
     * @param data ParamDocument
     * @param retrievalModel retrievalModel
     * @param embeddingModel Embedding 模型名称
     * @param embeddingModelProvider Embedding 模型供应商
     */
    DocumentResult insertDocByFile(
            String datasetId,
            File file,
            ParamDocument data,
            RetrievalModel retrievalModel,
            String embeddingModel,
            String embeddingModelProvider
    );

    /**
     * 获取文档嵌入状态（进度）
     * @param datasetId 知识库ID
     * @param batch 上传文档的批次号
     */
    List<BatchStatus> queryBatchStatus(String datasetId, String batch);

    /**
     * 获取知识库文档列表
     * @param datasetId 知识库ID
     */
    DifyPage<Document> documents(String datasetId) ;

    /**
     * 获取知识库文档列表
     * @param datasetId 知识库ID
     * @param page 页码
     * @param limit 返回条数
     * @param keyword 搜索关键词，可选，目前仅搜索文档名称
     */
    DifyPage<Document> documents(String datasetId, Integer page, Integer limit, String keyword) ;

    /**
     * 检索知识库
     * @param datasetId 知识库ID
     * @param query 检索关键词
     * @param retrievalModel RetrievalModel
     */
    RetrieveResult retrieve(String datasetId, String query, RetrievalModel retrievalModel) ;


    /**
     * 通过文本更新文档
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @param data ParamDocument
     */
    DocumentResult updateDocByText(String datasetId, String documentId, ParamDocument data);

    /**
     * 通过文件更新文档
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @param file File
     * @param rule ProcessRule
     * @deprecated 官方文档已将文件更新对齐为 PATCH /datasets/{id}/documents/{document_id}，
     * 请改用 {@link #replaceDocByFile(String, String, File, ParamDocument)}；本方法（POST .../update-by-file）保留至下一次破坏性版本。
     */
    @Deprecated
    DocumentResult updateDocByFile(String datasetId, String documentId, File file, ProcessRule rule);

    /**
     * 通过文件更新文档
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @param name 文档名称
     * @param file 需要上传的文件
     * @param rule 处理规则
     * @deprecated 官方文档已将文件更新对齐为 PATCH /datasets/{id}/documents/{document_id}，
     * 请改用 {@link #replaceDocByFile(String, String, File, ParamDocument)}；本方法（POST .../update-by-file）保留至下一次破坏性版本。
     */
    @Deprecated
    DocumentResult updateDocByFile(String datasetId, String documentId, String name, File file, ProcessRule rule);

    /**
     * 删除文档
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     */
    Boolean deleteDocument(String datasetId, String documentId);

    /**
     * 获取文档文件信息
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     */
    DocFileInfo queryFileInfo(String datasetId, String documentId);

    /**
     * 获取文档分段
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     */
    SegmentResult querySegments(String datasetId, String documentId);

    /**
     * 获取文档分段
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @param keyword 关键词
     */
    SegmentResult querySegments(String datasetId, String documentId, String keyword);

    /**
     * 获取文档分段
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @param keyword 关键词
     * @param status 搜索状态
     */
    SegmentResult querySegments(String datasetId, String documentId, String keyword, String status);

    /**
     * 插入文档分段
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @param segments  List<Segment>
     *      content  文本内容/问题内容
     *      keywords 关键字，非必填
     *      answer   答案内容，非必填，如果知识库的模式为 Q&A 模式则传值
     */
    SegmentResult insertSegment(String datasetId, String documentId, List<Segment> segments);

    /**
     * 删除文档分段
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @param segmentId 分段ID
     */
    Boolean deleteSegment(String datasetId, String documentId, String segmentId);

    /**
     * 更新文档分段
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @param segmentId 分段ID
     * @param content  文本内容/问题内容
     * @param regenerate 是否重新生成向量
     */
    SegmentResult updateSegment(String datasetId, String documentId, String segmentId, String content, boolean regenerate);

    /**
     * 更新文档分段
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @param segment Segment
     * @param regenerate 是否重新生成子分段
     */
    SegmentResult updateSegment(String datasetId, String documentId, Segment segment, boolean regenerate);

    /**
     * 新增文档子分段
     * @param datasetId 知识库 ID
     * @param documentId 文档 ID
     * @param segmentId 分段 ID
     * @param content 子分段内容
     */
    SegmentChildChunk insertSegmentChildChunks(String datasetId, String documentId, String segmentId, String content);

    /**
     * 查询文档子分段
     * @param datasetId 知识库 ID
     * @param documentId 文档 ID
     * @param segmentId 分段 ID
     */
    DifyPage<SegmentChildChunk> querySegmentChildChunks(
            String datasetId,
            String documentId,
            String segmentId,
            Integer page,
            Integer limit
    );

    /**
     * 查询文档子分段
     * @param datasetId 知识库 ID
     * @param documentId 文档 ID
     * @param segmentId 分段 ID
     * @param page 页码（选填，默认1）
     * @param limit 每页数量（选填，默认20，最大100）
     * @param keyword 搜索关键词（选填）
     */
    DifyPage<SegmentChildChunk> querySegmentChildChunks(
            String datasetId,
            String documentId,
            String segmentId,
            Integer page,
            Integer limit,
            String keyword
    );

    /**
     * TODO 删除文档子分段
     * @param datasetId 知识库 ID
     * @param documentId 文档 ID
     * @param segmentId 分段 ID
     * @param childChunkId 子分段 ID
     */
    Boolean deleteSegmentChildChunks(String datasetId, String documentId, String segmentId, String childChunkId);

    /**
     * TODO 更新文档子分段
     * @param datasetId 知识库 ID
     * @param documentId 文档 ID
     * @param segmentId 分段 ID
     * @param childChunkId 子分段 ID
     * @param content 子分段内容
     */
    SegmentChildChunk updateSegmentChildChunks(
            String datasetId,
            String documentId,
            String segmentId,
            String childChunkId,
            String content
    );

    // ==================== 文档（补齐文档域） ====================

    /**
     * 获取文档详情
     * <p>对应 GET /datasets/{dataset_id}/documents/{document_id}；400 invalid_metadata、
     * 403 forbidden、404 not_found 原样抛出。</p>
     *
     * @param datasetId  知识库 ID
     * @param documentId 文档 ID
     * @param metadata   查询参数，控制响应包含、省略还是仅返回元数据字段：all（默认）/ only / without，可为 null
     * @return JSONObject 文档详情（索引状态、元数据、处理统计）
     */
    JSONObject getDocument(String datasetId, String documentId, String metadata);

    /**
     * 下载文档（返回原始上传文件的签名下载 URL）
     * <p>对应 GET /datasets/{dataset_id}/documents/{document_id}/download。</p>
     *
     * @param datasetId  知识库 ID
     * @param documentId 文档 ID
     * @return 签名下载 URL
     */
    String downloadDocument(String datasetId, String documentId);

    /**
     * 批量下载文档（ZIP）
     * <p>对应 POST /datasets/{dataset_id}/documents/download-zip；仅支持文件上传类文档，
     * document_ids 数量 1–100。</p>
     *
     * @param datasetId   知识库 ID
     * @param documentIds 文档 ID 列表（1–100）
     * @return ZIP 压缩包的原始字节数据
     */
    byte[] downloadDocumentsZip(String datasetId, List<String> documentIds);

    /**
     * 批量更新文档状态
     * <p>对应 PATCH /datasets/{dataset_id}/documents/status/{action}；
     * 400 invalid_action（action 无效或文档处于不允许该操作的状态）。</p>
     *
     * @param datasetId   知识库 ID
     * @param action      动作：enable / disable / archive / un_archive
     * @param documentIds 文档 ID 列表
     * @return 是否成功（result == success）
     */
    Boolean updateDocumentsStatus(String datasetId, String action, List<String> documentIds);

    /**
     * 上传新文件替换文档内容并重新触发索引（文档规定的标准更新方式）
     * <p>对应 PATCH /datasets/{dataset_id}/documents/{document_id}，返回 batch ID 用于轮询索引进度。
     * 旧的 POST .../update-by-file 为已废弃别名。</p>
     *
     * @param datasetId  知识库 ID
     * @param documentId 文档 ID
     * @param file       替换用的新文件
     * @param data       处理配置（doc_form/doc_language/process_rule/retrieval_model 等）
     * @return DocumentResult 包含 batch ID
     */
    DocumentResult replaceDocByFile(String datasetId, String documentId, File file, ParamDocument data);

    /**
     * 获取文档中的分段详情
     * <p>对应 GET /datasets/{dataset_id}/documents/{document_id}/segments/{segment_id}；
     * 404 覆盖知识库/文档/分段任一不存在。</p>
     *
     * @param datasetId  知识库 ID
     * @param documentId 文档 ID
     * @param segmentId  分段 ID
     * @return JSONObject 响应 {data: Segment, doc_form}
     */
    JSONObject getSegment(String datasetId, String documentId, String segmentId);

    // ==================== 元数据字段 ====================

    /**
     * 创建元数据字段（POST /datasets/{dataset_id}/metadata）
     *
     * @param datasetId 知识库 ID
     * @param name      字段名称（知识库内唯一，≤255 字符）
     * @param type      字段类型：string / number / time
     * @return JSONObject 新字段（id/name/type）
     */
    JSONObject createMetadataField(String datasetId, String name, String type);

    /**
     * 获取元数据字段列表（GET /datasets/{dataset_id}/metadata）
     *
     * @param datasetId 知识库 ID
     * @return JSONObject built_in_field_enabled + doc_metadata[]（含使用文档数 count）
     */
    JSONObject listMetadataFields(String datasetId);

    /**
     * 重命名元数据字段（PATCH /datasets/{dataset_id}/metadata/{metadata_id}）
     *
     * @param datasetId  知识库 ID
     * @param metadataId 字段 ID
     * @param name       新名称
     * @return JSONObject 更新后的字段
     */
    JSONObject updateMetadataField(String datasetId, String metadataId, String name);

    /**
     * 删除元数据字段（DELETE /datasets/{dataset_id}/metadata/{metadata_id}，204）
     * <p>破坏性操作：使用该字段的文档会丢失对应取值。</p>
     *
     * @param datasetId  知识库 ID
     * @param metadataId 字段 ID
     * @return 是否成功
     */
    Boolean deleteMetadataField(String datasetId, String metadataId);

    /**
     * 获取内置元数据字段（GET /datasets/{dataset_id}/metadata/built-in）
     *
     * @param datasetId 知识库 ID
     * @return JSONObject fields[]：document_name/uploader/upload_date/last_update_date/source
     */
    JSONObject getBuiltInMetadataFields(String datasetId);

    /**
     * 启用/禁用内置元数据字段（POST /datasets/{dataset_id}/metadata/built-in/{action}）
     *
     * @param datasetId 知识库 ID
     * @param action    enable / disable
     * @return JSONObject {result: success}
     */
    JSONObject toggleBuiltInMetadataField(String datasetId, String action);

    /**
     * 批量更新文档元数据（POST /datasets/{dataset_id}/documents/metadata）
     *
     * @param datasetId    知识库 ID
     * @param operationData 操作列表，每项含 document_id 与 metadata_list（元素 id/name 必填，value 可为 string/number/null）
     * @param partialUpdate true 时按部分更新处理（默认 false）
     * @return JSONObject {result}
     */
    JSONObject batchUpdateDocumentMetadata(String datasetId, List<java.util.Map<String, Object>> operationData, Boolean partialUpdate);

    // ==================== 知识库标签 ====================

    /**
     * 创建知识库标签（POST /datasets/tags；路径不含 dataset_id，需全量密钥）
     *
     * @param name 标签名称（工作空间内唯一，1–50 字符）
     * @return JSONObject 标签（id/name/type/binding_count）
     */
    JSONObject createTag(String name);

    /**
     * 获取知识库标签列表（GET /datasets/tags）
     *
     * @return JSONObject 标签数组
     */
    JSONObject listTags();

    /**
     * 修改知识库标签（PATCH /datasets/tags；404 not_found）
     *
     * @param tagId 标签 ID
     * @param name  新名称（1–50 字符）
     * @return JSONObject 更新后的标签
     */
    JSONObject updateTag(String tagId, String name);

    /**
     * 删除知识库标签（DELETE /datasets/tags，204；不会删除已绑定的知识库）
     *
     * @param tagId 标签 ID
     * @return 是否成功
     */
    Boolean deleteTag(String tagId);

    /**
     * 绑定标签到知识库（POST /datasets/tags/binding，204；未知 tag_id 被静默忽略）
     *
     * @param targetId 知识库 ID
     * @param tagIds   标签 ID 列表（≥1）
     * @return 是否成功
     */
    Boolean bindTags(String targetId, List<String> tagIds);

    /**
     * 解除标签与知识库的绑定（POST /datasets/tags/unbinding，204）
     *
     * @param targetId 知识库 ID
     * @param tagIds   要解绑的标签 ID 列表（≥1）
     * @return 是否成功
     */
    Boolean unbindTags(String targetId, List<String> tagIds);

    /**
     * 获取知识库绑定的标签（GET /datasets/{dataset_id}/tags）
     *
     * @param datasetId 知识库 ID
     * @return JSONObject {data:[{id,name}], total}
     */
    JSONObject getDatasetTags(String datasetId);

    // ==================== 模型与知识流水线 ====================

    /**
     * 获取可用模型（GET /workspaces/current/models/model-types/{model_type}）
     *
     * @param modelType 模型类型：text-embedding / rerank / llm / tts / speech2text / moderation
     * @return JSONObject 供应商与模型列表
     */
    JSONObject getAvailableModels(String modelType);

    /**
     * 上传流水线文件（POST /datasets/pipeline/file-upload，201 返回 id；需全量密钥）
     *
     * @param file 待上传文件
     * @return JSONObject {id}，用于 runPipeline 的 local_file.reference
     */
    JSONObject uploadPipelineFile(File file);

    /**
     * 获取数据源插件列表（GET /datasets/{dataset_id}/pipeline/datasource-plugins）
     *
     * @param datasetId  知识库 ID
     * @param isPublished true 读已发布版（默认），false 读草稿版
     * @return JSONObject 数据源节点数组
     */
    JSONObject listDatasourcePlugins(String datasetId, Boolean isPublished);

    /**
     * 执行数据源节点（POST .../pipeline/datasource/nodes/{node_id}/run，SSE 流式响应）
     *
     * @param datasetId      知识库 ID
     * @param nodeId         数据源节点 ID（来自 listDatasourcePlugins）
     * @param inputs         节点输入（必填）
     * @param datasourceType 数据源类型：local_file / online_document / online_drive / website_crawl
     * @param isPublished    是否已发布版本
     * @param credentialId   凭证 ID（可选，null 时使用供应商默认凭证）
     * @param listener       SSE 事件监听器
     * @return EventSource
     */
    EventSource runDatasourceNode(String datasetId, String nodeId, java.util.Map<String, Object> inputs,
                                  String datasourceType, boolean isPublished, String credentialId,
                                  EventSourceListener listener);

    /**
     * 运行知识流水线（阻塞模式，POST /datasets/{dataset_id}/pipeline/run）
     * <p>params 须包含 inputs / datasource_type / datasource_info_list / start_node_id / is_published，
     * response_mode 由本方法强制为 blocking。</p>
     *
     * @param datasetId 知识库 ID
     * @param params    运行参数
     * @return JSONObject 运行结果（workflow_run_id/task_id/data.status）
     */
    JSONObject runPipelineBlocking(String datasetId, java.util.Map<String, Object> params);

    /**
     * 运行知识流水线（流式模式，SSE）
     *
     * @param datasetId 知识库 ID
     * @param params    运行参数（同 {@link #runPipelineBlocking}，response_mode 强制为 streaming）
     * @param listener  SSE 事件监听器
     * @return EventSource
     */
    EventSource runPipelineStreaming(String datasetId, java.util.Map<String, Object> params, EventSourceListener listener);

}
