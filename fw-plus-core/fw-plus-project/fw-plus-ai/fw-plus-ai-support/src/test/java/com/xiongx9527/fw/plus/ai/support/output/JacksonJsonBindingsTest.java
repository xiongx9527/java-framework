package com.xiongx9527.fw.plus.ai.support.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputException;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputPort;
import com.xiongx9527.fw.plus.ai.base.service.AiServices;
import com.xiongx9527.fw.plus.ai.base.service.JsonResult;
import com.xiongx9527.fw.plus.ai.base.service.UserMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class JacksonJsonBindingsTest {

    static class Person {
        public String name;
        public int age;

        public Person() {
        }
    }

    @Test
    void bindsSimplePojo() {
        JacksonJsonBindings bindings = new JacksonJsonBindings();
        Person p = bindings.bind("{\"name\":\"bob\",\"age\":30}", Person.class);
        assertEquals("bob", p.name);
        assertEquals(30, p.age);
    }

    @Test
    void bindsGenericList() {
        JacksonJsonBindings bindings = new JacksonJsonBindings();
        List<Person> list = bindings.bind("[{\"name\":\"a\",\"age\":1},{\"name\":\"b\",\"age\":2}]",
                new com.fasterxml.jackson.core.type.TypeReference<List<Person>>() {}.getType());
        assertEquals(2, list.size());
        assertEquals("a", list.get(0).name);
        assertEquals(2, list.get(1).age);
    }

    @Test
    void nullJsonReturnsNull() {
        JacksonJsonBindings bindings = new JacksonJsonBindings();
        assertNull(bindings.bind(null, Person.class));
    }

    @Test
    void invalidJsonThrowsJsonOutputException() {
        JacksonJsonBindings bindings = new JacksonJsonBindings();
        JsonOutputException ex = assertThrows(JsonOutputException.class,
                () -> bindings.bind("{not json}", Person.class));
        assertTrue(ex.getCause() != null);
    }

    @Test
    void customObjectMapperAccepted() {
        JacksonJsonBindings bindings = new JacksonJsonBindings(new ObjectMapper());
        Person p = bindings.bind("{\"name\":\"c\",\"age\":5}", Person.class);
        assertEquals("c", p.name);
    }

    /** 测试用替身：返回固定 JSON 的稳定输出端口。 */
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

    static class RecordingLlm extends AbstractLlmPort {
        @Override
        protected void doStream(LlmRequest request, LlmStreamHandler handler) {
            handler.onToken("answer", null);
            handler.onComplete();
        }
    }

    interface PersonExtractor {
        @JsonResult
        @UserMessage("{{it}}")
        Person extract(String text);
    }

    @Test
    void aiServicesBindsPojoViaJackson() {
        PersonExtractor ex = AiServices.builder(PersonExtractor.class)
                .llm(new RecordingLlm())
                .jsonOutput(new FixedJson("{\"name\":\"bob\",\"age\":30}"))
                .jsonBindings(new JacksonJsonBindings())
                .build();
        Person p = ex.extract("anything");
        assertEquals("bob", p.name);
        assertEquals(30, p.age);
    }
}
