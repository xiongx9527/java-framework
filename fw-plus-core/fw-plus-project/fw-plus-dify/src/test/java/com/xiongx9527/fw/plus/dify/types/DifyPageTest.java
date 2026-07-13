package com.xiongx9527.fw.plus.dify.types;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.lang.reflect.Method;

/**
 * POJO tests executed by Maven Surefire without adding a JUnit dependency.
 */
public class DifyPageTest {

    public void testShouldParsePageFieldsWithSnakeCaseNames() throws Exception {
        String json = "{"
                + "\"page\":1,"
                + "\"limit\":20,"
                + "\"total\":2,"
                + "\"total_pages\":1,"
                + "\"has_more\":false,"
                + "\"data\":[{\"id\":\"dataset-1\",\"name\":\"知识库\",\"unknown\":\"value\"}]"
                + "}";

        DifyPage page = JSON.parseObject(json, DifyPage.class);

        assertEquals(1, page.getPage(), "page");
        assertEquals(20, page.getLimit(), "limit");
        assertEquals(2L, page.getTotal(), "total");
        assertEquals(1L, page.getTotalPages(), "totalPages");
        assertFalse(readHasMore(page), "hasMore");
        assertNotNull(page.getData(), "data");
        assertEquals(1, page.getData().size(), "data size");
    }

    public void testDynamicJsonShouldKeepUnknownFieldsWhenParsedAsJSONObject() {
        JSONObject object = JSON.parseObject("{\"id\":\"dataset-1\",\"future_field\":{\"enabled\":true}}");

        assertEquals("dataset-1", object.getString("id"), "id");
        assertEquals(Boolean.TRUE, object.getJSONObject("future_field").getBoolean("enabled"), "future_field.enabled");
    }

    public void testBooleanOperationResponseShouldParseSuccessResult() {
        JSONObject object = JSON.parseObject("{\"result\":\"success\"}");

        assertEquals("success", object.getString("result"), "result");
        assertEquals(Boolean.TRUE, "success".equals(object.getString("result")), "operation success");
    }

    private boolean readHasMore(DifyPage page) throws Exception {
        Method method = DifyPage.class.getMethod("isHasMore");
        return (Boolean) method.invoke(page);
    }

    private void assertEquals(Object expected, Object actual, String name) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(name + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private void assertFalse(boolean value, String name) {
        if (value) {
            throw new AssertionError(name + " expected <false> but was <true>");
        }
    }

    private void assertNotNull(Object value, String name) {
        if (value == null) {
            throw new AssertionError(name + " expected non-null value");
        }
    }
}
