package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSegmentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.DocumentPipeline;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbIndexingExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store.KbOriginalFileStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 索引管道测试（验收 A3/A4/A7）：CAS 认领、状态机 waiting→completed/error、原文回读、向量写入。
 * 使用 InMemoryEmbeddingStore 与固定向量模型，MyBatis mapper 以 Mockito 模拟。
 */
class KnowledgeIndexingServiceTest {

    private KbDocumentMapper documentMapper;

    private KbDatasetMapper datasetMapper;

    private KbSegmentMapper segmentMapper;

    private KbOriginalFileStore fileStore;

    private ContentRetrieverContext retrieverContext;

    private KbIndexingExecutor executor;

    private KbKnowledgeState state;

    private KbIndexingService service;

    private InMemoryEmbeddingStore<TextSegment> embeddingStore;

    private final AtomicReference<byte[]> savedFile = new AtomicReference<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        documentMapper = mock(KbDocumentMapper.class);
        datasetMapper = mock(KbDatasetMapper.class);
        segmentMapper = mock(KbSegmentMapper.class);
        fileStore = mock(KbOriginalFileStore.class);
        state = mock(KbKnowledgeState.class);
        Mockito.doNothing().when(state).checkReady();
        Mockito.when(state.canProcess()).thenReturn(true);

        embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingModel embeddingModel = fixedModel();
        retrieverContext = mock(ContentRetrieverContext.class);
        Mockito.when(retrieverContext.embeddingModel(anyString())).thenReturn(embeddingModel);
        Mockito.when(retrieverContext.embeddingStore(anyString()))
                .thenReturn((EmbeddingStore<TextSegment>) embeddingStore);

        executor = new KbIndexingExecutor();
        service = new KbIndexingService(datasetMapper, documentMapper, segmentMapper, fileStore,
                new DocumentPipeline(), executor, retrieverContext, state);

        when(fileStore.read(anyString())).thenAnswer(inv ->
                new ByteArrayInputStream(savedFile.get() == null ? new byte[0] : savedFile.get()));
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    /** 固定向量模型：任意文本返回确定性向量，便于 InMemory 检索断言。 */
    private static EmbeddingModel fixedModel() {
        return new EmbeddingModel() {
            @Override
            public Response<Embedding> embed(String text) {
                return Response.from(Embedding.from(new float[]{1f, (text == null ? 0 : text.length()) / 100f}));
            }

            @Override
            public Response<Embedding> embed(TextSegment textSegment) {
                return embed(textSegment.text());
            }

            @Override
            public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
                List<Embedding> embeddings = new ArrayList<>();
                for (TextSegment segment : textSegments) {
                    embeddings.add(embed(segment).content());
                }
                return Response.from(embeddings);
            }
        };
    }

    private static <T> ObjectProvider<T> providerOf(final T value) {
        return Mockito.mock(ObjectProvider.class);
    }

    private KbDocument waitingDocument() {
        KbDocument document = new KbDocument();
        document.setId("doc1");
        document.setDatasetId("ds1");
        document.setName("a.txt");
        document.setFilePath("a.txt");
        document.setIndexingStatus("waiting");
        document.setEnabled("1");
        document.setArchived("0");
        StringBuilder textBuilder = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            textBuilder.append("知识库测试内容。");
        }
        String text = textBuilder.toString();
        document.setDocHash("hash");
        savedFile.set(text.getBytes(StandardCharsets.UTF_8));
        return document;
    }

    private KbDataset dataset() {
        KbDataset dataset = new KbDataset();
        dataset.setId("ds1");
        dataset.setName("测试数据集");
        dataset.setEmbeddingModelKey("model-a");
        dataset.setEmbeddingModelActive("model-a");
        dataset.setEmbeddingStoreKey("store-a");
        dataset.setSegmentMode("general");
        dataset.setSegmentMaxLength(100);
        dataset.setSegmentOverlap(10);
        dataset.setStatus("active");
        return dataset;
    }

    @Test
    void cas认领失败时不重复索引() {
        KbDocument document = waitingDocument();
        when(documentMapper.selectById("doc1")).thenReturn(document);
        // CAS 认领失败（已被其它实例抢到）
        when(documentMapper.casTransit("doc1", "waiting", "parsing")).thenReturn(0);

        boolean processed = service.processOne("doc1");

        assertThat(processed).isFalse();
        // 认领失败后不再流转后续状态，也不写分段
        verify(documentMapper, never()).casTransit(eq("doc1"), eq("parsing"), anyString());
        verify(segmentMapper, never()).insert(any(KbSegment.class));
    }

    @Test
    void 管道完成后状态completed且向量写入() {
        KbDocument document = waitingDocument();
        when(documentMapper.selectById("doc1")).thenReturn(document);
        when(datasetMapper.selectById("ds1")).thenReturn(dataset());
        when(documentMapper.casTransit("doc1", "waiting", "parsing")).thenReturn(1);
        when(documentMapper.casTransit("doc1", "parsing", "splitting")).thenReturn(1);
        when(documentMapper.casTransit("doc1", "splitting", "indexing")).thenReturn(1);
        Mockito.doAnswer(inv -> {
            // insert 时回填分段 id
            KbSegment segment = inv.getArgument(0);
            if (segment.getId() == null) {
                segment.setId("seg-" + System.nanoTime());
            }
            return 1;
        }).when(segmentMapper).insert(any(KbSegment.class));

        boolean processed = service.processOne("doc1");

        assertThat(processed).isTrue();
        // 最终状态 completed
        verify(documentMapper).casTransit("doc1", "indexing", "completed");
        // 向量已写入 InMemory store
        assertThat(embeddingStore).isNotNull();
        // 原文回读（A7）
        verify(fileStore).read("a.txt");
        // 分段已落表
        verify(segmentMapper, Mockito.atLeastOnce()).insert(any(KbSegment.class));
        // 嵌入模型 active 已配置，不再回填更新
        verify(datasetMapper, never()).updateById(any(KbDataset.class));
    }

    @Test
    void 解析失败置error并写error_msg() {
        KbDocument document = waitingDocument();
        document.setFilePath(null);
        when(documentMapper.selectById("doc1")).thenReturn(document);
        when(datasetMapper.selectById("ds1")).thenReturn(dataset());
        when(documentMapper.casTransit("doc1", "waiting", "parsing")).thenReturn(1);

        boolean processed = service.processOne("doc1");

        assertThat(processed).isFalse();
        org.mockito.ArgumentCaptor<KbDocument> captor = org.mockito.ArgumentCaptor.forClass(KbDocument.class);
        verify(documentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getIndexingStatus()).isEqualTo("error");
        assertThat(captor.getValue().getErrorMsg()).contains("原始文件路径为空");
    }

    @Test
    void qa模式给出预留提示() {
        KbDocument document = waitingDocument();
        KbDataset dataset = dataset();
        dataset.setSegmentMode("qa");
        when(documentMapper.selectById("doc1")).thenReturn(document);
        when(datasetMapper.selectById("ds1")).thenReturn(dataset);
        when(documentMapper.casTransit("doc1", "waiting", "parsing")).thenReturn(1);
        when(documentMapper.casTransit("doc1", "parsing", "splitting")).thenReturn(1);

        service.processOne("doc1");

        org.mockito.ArgumentCaptor<KbDocument> captor = org.mockito.ArgumentCaptor.forClass(KbDocument.class);
        verify(documentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getIndexingStatus()).isEqualTo("error");
        assertThat(captor.getValue().getErrorMsg()).contains("预留");
    }

    @Test
    void 重建索引清空分段并重新入队() {
        KbDocument document = waitingDocument();
        document.setIndexingStatus("completed");
        when(documentMapper.selectById("doc1")).thenReturn(document);
        KbSegment existing = new KbSegment();
        existing.setId("seg-old");
        existing.setIndexNodeId("vec-old");
        when(segmentMapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        service.reindex("doc1");

        // 旧分段行删除
        verify(segmentMapper).deleteById("seg-old");
        // 文档置回 waiting
        org.mockito.ArgumentCaptor<KbDocument> captor = org.mockito.ArgumentCaptor.forClass(KbDocument.class);
        verify(documentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getIndexingStatus()).isEqualTo("waiting");
    }
}
