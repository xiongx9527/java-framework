package com.xx9527.fw.plus.dify.dataset.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.xx9527.fw.plus.dify.SimpleHttpClient;
import com.xx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xx9527.fw.plus.dify.dataset.entity.*;
import com.xx9527.fw.plus.dify.dataset.heros.DocumentHero;
import com.xx9527.fw.plus.dify.dataset.params.ParamDataset;
import com.xx9527.fw.plus.dify.dataset.params.ParamDocument;
import com.xx9527.fw.plus.dify.dataset.params.ParamUpdateDataset;
import com.xx9527.fw.plus.dify.dataset.types.*;
import com.xx9527.fw.plus.dify.routes.DatasetRoutes;
import com.xx9527.fw.plus.dify.types.ApiConfig;
import com.xx9527.fw.plus.dify.types.DifyPage;
import com.xx9527.fw.plus.dify.types.DifyRoute;
import com.xx9527.fw.plus.dify.types.HttpMethod;
import com.xx9527.fw.plus.dify.dataset.heros.DatasetHero;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatasetClientImpl implements IDatasetClient {
    /** API 配置对象，包含 Dify 服务器地址和认证密钥 */
    private final ApiConfig server;

    /** 知识库缓存映射，用于缓存已加载的知识库对象，避免重复请求 */
    private final Map<String, DatasetHero> datasetCache = new ConcurrentHashMap<>();

    /** 文档缓存映射，用于缓存已加载的文档对象，避免重复请求 */
    private final Map<String, DocumentHero> documentCache = new ConcurrentHashMap<>();

    /**
     * 构建 DatasetClientImpl 实例的静态工厂方法
     *
     * <p>该方法用于创建 DatasetClientImpl 的实例，通过传入的服务器地址和 API 密钥进行初始化。</p>
     *
     * @param server Dify 服务器的访问地址
     *               通常为 HTTP/HTTPS 协议的完整 URL，如 https://api.dify.ai/v1
     * @param apiKey API 认证密钥，用于访问 Dify 服务时的身份验证
     *               该密钥需要在 Dify 平台中获取并具有相应的权限
     * @return DatasetClientImpl 实例，已配置好服务器地址和认证信息
     */
    public static DatasetClientImpl build(String server, String apiKey) {
        return new DatasetClientImpl(server, apiKey);
    }

    /**
     * 私有构造函数，初始化 DatasetClientImpl 实例
     *
     * <p>该构造函数为私有，只能通过 build 静态方法创建实例。</p>
     *
     * @param server Dify 服务器的访问地址
     *               通常为 HTTP/HTTPS 协议的完整 URL
     * @param apiKey API 认证密钥，用于访问 Dify 服务时的身份验证
     */
    private DatasetClientImpl(String server, String apiKey) {
        this.server = ApiConfig.builder().server(server).apiKey(apiKey).build();
    }

    /**
     * 获取或创建知识库英雄对象
     *
     * <p>根据是否启用缓存来决定是从缓存中获取还是创建新的 DatasetHero 对象。</p>
     *
     * @param formCache 是否从缓存中获取
     *                  - true: 优先从缓存中获取，如果缓存不存在则创建并缓存
     *                  - false: 每次都创建新的 DatasetHero 对象
     * @param datasetId 知识库唯一标识符，用于定位特定的知识库
     *                  该 ID 通常由 Dify 服务端生成和返回
     * @return DatasetHero 知识库操作对象，封装了对知识库的各种操作方法
     */
    private DatasetHero ofDataset(boolean formCache, String datasetId) {
        if (!formCache) {
            return DatasetHero.of(datasetId, server);
        }

        return datasetCache.computeIfAbsent(datasetId, k -> DatasetHero.of(datasetId, server));
    }

    /**
     * 获取或创建文档英雄对象
     *
     * <p>根据是否启用缓存来决定是从缓存中获取还是创建新的 DocumentHero 对象。</p>
     *
     * @param formCache 是否从缓存中获取
     *                  - true: 优先从缓存中获取，如果缓存不存在则创建并缓存
     *                  - false: 每次都创建新的 DocumentHero 对象
     * @param datasetId 知识库唯一标识符，用于定位文档所属的知识库
     *                  该 ID 通常由 Dify 服务端生成和返回
     * @param documentId 文档唯一标识符，用于定位特定的文档
     *                   该 ID 通常由 Dify 服务端生成和返回
     * @return DocumentHero 文档操作对象，封装了对文档的各种操作方法
     */
    private DocumentHero ofDocument(boolean formCache, String datasetId, String documentId) {
        if (!formCache) {
            return DocumentHero.of(datasetId, documentId, server);
        }

        return documentCache.computeIfAbsent(
                String.format("%s-%s", datasetId, documentId),
                k -> DocumentHero.of(datasetId, documentId, server)
        );
    }

    /**
     * 获取 HTTP 客户端实例
     *
     * <p>返回已配置好的 SimpleHttpClient 实例，用于向 Dify 服务端发送 HTTP 请求。</p>
     *
     * @return SimpleHttpClient HTTP 客户端实例，已配置服务器地址和认证信息
     */
    @Override
    public SimpleHttpClient httpClient() {
        return SimpleHttpClient.get(server);
    }

    /**
     * 获取知识库操作对象（不缓存）
     *
     * <p>创建一个指定 ID 的知识库操作对象，不使用缓存机制。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位特定的知识库
     *                  该 ID 通常由 Dify 服务端生成和返回
     * @return DatasetHero 知识库操作对象，封装了对知识库的各种操作方法
     */
    @Override
    public DatasetHero ofDataset(String datasetId) {
        return ofDataset(false, datasetId);
    }

    /**
     * 获取文档操作对象（不缓存）
     *
     * <p>创建一个指定知识库和文档 ID 的文档操作对象，不使用缓存机制。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位文档所属的知识库
     *                  该 ID 通常由 Dify 服务端生成和返回
     * @param documentId 文档唯一标识符，用于定位特定的文档
     *                   该 ID 通常由 Dify 服务端生成和返回
     * @return DocumentHero 文档操作对象，封装了对文档的各种操作方法
     */
    @Override
    public DocumentHero ofDocument(String datasetId, String documentId) {
        return ofDocument(false, datasetId, documentId);
    }

    /**
     * 查询可用的文本嵌入模型列表
     *
     * <p>该方法用于获取 Dify 服务端支持的所有文本嵌入模型，用于知识库文档的向量化处理。</p>
     *
     * @return List<JSONObject> 文本嵌入模型列表
     *         每个模型对象包含模型名称、提供商、最大输入长度、价格等详细信息
     */
    @Override
    public List<JSONObject> selectTextEmbedding() {
        String result = SimpleHttpClient.get(server).requestJson(DatasetRoutes.MODELS_TEXT_EMBEDDING);
        return JSON.parseObject(result).getJSONArray("data").toJavaList(JSONObject.class);
    }

    /**
     * 分页查询知识库列表
     *
     * <p>该方法用于获取当前账户下所有知识库的分页列表，支持自定义页码和每页数量。</p>
     *
     * @param page 页码，从 1 开始计数
     *             指定要返回的知识库页码
     * @param limit 每页记录数，限制单次查询返回的最大知识库数量
     *              用于控制响应数据量
     * @return DifyPage<Dataset> 知识库分页结果对象
     *         包含总记录数、当前页码、每页数量以及具体的知识库列表
     */
    @Override
    public DifyPage<Dataset> list(int page, int limit) {
        String result = SimpleHttpClient.get(server).requestJson(DatasetRoutes.DATASETS.getUrl(), HttpMethod.GET, new HashMap<String, Object>() {{
            put("page", page);
            put("limit", limit);
        }}, null);

        return JSON.parseObject(result, new TypeReference<DifyPage<Dataset>>() {});
    }

    /**
     * 创建新的知识库
     *
     * <p>该方法用于在 Dify 平台上创建一个新的知识库，包括设置名称、描述、索引方式等配置。</p>
     *
     * @param data 知识库创建参数对象，包含知识库的名称、描述、索引类型、检索策略等配置信息
     *             这些配置将决定知识库的行为和性能特征
     * @return DatasetHero 新创建的知识库操作对象
     *         可通过该对象继续执行知识库的其他操作
     */
    @Override
    public DatasetHero create(ParamDataset data) {
        String result = SimpleHttpClient.get(server).requestJson(DatasetRoutes.DATASETS, null, data);

        Dataset dataset = JSON.parseObject(result, Dataset.class);
        return DatasetHero.of(dataset, server);
    }

    /**
     * 获取指定知识库的详细信息
     *
     * <p>该方法用于查询 Dify 平台上某个知识库的完整配置和状态信息。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位需要查询的知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @return DatasetHero 知识库操作对象，包含知识库的详细信息和操作方法
     */
    @Override
    public DatasetHero get(String datasetId) {
        DifyRoute r = DatasetRoutes.DATASETS_INFO.format(
                new HashMap<String, Object>() {{
                    put("datasetId", datasetId);
                }}
        );
        String result = SimpleHttpClient.get(server).requestJson(r);

        Dataset dataset = JSON.parseObject(result, Dataset.class);
        return DatasetHero.of(dataset, server);
    }

    /**
     * 更新知识库配置
     *
     * <p>该方法用于修改已有知识库的配置信息，如名称、描述、检索策略等。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位需要更新的知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param data 知识库更新参数对象，包含需要更新的配置项
     *             可以部分更新，未提供的字段将保持不变
     * @return DatasetHero 更新后的知识库操作对象
     */
    @Override
    public DatasetHero update(String datasetId, ParamUpdateDataset data) {
        return ofDataset(true, datasetId).update(data);
    }

    /**
     * 删除知识库
     *
     * <p>该方法用于永久删除指定的知识库及其所有关联的文档和向量数据。
     * 此操作不可逆，请谨慎使用。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位需要删除的知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     */
    @Override
    public void delete(String datasetId) {
        ofDataset(true, datasetId).delete();
    }

    /**
     * 通过文本内容插入文档到知识库
     *
     * <p>该方法用于将纯文本内容作为文档添加到指定知识库中，系统会自动进行分段和向量化处理。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param doc 文档参数对象，包含文档的名称、文本内容、处理规则等配置信息
     * @return DocumentResult 文档插入结果对象
     *         包含文档 ID、处理状态、分段信息等
     */
    @Override
    public DocumentResult insertDocByText(String datasetId, ParamDocument doc) {
        return ofDataset(true, datasetId).insertText(doc);
    }

    /**
     * 通过文本内容插入文档到知识库（自定义检索和嵌入模型）
     *
     * <p>该方法用于将纯文本内容作为文档添加到指定知识库中，支持自定义检索策略和文本嵌入模型。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param doc 文档参数对象，包含文档的名称、文本内容、处理规则等配置信息
     * @param retrievalModel 检索模型配置，定义文档的检索策略和相似度计算方式
     * @param embeddingModel 文本嵌入模型名称，用于将文本转换为向量表示
     *                       如 text-embedding-ada-002 等
     * @param embeddingModelProvider 文本嵌入模型提供商，如 openai、azure 等
     * @return DocumentResult 文档插入结果对象
     *         包含文档 ID、处理状态、分段信息等
     */
    @Override
    public DocumentResult insertDocByText(String datasetId, ParamDocument doc, RetrievalModel retrievalModel, String embeddingModel, String embeddingModelProvider) {
        return ofDataset(true, datasetId).insertText(doc, retrievalModel, embeddingModel, embeddingModelProvider);
    }

    /**
     * 通过文件上传插入文档到知识库
     *
     * <p>该方法用于将本地文件上传到知识库中，系统会自动解析文件内容并进行分段和向量化处理。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param file 要上传的文件对象，支持多种格式如 TXT、PDF、DOCX、MD 等
     *             文件大小和格式需符合服务端的限制要求
     * @param data 文档参数对象，包含文档的处理规则、元数据等配置信息
     * @return DocumentResult 文档插入结果对象
     *         包含文档 ID、处理状态、分段信息等
     */
    @Override
    public DocumentResult insertDocByFile(String datasetId, File file, ParamDocument data) {
        return ofDataset(true, datasetId).insertFile(file, data);
    }

    /**
     * 通过文件上传插入文档到知识库（自定义检索和嵌入模型）
     *
     * <p>该方法用于将本地文件上传到知识库中，支持自定义检索策略和文本嵌入模型。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param file 要上传的文件对象，支持多种格式如 TXT、PDF、DOCX、MD 等
     *             文件大小和格式需符合服务端的限制要求
     * @param data 文档参数对象，包含文档的处理规则、元数据等配置信息
     * @param retrievalModel 检索模型配置，定义文档的检索策略和相似度计算方式
     * @param embeddingModel 文本嵌入模型名称，用于将文本转换为向量表示
     *                       如 text-embedding-ada-002 等
     * @param embeddingModelProvider 文本嵌入模型提供商，如 openai、azure 等
     * @return DocumentResult 文档插入结果对象
     *         包含文档 ID、处理状态、分段信息等
     */
    @Override
    public DocumentResult insertDocByFile(String datasetId, File file, ParamDocument data, RetrievalModel retrievalModel, String embeddingModel, String embeddingModelProvider) {
        return ofDataset(true, datasetId).insertFile(file, data, retrievalModel, embeddingModel, embeddingModelProvider);
    }

    /**
     * 查询批量处理任务的状态
     *
     * <p>该方法用于获取知识库中文档批量处理任务的执行状态，适用于异步处理场景。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param batch 批量处理任务 ID，用于定位特定的批处理任务
     *              该 ID 通常在启动批量处理时由服务端返回
     * @return List<BatchStatus> 批量处理状态列表
     *         包含每个文档的处理状态、进度、错误信息等
     */
    @Override
    public List<BatchStatus> queryBatchStatus(String datasetId, String batch) {
        return ofDataset(true, datasetId).queryBatchStatus(batch);
    }

    /**
     * 查询知识库中的文档列表（默认分页）
     *
     * <p>该方法用于获取指定知识库中所有文档的分页列表，使用默认的分页参数。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @return DifyPage<Document> 文档分页结果对象
     *         包含总记录数、当前页码、每页数量以及具体的文档列表
     */
    @Override
    public DifyPage<Document> documents(String datasetId) {
        return ofDataset(true, datasetId).documents();
    }

    /**
     * 分页查询知识库中的文档列表
     *
     * <p>该方法用于获取指定知识库中文档的分页列表，支持自定义分页和关键词搜索。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param page 页码，从 1 开始计数
     *             指定要返回的文档页码
     * @param limit 每页记录数，限制单次查询返回的最大文档数量
     *              用于控制响应数据量
     * @param keyword 搜索关键词，用于模糊匹配文档名称或内容
     *                可为 null，表示不进行关键词过滤
     * @return DifyPage<Document> 文档分页结果对象
     *         包含总记录数、当前页码、每页数量以及具体的文档列表
     */
    @Override
    public DifyPage<Document> documents(String datasetId, Integer page, Integer limit, String keyword) {
        return ofDataset(true, datasetId).documents(page, limit, keyword);
    }

    /**
     * 检索知识库中的相关内容
     *
     * <p>该方法用于在指定知识库中检索与查询文本最相关的文档片段，支持自定义检索策略。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param query 查询文本，用于在知识库中检索相关内容
     *              可以是问题、关键词或任意文本片段
     * @param retrievalModel 检索模型配置，定义检索策略、相似度阈值、返回数量等参数
     * @return RetrieveResult 检索结果对象
     *         包含匹配的文档片段列表、相关性分数、元数据等信息
     */
    @Override
    public RetrieveResult retrieve(String datasetId, String query, RetrievalModel retrievalModel) {
        return ofDataset(true, datasetId).retrieve(query, retrievalModel);
    }

    /**
     * 通过文本内容更新文档
     *
     * <p>该方法用于更新知识库中已有文档的文本内容，会重新进行分段和向量化处理。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位需要更新的文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param data 文档参数对象，包含新的文本内容和处理规则
     * @return DocumentResult 文档更新结果对象
     *         包含文档 ID、处理状态、分段信息等
     */
    @Override
    public DocumentResult updateDocByText(String datasetId, String documentId, ParamDocument data) {
        return ofDocument(true, datasetId, documentId).updateByText(data);
    }

    /**
     * 通过文件上传更新文档
     *
     * <p>该方法用于通过上传新文件来更新知识库中的文档内容，会重新解析文件并进行分段和向量化处理。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位需要更新的文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param file 要上传的新文件对象，支持多种格式如 TXT、PDF、DOCX、MD 等
     *             文件大小和格式需符合服务端的限制要求
     * @param rule 文档处理规则，定义分段策略、清洗规则等
     * @return DocumentResult 文档更新结果对象
     *         包含文档 ID、处理状态、分段信息等
     */
    @Override
    public DocumentResult updateDocByFile(String datasetId, String documentId, File file, ProcessRule rule) {
        return ofDocument(true, datasetId, documentId).updateByFile(file, rule);
    }

    /**
     * 通过文件上传更新文档（可指定文件名）
     *
     * <p>该方法用于通过上传新文件来更新知识库中的文档内容，支持自定义文件名。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位需要更新的文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param name 新的文档名称，用于替换原有文件名
     *             长度和内容需符合服务端的验证规则
     * @param file 要上传的新文件对象，支持多种格式如 TXT、PDF、DOCX、MD 等
     *             文件大小和格式需符合服务端的限制要求
     * @param rule 文档处理规则，定义分段策略、清洗规则等
     * @return DocumentResult 文档更新结果对象
     *         包含文档 ID、处理状态、分段信息等
     */
    @Override
    public DocumentResult updateDocByFile(String datasetId, String documentId, String name, File file, ProcessRule rule) {
        return ofDocument(true, datasetId, documentId).updateByFile(name, file, rule);
    }

    /**
     * 删除知识库中的文档
     *
     * <p>该方法用于永久删除指定知识库中的文档及其所有关联的向量数据和分段。
     * 此操作不可逆，请谨慎使用。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位需要删除的文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @return Boolean 表示删除操作是否成功
     *         - true: 服务端成功处理删除请求，文档已被永久删除
     *         - false: 删除操作失败，可能原因包括文档不存在、无权限或网络异常
     */
    @Override
    public Boolean deleteDocument(String datasetId, String documentId) {
        return ofDocument(true, datasetId, documentId).delete();
    }

    /**
     * 查询文档的文件信息
     *
     * <p>该方法用于获取知识库中文档的详细文件信息，如文件名、大小、格式、上传时间等元数据。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位需要查询的文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @return DocFileInfo 文档文件信息对象
     *         包含文件名、文件大小、文件格式、上传时间、下载次数等详细元数据
     */
    @Override
    public DocFileInfo queryFileInfo(String datasetId, String documentId) {
        return ofDocument(true, datasetId, documentId).queryFileInfo();
    }

    /**
     * 查询文档的分段列表（全部分段）
     *
     * <p>该方法用于获取知识库中文档的所有文本分段，适用于查看文档的完整分段结构。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位需要查询的文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @return SegmentResult 分段结果对象
     *         包含分段总数和所有分段的详细信息
     */
    @Override
    public SegmentResult querySegments(String datasetId, String documentId) {
        return ofDocument(true, datasetId, documentId).querySegments();
    }

    /**
     * 查询文档的分段列表（关键词搜索）
     *
     * <p>该方法用于获取知识库中文档的文本分段，支持按关键词过滤分段内容。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位需要查询的文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param keyword 搜索关键词，用于模糊匹配分段内容
     *                可为 null，表示不进行关键词过滤
     * @return SegmentResult 分段结果对象
     *         包含匹配的分段总数和符合条件的分段详细信息
     */
    @Override
    public SegmentResult querySegments(String datasetId, String documentId, String keyword) {
        return ofDocument(true, datasetId, documentId).querySegments(keyword);
    }

    /**
     * 查询文档的分段列表（关键词和状态过滤）
     *
     * <p>该方法用于获取知识库中文档的文本分段，支持按关键词和分段状态进行组合过滤。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位需要查询的文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param keyword 搜索关键词，用于模糊匹配分段内容
     *                可为 null，表示不进行关键词过滤
     * @param status 分段状态过滤条件，用于筛选特定状态的分段
     *               可选值包括 completed（已完成）、indexing（索引中）、error（错误）等
     *               可为 null，表示不过滤状态
     * @return SegmentResult 分段结果对象
     *         包含匹配的分段总数和符合条件的分段详细信息
     */
    @Override
    public SegmentResult querySegments(String datasetId, String documentId, String keyword, String status) {
        return ofDocument(true, datasetId, documentId).querySegments(keyword, status);
    }

    /**
     * 插入文档分段
     *
     * <p>该方法用于向知识库中的文档添加新的文本分段，支持批量插入多个分段。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位目标文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param segments 分段列表，包含一个或多个待插入的分段对象
     *                 每个分段包含内容、关键词等属性
     * @return SegmentResult 分段插入结果对象
     *         包含插入的分段总数和每个分段的详细信息
     */
    @Override
    public SegmentResult insertSegment(String datasetId, String documentId, List<Segment> segments) {
        return ofDocument(true, datasetId, documentId).insertSegment(segments);
    }

    /**
     * 删除文档分段
     *
     * <p>该方法用于永久删除指定文档中的某个文本分段及其关联的向量数据。
     * 此操作不可逆，请谨慎使用。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位目标文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param segmentId 分段唯一标识符，用于定位需要删除的分段
     *                  该 ID 通常在创建分段时由服务端返回
     * @return Boolean 表示删除操作是否成功
     *         - true: 服务端成功处理删除请求，分段已被永久删除
     *         - false: 删除操作失败，可能原因包括分段不存在、无权限或网络异常
     */
    @Override
    public Boolean deleteSegment(String datasetId, String documentId, String segmentId) {
        return ofDocument(true, datasetId, documentId).deleteSegment(segmentId);
    }

    /**
     * 更新文档分段内容（通过分段 ID）
     *
     * <p>该方法用于修改知识库中文档的某个分段的内容，支持选择是否重新生成向量嵌入。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位目标文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param segmentId 分段唯一标识符，用于定位需要更新的分段
     *                  该 ID 通常在创建分段时由服务端返回
     * @param content 新的分段文本内容，用于替换原有内容
     *                长度和格式需符合服务端的限制要求
     * @param regenerate 是否重新生成向量嵌入
     *                   - true: 根据新内容重新计算向量表示，会影响检索结果
     *                   - false: 仅更新文本内容，保持原有向量不变
     * @return SegmentResult 分段更新结果对象
     *         包含分段 ID、更新状态、向量生成状态等信息
     */
    @Override
    public SegmentResult updateSegment(String datasetId, String documentId, String segmentId, String content, boolean regenerate) {
        return ofDocument(true, datasetId, documentId).updateSegment(segmentId, content, regenerate);
    }

    /**
     * 更新文档分段内容（通过分段对象）
     *
     * <p>该方法用于修改知识库中文档的某个分段的内容，通过分段对象传递完整的更新信息。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位目标文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param segment 分段对象，包含需要更新的完整信息
     *                包括分段 ID、内容、关键词等属性
     * @param regenerate 是否重新生成向量嵌入
     *                   - true: 根据新内容重新计算向量表示，会影响检索结果
     *                   - false: 仅更新文本内容，保持原有向量不变
     * @return SegmentResult 分段更新结果对象
     *         包含分段 ID、更新状态、向量生成状态等信息
     */
    @Override
    public SegmentResult updateSegment(String datasetId, String documentId, Segment segment, boolean regenerate) {
        return ofDocument(true, datasetId, documentId).updateSegment(segment, regenerate);
    }

    /**
     * 插入子分段
     *
     * <p>该方法用于向知识库中文档的某个分段添加子分段内容，适用于 hierarchical indexing 场景。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位目标文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param segmentId 父分段唯一标识符，用于定位目标分段
     *                  该 ID 通常在创建分段时由服务端返回
     * @param content 子分段的文本内容
     *                长度和格式需符合服务端的限制要求
     * @return SegmentChildChunk 子分段插入结果对象
     *         包含子分段 ID、内容、创建时间等信息
     */
    @Override
    public SegmentChildChunk insertSegmentChildChunks(String datasetId, String documentId, String segmentId, String content) {
        return ofDocument(true, datasetId, documentId).insertSegmentChildChunks(segmentId, content);
    }

    /**
     * 查询子分段列表（默认分页）
     *
     * <p>该方法用于获取知识库中文档某个分段下的所有子分段，使用默认的分页参数。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位目标文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param segmentId 父分段唯一标识符，用于定位目标分段
     *                  该 ID 通常在创建分段时由服务端返回
     * @param page 页码，从 1 开始计数
     *             指定要返回的子分段页码
     * @param limit 每页记录数，限制单次查询返回的最大子分段数量
     *              用于控制响应数据量
     * @return DifyPage<SegmentChildChunk> 子分段分页结果对象
     *         包含总记录数、当前页码、每页数量以及具体的子分段列表
     */
    @Override
    public DifyPage<SegmentChildChunk> querySegmentChildChunks(String datasetId, String documentId, String segmentId, Integer page, Integer limit) {
        return querySegmentChildChunks(datasetId, documentId, segmentId, page, limit, null);
    }

    /**
     * 分页查询子分段列表
     *
     * <p>该方法用于获取知识库中文档某个分段下的子分段列表，支持分页和关键词搜索。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位目标文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param segmentId 父分段唯一标识符，用于定位目标分段
     *                  该 ID 通常在创建分段时由服务端返回
     * @param page 页码，从 1 开始计数
     *             指定要返回的子分段页码
     * @param limit 每页记录数，限制单次查询返回的最大子分段数量
     *              用于控制响应数据量
     * @param keyword 搜索关键词，用于模糊匹配子分段内容
     *                可为 null，表示不进行关键词过滤
     * @return DifyPage<SegmentChildChunk> 子分段分页结果对象
     *         包含总记录数、当前页码、每页数量以及具体的子分段列表
     */
    @Override
    public DifyPage<SegmentChildChunk> querySegmentChildChunks(String datasetId, String documentId, String segmentId, Integer page, Integer limit, String keyword) {
        return ofDocument(true, datasetId, documentId).querySegmentChildChunks(segmentId, page, limit, keyword);
    }

    /**
     * 删除子分段
     *
     * <p>该方法用于永久删除指定分段下的某个子分段。
     * 此操作不可逆，请谨慎使用。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位目标文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param segmentId 父分段唯一标识符，用于定位目标分段
     *                  该 ID 通常在创建分段时由服务端返回
     * @param childChunkId 子分段唯一标识符，用于定位需要删除的子分段
     *                     该 ID 通常在创建子分段时由服务端返回
     * @return Boolean 表示删除操作是否成功
     *         - true: 服务端成功处理删除请求，子分段已被永久删除
     *         - false: 删除操作失败，可能原因包括子分段不存在、无权限或网络异常
     */
    @Override
    public Boolean deleteSegmentChildChunks(String datasetId, String documentId, String segmentId, String childChunkId) {
        return ofDocument(true, datasetId, documentId).deleteSegmentChildChunks(segmentId, childChunkId);
    }

    /**
     * 更新子分段内容
     *
     * <p>该方法用于修改知识库中文档某个分段下的子分段内容。</p>
     *
     * @param datasetId 知识库唯一标识符，用于定位目标知识库
     *                  该 ID 通常在创建知识库时由服务端返回
     * @param documentId 文档唯一标识符，用于定位目标文档
     *                   该 ID 通常在创建文档时由服务端返回
     * @param segmentId 父分段唯一标识符，用于定位目标分段
     *                  该 ID 通常在创建分段时由服务端返回
     * @param childChunkId 子分段唯一标识符，用于定位需要更新的子分段
     *                     该 ID 通常在创建子分段时由服务端返回
     * @param content 新的子分段文本内容，用于替换原有内容
     *                长度和格式需符合服务端的限制要求
     * @return SegmentChildChunk 子分段更新结果对象
     *         包含子分段 ID、更新后的内容、更新时间等信息
     */
    @Override
    public SegmentChildChunk updateSegmentChildChunks(String datasetId, String documentId, String segmentId, String childChunkId, String content) {
        return ofDocument(true, datasetId, documentId).updateSegmentChildChunks(segmentId, childChunkId, content);
    }

}
