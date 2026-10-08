package com.xiongx9527.fw.plus.dify.dataset.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.xiongx9527.fw.plus.dify.SimpleHttpClient;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.dataset.entity.*;
import com.xiongx9527.fw.plus.dify.dataset.heros.DocumentHero;
import com.xiongx9527.fw.plus.dify.dataset.params.ParamDataset;
import com.xiongx9527.fw.plus.dify.dataset.params.ParamDocument;
import com.xiongx9527.fw.plus.dify.dataset.params.ParamUpdateDataset;
import com.xiongx9527.fw.plus.dify.dataset.types.*;
import com.xiongx9527.fw.plus.dify.routes.DatasetRoutes;
import com.xiongx9527.fw.plus.dify.types.ApiConfig;
import com.xiongx9527.fw.plus.dify.types.DifyPage;
import com.xiongx9527.fw.plus.dify.types.DifyRoute;
import com.xiongx9527.fw.plus.dify.types.HttpMethod;
import com.xiongx9527.fw.plus.dify.dataset.heros.DatasetHero;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
     * @deprecated 请改用 {@link #replaceDocByFile(String, String, File, ParamDocument)}（PATCH 对齐路径）
     */
    @Deprecated
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
     * @deprecated 请改用 {@link #replaceDocByFile(String, String, File, ParamDocument)}（PATCH 对齐路径）
     */
    @Deprecated
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

    // ==================== 知识库列表（带筛选） ====================

    /**
     * 查询知识库列表（带筛选）
     *
     * <p>对应 GET /datasets；query：page/limit/keyword/include_all/tag_ids。
     * 使用限定范围的知识库 API 密钥时该接口返回 403 forbidden（路径不含 dataset_id，
     * 需可访问所有知识库的全量密钥）。</p>
     *
     * @param page       页码（默认 1）
     * @param limit      每页条目数（默认 20，服务器上限 100）
     * @param keyword    按名称筛选的搜索关键词（可选）
     * @param includeAll 是否包含所有知识库，无论权限如何（可选，默认 false）
     * @param tagIds     用于筛选的标签 ID 列表（可选）
     * @return DifyPage&lt;Dataset&gt; 分页知识库列表
     */
    @Override
    public DifyPage<Dataset> listDatasets(int page, int limit, String keyword, Boolean includeAll, List<String> tagIds) {
        Map<String, Object> query = new HashMap<>();
        query.put("page", page);
        query.put("limit", limit);
        if (StringUtils.isNotBlank(keyword)) {
            query.put("keyword", keyword);
        }
        if (Objects.nonNull(includeAll)) {
            query.put("include_all", includeAll);
        }
        if (Objects.nonNull(tagIds) && !tagIds.isEmpty()) {
            query.put("tag_ids", StringUtils.join(tagIds, ","));
        }

        String result = httpClient().requestJson(DatasetRoutes.DATASETS_LIST, query, null);
        return JSON.parseObject(result, new TypeReference<DifyPage<Dataset>>() {});
    }

    // ==================== 文档（补齐文档域） ====================

    /**
     * 获取文档详情
     *
     * <p>对应 GET /datasets/{dataset_id}/documents/{document_id}；
     * 400 invalid_metadata、403 forbidden、404 not_found 原样抛出。</p>
     *
     * @param datasetId  知识库 ID
     * @param documentId 文档 ID
     * @param metadata   控制响应包含、省略还是仅返回元数据字段：all（默认）/ only / without，可为 null
     * @return JSONObject 文档详情（索引状态、元数据、处理统计）
     */
    @Override
    public JSONObject getDocument(String datasetId, String documentId, String metadata) {
        DifyRoute route = DatasetRoutes.DATASETS_DOCS_INFO.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
            put("documentId", documentId);
        }});
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(metadata)) {
            query.put("metadata", metadata);
        }
        return JSON.parseObject(httpClient().requestJson(route, query, null));
    }

    /**
     * 下载文档
     *
     * <p>对应 GET /datasets/{dataset_id}/documents/{document_id}/download，
     * 返回原始上传文件的签名下载 URL；403 四态与 404 原样抛出。</p>
     *
     * @param datasetId  知识库 ID
     * @param documentId 文档 ID
     * @return 签名下载 URL
     */
    @Override
    public String downloadDocument(String datasetId, String documentId) {
        DifyRoute route = DatasetRoutes.DATASETS_DOCS_DOWNLOAD.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
            put("documentId", documentId);
        }});
        String result = httpClient().requestJson(route, null, null);
        if (StringUtils.isBlank(result)) {
            return result;
        }
        JSONObject json = JSON.parseObject(result);
        String url = json != null ? json.getString("url") : null;
        return StringUtils.isNotBlank(url) ? url : result;
    }

    /**
     * 批量下载文档（ZIP）
     *
     * <p>对应 POST /datasets/{dataset_id}/documents/download-zip；
     * 仅支持文件上传类文档，document_ids 数量 1–100。</p>
     *
     * @param datasetId   知识库 ID
     * @param documentIds 文档 ID 列表（1–100）
     * @return ZIP 压缩包的原始字节数据
     */
    @Override
    public byte[] downloadDocumentsZip(String datasetId, List<String> documentIds) {
        DifyRoute route = DatasetRoutes.DATASETS_DOCS_DOWNLOAD_ZIP.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
        }});
        Map<String, Object> body = new HashMap<>();
        body.put("document_ids", documentIds);
        return httpClient().requestBytes(route, null, body);
    }

    /**
     * 批量更新文档状态
     *
     * <p>对应 PATCH /datasets/{dataset_id}/documents/status/{action}；
     * 400 invalid_action（action 无效或文档处于不允许该操作的状态）。</p>
     *
     * @param datasetId   知识库 ID
     * @param action      动作：enable / disable / archive / un_archive
     * @param documentIds 文档 ID 列表
     * @return 是否成功（result == success）
     */
    @Override
    public Boolean updateDocumentsStatus(String datasetId, String action, List<String> documentIds) {
        DifyRoute route = DatasetRoutes.DATASETS_DOCS_STATUS_SET.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
            put("action", action);
        }});
        Map<String, Object> body = new HashMap<>();
        body.put("document_ids", documentIds);
        return parseResultSuccess(httpClient().requestJson(route, null, body));
    }

    /**
     * 上传新文件替换文档内容并重新触发索引（文档规定的标准更新方式）
     *
     * <p>对应 PATCH /datasets/{dataset_id}/documents/{document_id}（multipart：file + data），
     * 返回 batch ID 用于轮询索引进度。旧的 POST .../update-by-file 为已废弃别名。</p>
     *
     * @param datasetId  知识库 ID
     * @param documentId 文档 ID
     * @param file       替换用的新文件
     * @param data       处理配置（doc_form/doc_language/process_rule/retrieval_model 等）
     * @return DocumentResult 包含 batch ID
     */
    @Override
    public DocumentResult replaceDocByFile(String datasetId, String documentId, File file, ParamDocument data) {
        DifyRoute route = DatasetRoutes.DATASETS_DOCS_FILE_REPLACE.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
            put("documentId", documentId);
        }});
        Map<String, Object> fields = new HashMap<>();
        fields.put("file", file);
        if (Objects.nonNull(data)) {
            Map<String, Object> dataMap = data.toMap();
            // 部分更新接口：data 中不接受 name/text
            dataMap.remove("name");
            dataMap.remove("text");
            fields.put("data", JSON.toJSONString(dataMap));
        }
        String result = httpClient().requestMultipart(route, null, fields);
        return DocumentResult.parse(datasetId, result, server);
    }

    /**
     * 获取文档中的分段详情
     *
     * <p>对应 GET /datasets/{dataset_id}/documents/{document_id}/segments/{segment_id}；
     * 404 覆盖知识库/文档/分段任一不存在。</p>
     *
     * @param datasetId  知识库 ID
     * @param documentId 文档 ID
     * @param segmentId  分段 ID
     * @return JSONObject 响应 {data: Segment, doc_form}
     */
    @Override
    public JSONObject getSegment(String datasetId, String documentId, String segmentId) {
        DifyRoute route = DatasetRoutes.DATASETS_DOCS_SEGMENT_INFO.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
            put("documentId", documentId);
            put("segmentId", segmentId);
        }});
        return JSON.parseObject(httpClient().requestJson(route, null, null));
    }

    // ==================== 元数据字段 ====================

    /**
     * 创建元数据字段
     *
     * <p>对应 POST /datasets/{dataset_id}/metadata；201；400 invalid_param。
     * 使用限定范围的知识库密钥时返回 403（需全量密钥）。</p>
     *
     * @param datasetId 知识库 ID
     * @param name      字段名称（知识库内唯一，≤255 字符）
     * @param type      字段类型：string / number / time
     * @return JSONObject 新字段（id/name/type）
     */
    @Override
    public JSONObject createMetadataField(String datasetId, String name, String type) {
        DifyRoute route = DatasetRoutes.DATASETS_METADATA_ADD.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
        }});
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", type);
        return JSON.parseObject(httpClient().requestJson(route, null, body));
    }

    /**
     * 获取元数据字段列表
     *
     * <p>对应 GET /datasets/{dataset_id}/metadata；
     * 返回 built_in_field_enabled + doc_metadata[]（含使用文档数 count）。</p>
     *
     * @param datasetId 知识库 ID
     * @return JSONObject 元数据字段结构
     */
    @Override
    public JSONObject listMetadataFields(String datasetId) {
        DifyRoute route = DatasetRoutes.DATASETS_METADATA.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
        }});
        return JSON.parseObject(httpClient().requestJson(route, null, null));
    }

    /**
     * 重命名元数据字段
     *
     * <p>对应 PATCH /datasets/{dataset_id}/metadata/{metadata_id}，body name。</p>
     *
     * @param datasetId  知识库 ID
     * @param metadataId 字段 ID
     * @param name       新名称
     * @return JSONObject 更新后的字段
     */
    @Override
    public JSONObject updateMetadataField(String datasetId, String metadataId, String name) {
        DifyRoute route = DatasetRoutes.DATASETS_METADATA_SET.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
            put("metadataId", metadataId);
        }});
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        return JSON.parseObject(httpClient().requestJson(route, null, body));
    }

    /**
     * 删除元数据字段
     *
     * <p>对应 DELETE /datasets/{dataset_id}/metadata/{metadata_id}（204）。
     * 破坏性操作：使用该字段的文档会丢失对应取值。</p>
     *
     * @param datasetId  知识库 ID
     * @param metadataId 字段 ID
     * @return 是否成功
     */
    @Override
    public Boolean deleteMetadataField(String datasetId, String metadataId) {
        DifyRoute route = DatasetRoutes.DATASETS_METADATA_DEL.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
            put("metadataId", metadataId);
        }});
        return parseResultSuccess(httpClient().requestJson(route, null, null));
    }

    /**
     * 获取内置元数据字段
     *
     * <p>对应 GET /datasets/{dataset_id}/metadata/built-in；
     * fields：document_name/uploader/upload_date/last_update_date/source。</p>
     *
     * @param datasetId 知识库 ID
     * @return JSONObject fields 数组
     */
    @Override
    public JSONObject getBuiltInMetadataFields(String datasetId) {
        DifyRoute route = DatasetRoutes.DATASETS_METADATA_BUILT_IN.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
        }});
        return JSON.parseObject(httpClient().requestJson(route, null, null));
    }

    /**
     * 启用/禁用内置元数据字段
     *
     * <p>对应 POST /datasets/{dataset_id}/metadata/built-in/{action}；
     * action：enable / disable；响应 {result: success}。</p>
     *
     * @param datasetId 知识库 ID
     * @param action    enable / disable
     * @return JSONObject {result: success}
     */
    @Override
    public JSONObject toggleBuiltInMetadataField(String datasetId, String action) {
        DifyRoute route = DatasetRoutes.DATASETS_METADATA_BUILT_IN_SET.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
            put("action", action);
        }});
        return JSON.parseObject(httpClient().requestJson(route, null, null));
    }

    /**
     * 批量更新文档元数据
     *
     * <p>对应 POST /datasets/{dataset_id}/documents/metadata；
     * 400 进行中冲突、404。</p>
     *
     * @param datasetId     知识库 ID
     * @param operationData 操作列表，每项含 document_id 与 metadata_list（元素 id/name 必填，value 可为 string/number/null）
     * @param partialUpdate true 时按部分更新处理（默认 false）
     * @return JSONObject {result}
     */
    @Override
    public JSONObject batchUpdateDocumentMetadata(String datasetId, List<Map<String, Object>> operationData, Boolean partialUpdate) {
        DifyRoute route = DatasetRoutes.DATASETS_DOCS_METADATA_BATCH.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
        }});
        Map<String, Object> body = new HashMap<>();
        body.put("operation_data", operationData);
        if (Objects.nonNull(partialUpdate)) {
            body.put("partial_update", partialUpdate);
        }
        return JSON.parseObject(httpClient().requestJson(route, null, body));
    }

    // ==================== 知识库标签 ====================

    /**
     * 创建知识库标签
     *
     * <p>对应 POST /datasets/tags；路径不含 dataset_id，需全量密钥（限定范围密钥返回 403）。
     * name 唯一 1–50 字符；400 重名。</p>
     *
     * @param name 标签名称（工作空间内唯一，1–50 字符）
     * @return JSONObject 标签（id/name/type/binding_count）
     */
    @Override
    public JSONObject createTag(String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        return JSON.parseObject(httpClient().requestJson(DatasetRoutes.DATASETS_TAGS_ADD, null, body));
    }

    /**
     * 获取知识库标签列表
     *
     * <p>对应 GET /datasets/tags；路径不含 dataset_id，需全量密钥。</p>
     *
     * @return JSONObject 标签数组
     */
    @Override
    public JSONObject listTags() {
        return JSON.parseObject(httpClient().requestJson(DatasetRoutes.DATASETS_TAGS, null, null));
    }

    /**
     * 修改知识库标签
     *
     * <p>对应 PATCH /datasets/tags，body tag_id + name；404 not_found。
     * 路径不含 dataset_id，需全量密钥。</p>
     *
     * @param tagId 标签 ID
     * @param name  新名称（1–50 字符）
     * @return JSONObject 更新后的标签
     */
    @Override
    public JSONObject updateTag(String tagId, String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("tag_id", tagId);
        body.put("name", name);
        return JSON.parseObject(httpClient().requestJson(DatasetRoutes.DATASETS_TAGS_SET, null, body));
    }

    /**
     * 删除知识库标签
     *
     * <p>对应 DELETE /datasets/tags，body tag_id（204）；不会删除已绑定的知识库。
     * 路径不含 dataset_id，需全量密钥。</p>
     *
     * @param tagId 标签 ID
     * @return 是否成功
     */
    @Override
    public Boolean deleteTag(String tagId) {
        Map<String, Object> body = new HashMap<>();
        body.put("tag_id", tagId);
        return parseResultSuccess(httpClient().requestJson(DatasetRoutes.DATASETS_TAGS_DEL, null, body));
    }

    /**
     * 绑定标签到知识库
     *
     * <p>对应 POST /datasets/tags/binding，body target_id + tag_ids[]（204）；
     * 未知 tag_id 被静默忽略；404。路径不含 dataset_id，需全量密钥。</p>
     *
     * @param targetId 知识库 ID
     * @param tagIds   标签 ID 列表（≥1）
     * @return 是否成功
     */
    @Override
    public Boolean bindTags(String targetId, List<String> tagIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("target_id", targetId);
        body.put("tag_ids", tagIds);
        return parseResultSuccess(httpClient().requestJson(DatasetRoutes.DATASETS_TAGS_BINDING, null, body));
    }

    /**
     * 解除标签与知识库的绑定
     *
     * <p>对应 POST /datasets/tags/unbinding，body target_id + tag_ids[]（204）。
     * 官方接口的 tag_id 单数字段为废弃别名，本 SDK 仅暴露 tag_ids 数组。
     * 路径不含 dataset_id，需全量密钥。</p>
     *
     * @param targetId 知识库 ID
     * @param tagIds   要解绑的标签 ID 列表（≥1）
     * @return 是否成功
     */
    @Override
    public Boolean unbindTags(String targetId, List<String> tagIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("target_id", targetId);
        body.put("tag_ids", tagIds);
        return parseResultSuccess(httpClient().requestJson(DatasetRoutes.DATASETS_TAGS_UNBINDING, null, body));
    }

    /**
     * 获取知识库绑定的标签
     *
     * <p>对应 GET /datasets/{dataset_id}/tags；返回 {data:[{id,name}], total}；
     * 403/404 原样抛出。</p>
     *
     * @param datasetId 知识库 ID
     * @return JSONObject {data, total}
     */
    @Override
    public JSONObject getDatasetTags(String datasetId) {
        DifyRoute route = DatasetRoutes.DATASETS_TAGS_OF_DATASET.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
        }});
        return JSON.parseObject(httpClient().requestJson(route, null, null));
    }

    // ==================== 模型与知识流水线 ====================

    /**
     * 获取可用模型
     *
     * <p>对应 GET /workspaces/current/models/model-types/{model_type}。</p>
     *
     * @param modelType 模型类型：text-embedding / rerank / llm / tts / speech2text / moderation
     * @return JSONObject 供应商与模型列表
     */
    @Override
    public JSONObject getAvailableModels(String modelType) {
        DifyRoute route = DatasetRoutes.MODELS_BY_TYPE.format(new HashMap<String, Object>() {{
            put("modelType", modelType);
        }});
        return JSON.parseObject(httpClient().requestJson(route, null, null));
    }

    /**
     * 上传流水线文件
     *
     * <p>对应 POST /datasets/pipeline/file-upload（multipart file，201 返回 id）；
     * 路径不含 dataset_id，需全量密钥；403、413、415 原样抛出。</p>
     *
     * @param file 待上传文件
     * @return JSONObject {id}，用于 runPipeline 的 local_file.reference
     */
    @Override
    public JSONObject uploadPipelineFile(File file) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("file", file);
        return JSON.parseObject(httpClient().requestMultipart(DatasetRoutes.DATASETS_PIPELINE_FILE_UPLOAD, null, fields));
    }

    /**
     * 获取数据源插件列表
     *
     * <p>对应 GET /datasets/{dataset_id}/pipeline/datasource-plugins；
     * query is_published（默认 true）；400 Pipeline not found。</p>
     *
     * @param datasetId   知识库 ID
     * @param isPublished true 读已发布版（默认），false 读草稿版
     * @return JSONObject 数据源节点数组
     */
    @Override
    public JSONObject listDatasourcePlugins(String datasetId, Boolean isPublished) {
        DifyRoute route = DatasetRoutes.DATASETS_PIPELINE_DATASOURCE_PLUGINS.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
        }});
        Map<String, Object> query = new HashMap<>();
        if (Objects.nonNull(isPublished)) {
            query.put("is_published", isPublished);
        }
        return JSON.parseObject(httpClient().requestJson(route, query, null));
    }

    /**
     * 执行数据源节点（SSE 流式响应）
     *
     * <p>对应 POST /datasets/{dataset_id}/pipeline/datasource/nodes/{node_id}/run；
     * body：inputs / datasource_type / is_published / credential_id。</p>
     *
     * @param datasetId      知识库 ID
     * @param nodeId         节点 ID（来自 listDatasourcePlugins）
     * @param inputs         节点输入（必填）
     * @param datasourceType 数据源类型：local_file / online_document / online_drive / website_crawl
     * @param isPublished    是否已发布版本
     * @param credentialId   凭证 ID（可选，null 时使用供应商默认凭证）
     * @param listener       SSE 事件监听器
     * @return EventSource
     */
    @Override
    public EventSource runDatasourceNode(String datasetId, String nodeId, Map<String, Object> inputs,
                                         String datasourceType, boolean isPublished, String credentialId,
                                         EventSourceListener listener) {
        DifyRoute route = DatasetRoutes.DATASETS_PIPELINE_NODE_RUN.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
            put("nodeId", nodeId);
        }});
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("datasource_type", datasourceType);
        body.put("is_published", isPublished);
        if (Objects.nonNull(credentialId)) {
            body.put("credential_id", credentialId);
        }
        return httpClient().requestSse(route, null, body, listener);
    }

    /**
     * 运行知识流水线（阻塞模式）
     *
     * <p>对应 POST /datasets/{dataset_id}/pipeline/run，response_mode 强制为 blocking；
     * params 须包含 inputs / datasource_type / datasource_info_list / start_node_id / is_published。</p>
     *
     * @param datasetId 知识库 ID
     * @param params    运行参数
     * @return JSONObject 运行结果（workflow_run_id/task_id/data.status）
     */
    @Override
    public JSONObject runPipelineBlocking(String datasetId, Map<String, Object> params) {
        DifyRoute route = DatasetRoutes.DATASETS_PIPELINE_RUN.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
        }});
        Map<String, Object> body = new HashMap<>();
        if (Objects.nonNull(params)) {
            body.putAll(params);
        }
        body.put("response_mode", "blocking");
        return JSON.parseObject(httpClient().requestJson(route, null, body));
    }

    /**
     * 运行知识流水线（流式模式，SSE）
     *
     * <p>对应 POST /datasets/{dataset_id}/pipeline/run，response_mode 强制为 streaming。</p>
     *
     * @param datasetId 知识库 ID
     * @param params    运行参数（同 {@link #runPipelineBlocking}）
     * @param listener  SSE 事件监听器
     * @return EventSource
     */
    @Override
    public EventSource runPipelineStreaming(String datasetId, Map<String, Object> params, EventSourceListener listener) {
        DifyRoute route = DatasetRoutes.DATASETS_PIPELINE_RUN.format(new HashMap<String, Object>() {{
            put("datasetId", datasetId);
        }});
        Map<String, Object> body = new HashMap<>();
        if (Objects.nonNull(params)) {
            body.putAll(params);
        }
        body.put("response_mode", "streaming");
        return httpClient().requestSse(route, null, body, listener);
    }

    /**
     * 解析 {result: success} 或 204 空响应为布尔结果
     *
     * @param body 响应体，可为 null/空（204 场景）
     * @return 是否成功
     */
    private Boolean parseResultSuccess(String body) {
        if (StringUtils.isBlank(body)) {
            return Boolean.TRUE;
        }
        JSONObject json = JSON.parseObject(body);
        if (json == null || !json.containsKey("result")) {
            return Boolean.TRUE;
        }
        return "success".equalsIgnoreCase(json.getString("result"));
    }

}
