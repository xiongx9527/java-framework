package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsNotEqualTo;
import dev.langchain4j.store.embedding.filter.logical.And;
import dev.langchain4j.store.embedding.filter.logical.Or;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dify metadata_condition → langchain4j Filter 的语义测试（设计 §3.1/§3.4：M1 支持 is/is not，and/or 组合）。
 */
class KnowledgeMetadataFilterTest {

    private final KbSearchService service = new KbSearchService(null, null, null, null, null, null, null);

    @Test
    void 空条件返回null() {
        assertThat(service.buildMetadataCondition(null)).isNull();
        assertThat(service.buildMetadataCondition(new JSONObject())).isNull();
    }

    @Test
    void and条件构造等值过滤() {
        JSONObject condition = new JSONObject();
        condition.put("logical_operator", "and");
        JSONObject first = new JSONObject();
        first.put("name", "lang");
        first.put("comparison_operator", "is");
        first.put("value", "zh");
        JSONObject second = new JSONObject();
        second.put("name", "category");
        second.put("comparison_operator", "is");
        second.put("value", "faq");
        condition.put("conditions", new JSONObject[]{first, second});

        Filter filter = service.buildMetadataCondition(condition);
        assertThat(filter).isInstanceOf(And.class);
        And and = (And) filter;
        assertThat(and.left()).isInstanceOf(IsEqualTo.class);
        assertThat(and.right()).isInstanceOf(IsEqualTo.class);
        IsEqualTo left = (IsEqualTo) and.left();
        assertThat(left.key()).isEqualTo("lang");
        assertThat(left.comparisonValue()).isEqualTo("zh");
    }

    @Test
    void or条件与is_not操作符() {
        JSONObject condition = new JSONObject();
        condition.put("logical_operator", "or");
        JSONObject first = new JSONObject();
        first.put("name", "lang");
        first.put("comparison_operator", "is not");
        first.put("value", "en");
        JSONObject second = new JSONObject();
        second.put("name", "archived_flag");
        second.put("value", "1");
        condition.put("conditions", new JSONObject[]{first, second});

        Filter filter = service.buildMetadataCondition(condition);
        assertThat(filter).isInstanceOf(Or.class);
        Or or = (Or) filter;
        assertThat(or.left()).isInstanceOf(IsNotEqualTo.class);
        assertThat(or.right()).isInstanceOf(IsEqualTo.class);
    }

    @Test
    void 缺省操作符按is处理() {
        JSONObject condition = new JSONObject();
        JSONObject only = new JSONObject();
        only.put("name", "owner");
        only.put("value", "biz");
        condition.put("conditions", new JSONObject[]{only});

        Filter filter = service.buildMetadataCondition(condition);
        assertThat(filter).isInstanceOf(IsEqualTo.class);
        assertThat(((IsEqualTo) filter).key()).isEqualTo("owner");
    }
}
