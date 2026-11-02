package com.xiongx9527.fw.plus.ai.store.embedding;

import com.xiongx9527.fw.plus.ai.data.document.Document;
import com.xiongx9527.fw.plus.ai.data.document.DocumentSplitter;
import com.xiongx9527.fw.plus.ai.data.document.DocumentTransformer;
import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;
import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.data.segment.TextSegmentTransformer;
import com.xiongx9527.fw.plus.ai.model.embedding.EmbeddingModel;
import com.xiongx9527.fw.plus.ai.model.embedding.request.EmbeddingInputType;
import com.xiongx9527.fw.plus.ai.model.embedding.request.EmbeddingRequest;
import com.xiongx9527.fw.plus.ai.model.embedding.response.EmbeddingResponse;
import com.xiongx9527.fw.plus.ai.model.output.Response;
import com.xiongx9527.fw.plus.ai.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文档入库器：把文档转换成向量并写入向量库。
 *
 * <p><b>作用</b>：固化「转换 → 切分 → 片段加工 → 向量化 → 写入」这条标准入库流水线，
 * 各环节通过接口注入，便于替换解析器、切分策略与向量库。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.EmbeddingStoreIngestor}。</p>
 *
 * <p><b>实现约定</b>：流水线各环节允许为 {@code null}（表示跳过该环节）；
 * 未注入切分器时整篇文档作为一个片段入库。令牌用量按各批次累加后返回。</p>
 *
 * @author xiongxiao
 */
public class EmbeddingStoreIngestor {

    private final DocumentTransformer documentTransformer;
    private final DocumentSplitter documentSplitter;
    private final TextSegmentTransformer textSegmentTransformer;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingInputType embeddingInputType;

    /**
     * 构造入库器。
     *
     * @param documentTransformer   文档转换器，可为 {@code null}
     * @param documentSplitter      文档切分器，可为 {@code null}
     * @param textSegmentTransformer 片段转换器，可为 {@code null}
     * @param embeddingModel        嵌入模型
     * @param embeddingStore        向量库
     */
    public EmbeddingStoreIngestor(DocumentTransformer documentTransformer, DocumentSplitter documentSplitter,
                                  TextSegmentTransformer textSegmentTransformer, EmbeddingModel embeddingModel,
                                  EmbeddingStore<TextSegment> embeddingStore) {
        this(documentTransformer, documentSplitter, textSegmentTransformer, embeddingModel, embeddingStore, null);
    }

    /**
     * 构造入库器（可指定输入用途）。
     *
     * @param documentTransformer   文档转换器，可为 {@code null}
     * @param documentSplitter      文档切分器，可为 {@code null}
     * @param textSegmentTransformer 片段转换器，可为 {@code null}
     * @param embeddingModel        嵌入模型
     * @param embeddingStore        向量库
     * @param embeddingInputType    输入用途，可为 {@code null}
     */
    public EmbeddingStoreIngestor(DocumentTransformer documentTransformer, DocumentSplitter documentSplitter,
                                  TextSegmentTransformer textSegmentTransformer, EmbeddingModel embeddingModel,
                                  EmbeddingStore<TextSegment> embeddingStore,
                                  EmbeddingInputType embeddingInputType) {
        this.documentTransformer = documentTransformer;
        this.documentSplitter = documentSplitter;
        this.textSegmentTransformer = textSegmentTransformer;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.embeddingInputType = embeddingInputType;
    }

    /**
     * 用默认流水线把单个文档入库。
     *
     * @param document       文档
     * @param embeddingStore 向量库
     * @return 入库结果
     */
    public static IngestionResult ingest(Document document, EmbeddingStore<TextSegment> embeddingStore) {
        return builder().embeddingStore(embeddingStore).build().ingest(document);
    }

    /**
     * 用默认流水线把多个文档入库。
     *
     * @param documents      文档列表
     * @param embeddingStore 向量库
     * @return 入库结果
     */
    public static IngestionResult ingest(List<Document> documents, EmbeddingStore<TextSegment> embeddingStore) {
        return builder().embeddingStore(embeddingStore).build().ingest(documents);
    }

    /**
     * 入库单个文档。
     *
     * @param document 文档
     * @return 入库结果
     */
    public IngestionResult ingest(Document document) {
        return ingest(java.util.Collections.singletonList(document));
    }

    /**
     * 入库多个文档。
     *
     * @param documents 文档数组
     * @return 入库结果
     */
    public IngestionResult ingest(Document... documents) {
        return ingest(Arrays.asList(documents));
    }

    /**
     * 入库多个文档。
     *
     * @param documents 文档列表
     * @return 入库结果
     */
    public IngestionResult ingest(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return new IngestionResult(null);
        }
        List<Document> transformed = documentTransformer == null
                ? documents
                : documentTransformer.transformAll(documents);

        List<TextSegment> segments = new ArrayList<TextSegment>();
        for (Document document : transformed) {
            if (documentSplitter == null) {
                segments.add(document.toTextSegment());
            } else {
                segments.addAll(documentSplitter.split(document));
            }
        }
        if (textSegmentTransformer != null) {
            segments = textSegmentTransformer.transformAll(segments);
        }
        if (segments.isEmpty()) {
            return new IngestionResult(null);
        }

        TokenUsage totalUsage = null;
        if (embeddingInputType == null) {
            Response<List<Embedding>> response = embeddingModel.embedAll(segments);
            embeddingStore.addAll(response.content(), segments);
            totalUsage = response.tokenUsage();
        } else {
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .textSegments(segments)
                    .inputType(embeddingInputType)
                    .build();
            EmbeddingResponse response = embeddingModel.embed(request);
            embeddingStore.addAll(response.embeddings(), segments);
            totalUsage = response.tokenUsage();
        }
        return new IngestionResult(totalUsage);
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link EmbeddingStoreIngestor} 构建器。 */
    public static class Builder {

        private DocumentTransformer documentTransformer;
        private DocumentSplitter documentSplitter;
        private TextSegmentTransformer textSegmentTransformer;
        private EmbeddingModel embeddingModel;
        private EmbeddingStore<TextSegment> embeddingStore;
        private EmbeddingInputType embeddingInputType;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置输入用途。
         *
         * @param embeddingInputType 输入用途
         * @return 本构建器
         */
        public Builder embeddingInputType(EmbeddingInputType embeddingInputType) {
            this.embeddingInputType = embeddingInputType;
            return this;
        }

        /**
         * 设置文档转换器。
         *
         * @param documentTransformer 文档转换器
         * @return 本构建器
         */
        public Builder documentTransformer(DocumentTransformer documentTransformer) {
            this.documentTransformer = documentTransformer;
            return this;
        }

        /**
         * 设置文档切分器。
         *
         * @param documentSplitter 文档切分器
         * @return 本构建器
         */
        public Builder documentSplitter(DocumentSplitter documentSplitter) {
            this.documentSplitter = documentSplitter;
            return this;
        }

        /**
         * 设置片段转换器。
         *
         * @param textSegmentTransformer 片段转换器
         * @return 本构建器
         */
        public Builder textSegmentTransformer(TextSegmentTransformer textSegmentTransformer) {
            this.textSegmentTransformer = textSegmentTransformer;
            return this;
        }

        /**
         * 设置嵌入模型。
         *
         * @param embeddingModel 嵌入模型
         * @return 本构建器
         */
        public Builder embeddingModel(EmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
            return this;
        }

        /**
         * 设置向量库。
         *
         * @param embeddingStore 向量库
         * @return 本构建器
         */
        public Builder embeddingStore(EmbeddingStore<TextSegment> embeddingStore) {
            this.embeddingStore = embeddingStore;
            return this;
        }

        /**
         * 构建入库器。
         *
         * @return 入库器
         */
        public EmbeddingStoreIngestor build() {
            return new EmbeddingStoreIngestor(documentTransformer, documentSplitter, textSegmentTransformer,
                    embeddingModel, embeddingStore, embeddingInputType);
        }
    }
}
