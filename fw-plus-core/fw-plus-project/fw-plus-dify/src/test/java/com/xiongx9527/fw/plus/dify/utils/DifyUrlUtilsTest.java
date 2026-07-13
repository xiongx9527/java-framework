package com.xiongx9527.fw.plus.dify.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class DifyUrlUtilsTest {

    public void testBuildUrlShouldKeepSingleSlashBetweenServerAndRoute() {
        assertEquals(
                "https://api.dify.ai/v1/datasets",
                DifyUrlUtils.buildUrl("https://api.dify.ai", "/v1/datasets", null)
        );
        assertEquals(
                "https://api.dify.ai/v1/datasets",
                DifyUrlUtils.buildUrl("https://api.dify.ai/", "v1/datasets", null)
        );
    }

    public void testBuildUrlShouldKeepExistingBasePath() {
        assertEquals(
                "https://proxy.example.com/dify/v1/chat-messages",
                DifyUrlUtils.buildUrl("https://proxy.example.com/dify/", "/v1/chat-messages", null)
        );
    }

    public void testBuildUrlShouldEncodeQueryWithUtf8() {
        Map<String, Object> query = new LinkedHashMap<String, Object>();
        query.put("keyword", "中文 空格");
        query.put("tag", "a&b");
        query.put("ignored", null);

        assertEquals(
                "https://api.dify.ai/v1/datasets?keyword=%E4%B8%AD%E6%96%87+%E7%A9%BA%E6%A0%BC&tag=a%26b",
                DifyUrlUtils.buildUrl("https://api.dify.ai", "/v1/datasets", query)
        );
    }

    public void testBuildUrlShouldNotAppendQuestionMarkWhenAllQueryValuesAreNull() {
        Map<String, Object> query = new LinkedHashMap<String, Object>();
        query.put("keyword", null);

        assertEquals(
                "https://api.dify.ai/v1/datasets",
                DifyUrlUtils.buildUrl("https://api.dify.ai", "/v1/datasets", query)
        );
    }

    public void testBuildUrlShouldAppendQueryWithAmpersandWhenRouteAlreadyHasQuery() {
        Map<String, Object> query = new LinkedHashMap<String, Object>();
        query.put("keyword", "abc");

        assertEquals(
                "https://api.dify.ai/v1/datasets?page=1&keyword=abc",
                DifyUrlUtils.buildUrl("https://api.dify.ai", "/v1/datasets?page=1", query)
        );
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }
}
