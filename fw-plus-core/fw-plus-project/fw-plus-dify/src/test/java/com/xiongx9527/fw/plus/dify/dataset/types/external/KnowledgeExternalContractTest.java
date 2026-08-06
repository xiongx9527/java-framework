package com.xiongx9527.fw.plus.dify.dataset.types.external;

import com.alibaba.fastjson.JSON;

import java.util.Map;

public class KnowledgeExternalContractTest {

    public void testShouldReadIntegerTopKAndMetadataCondition() {
        KnowledgeArgs args = JSON.parseObject(fixture("dify-external-request.json"), KnowledgeArgs.class);

        assertEquals(Integer.valueOf(5), args.getRetrieval_setting().getTop_k());
        assertEquals("and", args.getMetadata_condition().getLogical_operator());
        assertEquals("source_type", args.getMetadata_condition().getConditions().get(0).getName());
    }

    public void testShouldSerializeOfficialErrorFieldNames() {
        String json = JSON.toJSONString(new KnowledgeException(1001, "invalid authorization"));

        assertTrue(json.contains("\"error_code\":1001"));
        assertTrue(json.contains("\"error_msg\":\"invalid authorization\""));
        assertFalse(json.contains("error_message"));
    }

    public void testShouldSerializeMissingMetadataAsObject() {
        String json = JSON.toJSONString(KnowledgeResult.Record.builder()
                .content("Tinyflow knowledge")
                .build());

        assertTrue(json.contains("\"metadata\":{}"));
    }

    public void testShouldUseOfficialErrorFieldNamesInToObject() {
        Map<?, ?> error = (Map<?, ?>) new KnowledgeException(1001, "invalid authorization").toObject();

        assertEquals(Integer.valueOf(1001), error.get("error_code"));
        assertEquals("invalid authorization", error.get("error_msg"));
        assertFalse(error.containsKey("error_message"));
    }

    private String fixture(String name) {
        if (!"dify-external-request.json".equals(name)) {
            throw new IllegalArgumentException("Unknown fixture: " + name);
        }
        return "{\"knowledge_id\":\"knowledge-1\",\"query\":\"What is Tinyflow?\","
                + "\"retrieval_setting\":{\"top_k\":5,\"score_threshold\":0.5},"
                + "\"metadata_condition\":{\"logical_operator\":\"and\",\"conditions\":[{"
                + "\"name\":\"source_type\",\"comparison_operator\":\"is\",\"value\":\"manual\"}]}}";
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private void assertTrue(boolean actual) {
        if (!actual) {
            throw new AssertionError("expected:<true> but was:<false>");
        }
    }

    private void assertFalse(boolean actual) {
        if (actual) {
            throw new AssertionError("expected:<false> but was:<true>");
        }
    }
}
