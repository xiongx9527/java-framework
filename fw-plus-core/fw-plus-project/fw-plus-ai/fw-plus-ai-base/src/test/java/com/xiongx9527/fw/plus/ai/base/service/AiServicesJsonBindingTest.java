package com.xiongx9527.fw.plus.ai.base.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.output.JsonBindings;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputPort;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiServicesJsonBindingTest {

    static class RecordingLlm extends AbstractLlmPort {
        @Override
        protected void doStream(LlmRequest request, LlmStreamHandler handler) {
            handler.onToken("answer", null);
            handler.onComplete();
        }
    }

    /** 直接返回固定 JSON 的输出端口（测试替身）。 */
    static class FixedJson implements JsonOutputPort {
        private final String value;

        FixedJson(String value) {
            this.value = value;
        }

        @Override
        public String completeJson(LlmRequest request) {
            return value;
        }

        @Override
        public void streamJson(LlmRequest request, LlmStreamHandler handler) {
            if (handler != null) {
                handler.onToken(value, null);
                handler.onComplete();
            }
        }
    }

    /** 记录绑定用的 Type 与 JSON，并返回预设的目标对象（验证接线，不真正解析）。 */
    static class RecordingBindings implements JsonBindings {
        Type lastType;
        String lastJson;
        final Object product;

        RecordingBindings(Object product) {
            this.product = product;
        }

        @Override
        public <T> T bind(String json, Type type) {
            this.lastJson = json;
            this.lastType = type;
            @SuppressWarnings("unchecked")
            T t = (T) product;
            return t;
        }
    }

    static class Person {
        String name;

        Person() {
        }

        Person(String name) {
            this.name = name;
        }
    }

    interface PojoExtractor {
        @JsonResult
        @UserMessage("{{it}}")
        Person extract(String text);
    }

    interface ListExtractor {
        @JsonResult
        @UserMessage("{{it}}")
        List<Person> extractList(String text);
    }

    interface StringExtractor {
        @JsonResult
        @UserMessage("{{it}}")
        String extract(String text);
    }

    @Test
    void jsonResultBindsToPojoWhenBindingsSet() {
        RecordingBindings bindings = new RecordingBindings(new Person("bob"));
        PojoExtractor ex = AiServices.builder(PojoExtractor.class)
                .llm(new RecordingLlm())
                .jsonOutput(new FixedJson("{\"name\":\"bob\"}"))
                .jsonBindings(bindings)
                .build();
        Person p = ex.extract("anything");
        assertNotNull(p);
        assertEquals("bob", p.name);
        assertEquals("{\"name\":\"bob\"}", bindings.lastJson);
        assertSame(Person.class, bindings.lastType);
    }

    @Test
    void jsonResultBindsGenericType() {
        RecordingBindings bindings = new RecordingBindings(
                Arrays.asList(new Person("a"), new Person("b")));
        ListExtractor ex = AiServices.builder(ListExtractor.class)
                .llm(new RecordingLlm())
                .jsonOutput(new FixedJson("[{\"name\":\"a\"},{\"name\":\"b\"}]"))
                .jsonBindings(bindings)
                .build();
        List<Person> list = ex.extractList("anything");
        assertNotNull(list);
        assertEquals(2, list.size());
        assertTrue(bindings.lastType instanceof ParameterizedType);
        assertEquals(List.class, ((ParameterizedType) bindings.lastType).getRawType());
    }

    @Test
    void jsonResultStringReturnUnchanged() {
        // 返回类型为 String 时即使配置了 bindings 也直接返回原始 JSON 文本，不触发绑定
        RecordingBindings bindings = new RecordingBindings(new Person("x"));
        StringExtractor s = AiServices.builder(StringExtractor.class)
                .llm(new RecordingLlm())
                .jsonOutput(new FixedJson("{\"name\":\"bob\"}"))
                .jsonBindings(bindings)
                .build();
        assertEquals("{\"name\":\"bob\"}", s.extract("anything"));
        // 未触发绑定
        assertEquals(null, bindings.lastType);
    }

    @Test
    void jsonResultPojoWithoutBindingsThrows() {
        PojoExtractor ex = AiServices.builder(PojoExtractor.class)
                .llm(new RecordingLlm())
                .jsonOutput(new FixedJson("{\"name\":\"bob\"}"))
                .build();
        IllegalStateException ex2 = assertThrows(IllegalStateException.class,
                () -> ex.extract("anything"));
        assertTrue(ex2.getMessage().contains("JsonBindings"));
    }
}
