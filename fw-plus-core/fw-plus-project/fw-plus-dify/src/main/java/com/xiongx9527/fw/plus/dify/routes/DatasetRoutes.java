package com.xiongx9527.fw.plus.dify.routes;

import com.xiongx9527.fw.plus.dify.types.DifyRoute;
import com.xiongx9527.fw.plus.dify.types.HttpMethod;

public class DatasetRoutes {
    public static final DifyRoute MODELS_TEXT_EMBEDDING = new DifyRoute(
            "/workspaces/current/models/model-types/text-embedding",
            HttpMethod.GET
    );

    public static final DifyRoute MODELS_BY_TYPE = new DifyRoute(
            "/workspaces/current/models/model-types/${modelType}",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS = new DifyRoute(
            "/datasets",
            HttpMethod.POST
    );

    /**
     * 知识库列表（与 POST /datasets 常量区分：同路径、GET 方法）
     */
    public static final DifyRoute DATASETS_LIST = new DifyRoute(
            "/datasets",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_INFO = new DifyRoute(
            "/datasets/${datasetId}",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_UPDATE = new DifyRoute(
            "/datasets/${datasetId}",
            HttpMethod.PATCH
    );

    public static final DifyRoute DATASETS_DELETE = new DifyRoute(
            "/datasets/${datasetId}",
            HttpMethod.DELETE
    );

    public static final DifyRoute DATASETS_RETRIEVE = new DifyRoute(
            "/datasets/${datasetId}/retrieve",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_CREATE_DOC_TEXT = new DifyRoute(
            "/datasets/${datasetId}/document/create-by-text",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_CREATE_DOC_FILE = new DifyRoute(
            "/datasets/${datasetId}/document/create-by-file",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_DOCS = new DifyRoute(
            "/datasets/${datasetId}/documents",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_INDEXING_STATUS = new DifyRoute(
            "/datasets/${datasetId}/documents/${batch}/indexing-status",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_DOCS_UPDATE_TEXT = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/update-by-text",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_DOCS_UPDATE_FILE = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/update-by-file",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_DOCS_DELETE = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}",
            HttpMethod.DELETE
    );

    public static final DifyRoute DATASETS_DOCS_FILE_INFO = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/upload-file",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_DOCS_INFO = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_DOCS_DOWNLOAD = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/download",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_DOCS_DOWNLOAD_ZIP = new DifyRoute(
            "/datasets/${datasetId}/documents/download-zip",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_DOCS_STATUS_SET = new DifyRoute(
            "/datasets/${datasetId}/documents/status/${action}",
            HttpMethod.PATCH
    );

    /**
     * 上传新文件替换文档内容并重新触发索引（文档规定的标准更新方式）
     */
    public static final DifyRoute DATASETS_DOCS_FILE_REPLACE = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}",
            HttpMethod.PATCH
    );

    public static final DifyRoute DATASETS_DOCS_SEGMENT_INFO = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}",
            HttpMethod.GET
    );


    public static final DifyRoute DATASETS_DOCS_SEGMENTS = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/segments",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_DOCS_SEGMENTS_ADD = new DifyRoute(
            DATASETS_DOCS_SEGMENTS.getUrl(),
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_DOCS_SEGMENTS_DEL = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}",
            HttpMethod.DELETE
    );

    public static final DifyRoute DATASETS_DOCS_SEGMENTS_UPDATE = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_DOCS_SEGMENTS_CHILD_CHUNKS_GET = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}/child_chunks",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_DOCS_SEGMENTS_CHILD_CHUNKS_ADD = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}/child_chunks",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_DOCS_SEGMENTS_CHILD_CHUNKS_DEL = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}/child_chunks/${childChunkId}",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_DOCS_SEGMENTS_CHILD_CHUNKS_SET = new DifyRoute(
            "/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}/child_chunks/${childChunkId}",
            HttpMethod.PATCH
    );

    // ==================== 元数据字段 ====================

    public static final DifyRoute DATASETS_METADATA = new DifyRoute(
            "/datasets/${datasetId}/metadata",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_METADATA_ADD = new DifyRoute(
            "/datasets/${datasetId}/metadata",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_METADATA_SET = new DifyRoute(
            "/datasets/${datasetId}/metadata/${metadataId}",
            HttpMethod.PATCH
    );

    public static final DifyRoute DATASETS_METADATA_DEL = new DifyRoute(
            "/datasets/${datasetId}/metadata/${metadataId}",
            HttpMethod.DELETE
    );

    public static final DifyRoute DATASETS_METADATA_BUILT_IN = new DifyRoute(
            "/datasets/${datasetId}/metadata/built-in",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_METADATA_BUILT_IN_SET = new DifyRoute(
            "/datasets/${datasetId}/metadata/built-in/${action}",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_DOCS_METADATA_BATCH = new DifyRoute(
            "/datasets/${datasetId}/documents/metadata",
            HttpMethod.POST
    );

    // ==================== 知识库标签 ====================

    public static final DifyRoute DATASETS_TAGS = new DifyRoute(
            "/datasets/tags",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_TAGS_ADD = new DifyRoute(
            "/datasets/tags",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_TAGS_SET = new DifyRoute(
            "/datasets/tags",
            HttpMethod.PATCH
    );

    public static final DifyRoute DATASETS_TAGS_DEL = new DifyRoute(
            "/datasets/tags",
            HttpMethod.DELETE
    );

    public static final DifyRoute DATASETS_TAGS_BINDING = new DifyRoute(
            "/datasets/tags/binding",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_TAGS_UNBINDING = new DifyRoute(
            "/datasets/tags/unbinding",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_TAGS_OF_DATASET = new DifyRoute(
            "/datasets/${datasetId}/tags",
            HttpMethod.GET
    );

    // ==================== 知识流水线 ====================

    public static final DifyRoute DATASETS_PIPELINE_FILE_UPLOAD = new DifyRoute(
            "/datasets/pipeline/file-upload",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_PIPELINE_DATASOURCE_PLUGINS = new DifyRoute(
            "/datasets/${datasetId}/pipeline/datasource-plugins",
            HttpMethod.GET
    );

    public static final DifyRoute DATASETS_PIPELINE_NODE_RUN = new DifyRoute(
            "/datasets/${datasetId}/pipeline/datasource/nodes/${nodeId}/run",
            HttpMethod.POST
    );

    public static final DifyRoute DATASETS_PIPELINE_RUN = new DifyRoute(
            "/datasets/${datasetId}/pipeline/run",
            HttpMethod.POST
    );

}
