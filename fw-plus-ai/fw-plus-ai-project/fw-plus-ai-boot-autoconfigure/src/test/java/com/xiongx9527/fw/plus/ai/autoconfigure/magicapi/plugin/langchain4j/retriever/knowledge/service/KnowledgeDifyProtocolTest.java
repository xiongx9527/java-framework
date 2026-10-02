package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web.KbOpenApiController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dify External Knowledge API 响应格式测试（设计 §3.4 / 验收 A16）。
 */
class KnowledgeDifyProtocolTest {

    private Object invokeOkRecords(List<KbSearchHit> hits) throws Exception {
        Method method = KbOpenApiController.class.getDeclaredMethod("okRecords", List.class);
        method.setAccessible(true);
        return method.invoke(null, hits);
    }

    @Test
    void 无命中返回空records() throws Exception {
        Object response = invokeOkRecords(Collections.emptyList());
        JSONObject json = new JSONObject(response instanceof JSONObject ? (JSONObject) response
                : JSONObject.from(response));
        assertThat(json.getJSONArray("records")).isEmpty();
        assertThat(json.getJSONObject("records")).isNull();
    }

    @Test
    void 命中记录字段齐备且metadata恒为对象() throws Exception {
        KbSearchHit hit = new KbSearchHit("ds1", "doc1", "手册.pdf", "seg1", 0,
                "分段内容", null, null, 0.87, null, null);
        Object response = invokeOkRecords(Arrays.asList(hit));
        JSONObject json = JSONObject.from(response);
        JSONArray records = json.getJSONArray("records");
        assertThat(records).hasSize(1);
        JSONObject record = records.getJSONObject(0);
        // 四字段：content/score/title/metadata
        assertThat(record.containsKey("content")).isTrue();
        assertThat(record.containsKey("score")).isTrue();
        assertThat(record.containsKey("title")).isTrue();
        assertThat(record.containsKey("metadata")).isTrue();
        assertThat(record.getString("content")).isEqualTo("分段内容");
        assertThat(record.getDoubleValue("score")).isEqualTo(0.87);
        assertThat(record.getString("title")).isEqualTo("手册.pdf");
        // metadata 恒为 JSON 对象（不返回 null）
        assertThat(record.getJSONObject("metadata")).isNotNull();
    }

    @Test
    void metadata非空时逐字段保留() throws Exception {
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("lang", "zh");
        metadata.put("category", "faq");
        KbSearchHit hit = new KbSearchHit("ds1", "doc1", "faq.json", "seg2", 3,
                "内容", null, null, 0.5, null, metadata);
        Object response = invokeOkRecords(Arrays.asList(hit));
        JSONObject json = JSONObject.from(response);
        JSONObject record = json.getJSONArray("records").getJSONObject(0);
        assertThat(record.getJSONObject("metadata").getString("lang")).isEqualTo("zh");
        assertThat(record.getJSONObject("metadata").getString("category")).isEqualTo("faq");
    }
}
