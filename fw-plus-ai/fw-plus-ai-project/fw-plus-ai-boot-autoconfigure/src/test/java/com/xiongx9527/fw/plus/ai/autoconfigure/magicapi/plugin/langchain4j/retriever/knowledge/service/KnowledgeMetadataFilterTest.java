package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.ContainsString;
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

    private final KbSearchService service = new KbSearchService(null, null, null, null, null, null, null, true);

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

    private static JSONObject condition(String logical, String name, String operator, String value) {
        JSONObject item = new JSONObject();
        item.put("name", name);
        item.put("comparison_operator", operator);
        item.put("value", value);
        JSONObject condition = new JSONObject();
        condition.put("logical_operator", logical);
        condition.put("conditions", new JSONObject[]{item});
        return condition;
    }

    @Test
    void in操作符构造IsIn() {
        JSONObject condition = condition("and", "lang", "in", "zh,en");
        Filter filter = service.buildMetadataCondition(condition);
        assertThat(filter).isInstanceOf(dev.langchain4j.store.embedding.filter.comparison.IsIn.class);
        assertThat(((dev.langchain4j.store.embedding.filter.comparison.IsIn) filter).key()).isEqualTo("lang");
    }

    @Test
    void 数值比较操作符构造IsGreaterThan() {
        Filter filter = service.buildMetadataCondition(condition("and", "views", "gt", "100"));
        assertThat(filter).isInstanceOf(dev.langchain4j.store.embedding.filter.comparison.IsGreaterThan.class);
    }

    @Test
    void contains构造原生ContainsString并支持内存测试() {
        Filter filter = service.buildMetadataCondition(condition("and", "category", "contains", "faq"));
        assertThat(filter).isInstanceOf(ContainsString.class);
        assertThat(((ContainsString) filter).comparisonValue()).isEqualTo("faq");
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("category", "faq-001");
        assertThat(filter.test(new dev.langchain4j.data.document.Metadata(metadata))).isTrue();
        java.util.Map<String, Object> miss = new java.util.LinkedHashMap<>();
        miss.put("category", "news");
        assertThat(filter.test(new dev.langchain4j.data.document.Metadata(miss))).isFalse();
    }

    @Test
    void not_contains构造Not包装() {
        Filter filter = service.buildMetadataCondition(condition("and", "category", "not contains", "faq"));
        assertThat(filter).isInstanceOf(dev.langchain4j.store.embedding.filter.logical.Not.class);
    }

    @Test
    void is_empty按空串近似() {
        Filter filter = service.buildMetadataCondition(condition("and", "remark", "is empty", null));
        assertThat(filter).isInstanceOf(dev.langchain4j.store.embedding.filter.comparison.IsEqualTo.class);
        assertThat(((dev.langchain4j.store.embedding.filter.comparison.IsEqualTo) filter).comparisonValue())
                .isEqualTo("");
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
