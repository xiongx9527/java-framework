package com.xiongx9527.fw.plus.ai.nativeimpl.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DifyRetrieverTest {

    private MockWebServer server;

    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        baseUrl = server.url("").toString().replaceAll("/+$", "");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** 构造 N 条 segment 的 Dify 检索响应并入队。 */
    private void enqueue(String datasetId, String... ids) {
        StringBuilder records = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                records.append(",");
            }
            records.append("{\"score\":").append(0.9 - i * 0.1)
                    .append(",\"segment\":{\"id\":\"").append(ids[i])
                    .append("\",\"document_id\":\"doc1\",\"position\":").append(i)
                    .append(",\"content\":\"content of ").append(ids[i])
                    .append("\",\"keywords\":[\"k").append(i).append("\"]}}");
        }
        String body = "{\"query\":{\"query\":\"q\"},\"records\":[" + records + "]}";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));
    }

    @Test
    void retrievesAndMapsSegmentsFromSingleDataset() {
        enqueue("ds1", "s1", "s2");
        DifyRetriever retriever = new DifyRetriever(DifyRetrieverConfig.builder()
                .server(baseUrl).apiKey("k").datasetId("ds1").build());
        List<Retrieved<Document>> hits = retriever.retrieve(RetrievalQuery.builder().text("q").build());

        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).getScore()).isGreaterThan(hits.get(1).getScore());
        assertThat(hits.get(0).getItem().getId()).isEqualTo("s1");
        assertThat(hits.get(0).getItem().getContent()).isEqualTo("content of s1");
        assertThat(hits.get(0).getSource()).isEqualTo("ds1");
        assertThat(hits.get(0).getItem().getMetadata())
                .containsEntry("dataset_id", "ds1")
                .containsEntry("document_id", "doc1");
        assertThat(hits.get(0).getItem().getMetadata().get("keywords"))
                .isEqualTo(Arrays.asList("k0"));
    }

    @Test
    void fanOutAcrossDatasetsMergesAndSorts() {
        enqueue("ds1", "s1");
        enqueue("ds2", "s2");
        DifyRetriever retriever = new DifyRetriever(DifyRetrieverConfig.builder()
                .server(baseUrl).apiKey("k")
                .datasetIds(Arrays.asList("ds1", "ds2")).build());
        List<Retrieved<Document>> hits = retriever.retrieve(RetrievalQuery.builder().text("q").build());

        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).getSource()).isIn("ds1", "ds2");
        assertThat(hits.get(1).getSource()).isIn("ds1", "ds2");
    }

    @Test
    void metadataConditionsAreSentInRequestBody() throws Exception {
        enqueue("ds1", "s1");
        DifyRetriever retriever = new DifyRetriever(DifyRetrieverConfig.builder()
                .server(baseUrl).apiKey("k").datasetId("ds1")
                .metadataConditions(Arrays.asList(
                        DifyRetrieverConfig.DifyMetadataCondition.of("category", "equal", "finance")))
                .metadataLogicalOperator("and")
                .build());
        retriever.retrieve(RetrievalQuery.builder().text("q").build());

        okhttp3.mockwebserver.RecordedRequest recorded = server.takeRequest();
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"metadata_filtering_conditions\"");
        assertThat(body).contains("\"name\":\"category\"");
        assertThat(body).contains("\"comparison_operator\":\"equal\"");
        assertThat(body).contains("\"value\":\"finance\"");
        assertThat(body).contains("\"logical_operator\":\"and\"");
    }

    @Test
    void blankQueryReturnsEmpty() {
        DifyRetriever retriever = new DifyRetriever(DifyRetrieverConfig.builder()
                .server(baseUrl).apiKey("k").datasetId("ds1").build());
        assertThat(retriever.retrieve(RetrievalQuery.builder().text("  ").build())).isEmpty();
    }

    @Test
    void unconfiguredConstructorThrows() {
        assertThatThrownBy(() -> new DifyRetriever(DifyRetrieverConfig.builder().server(baseUrl).build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void factoryRejectsUnconfiguredConfig() {
        assertThatThrownBy(() -> DifyRetrieverFactory.create(DifyRetrieverConfig.builder().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
