package com.xiongx9527.fw.plus.ai.base.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies @P constraint enforcement in DefaultToolExecutor: required / pattern / length range /
 * numeric range / JSON shape, plus the @P.message() override and the stable errorKey on the thrown
 * ToolArgumentException.
 */
public class DefaultToolExecutorValidationTest {

    static class JsonCodec implements ToolArgumentCodec {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public Map<String, Object> decode(String arguments) {
            if (arguments == null || arguments.trim().isEmpty()) {
                return new HashMap<String, Object>();
            }
            try {
                Object parsed = mapper.readValue(arguments, Object.class);
                if (!(parsed instanceof Map)) {
                    throw new IllegalArgumentException("arguments must be a JSON object");
                }
                return (Map<String, Object>) parsed;
            } catch (Exception e) {
                throw new IllegalArgumentException("decode failed: " + e.getMessage(), e);
            }
        }

        @Override
        public String encode(Object value) {
            try {
                return mapper.writeValueAsString(value);
            } catch (Exception e) {
                throw new IllegalArgumentException("encode failed", e);
            }
        }
    }

    static class Tools {
        @Tool("need")
        public String need(@P(value = "name", required = true) String name) {
            return name;
        }

        @Tool("pat")
        public String pat(@P(value = "code", pattern = "[A-Z]+") String code) {
            return code;
        }

        @Tool("len")
        public String len(@P(value = "s", minLength = 3, maxLength = 5) String s) {
            return s;
        }

        @Tool("num")
        public String num(@P(value = "n", min = "10", max = "20") String n) {
            return n;
        }

        @Tool("obj")
        public String obj(@P(value = "obj", jsonShape = "object") String obj) {
            return obj;
        }

        @Tool("arr")
        public String arr(@P(value = "arr", jsonShape = "array") String arr) {
            return arr;
        }

        @Tool("custom")
        public String custom(@P(value = "custom", required = true, message = "custom msg") String custom) {
            return custom;
        }
    }

    private DefaultToolExecutor executor() {
        return new DefaultToolExecutor(new Tools(), new JsonCodec());
    }

    @Test
    void requiredMissingThrowsWithKey() {
        ToolArgumentException ex = assertThrows(ToolArgumentException.class,
                () -> executor().execute(ToolExecutionRequest.of("need", "{}")));
        assertEquals(ToolArgumentException.KEY_REQUIRED, ex.getErrorKey());
    }

    @Test
    void patternMismatchThrowsWithKey() {
        ToolArgumentException ex = assertThrows(ToolArgumentException.class,
                () -> executor().execute(ToolExecutionRequest.of("pat", "{\"code\":\"abc\"}")));
        assertEquals(ToolArgumentException.KEY_PATTERN, ex.getErrorKey());
    }

    @Test
    void minLengthThrowsWithKey() {
        ToolArgumentException ex = assertThrows(ToolArgumentException.class,
                () -> executor().execute(ToolExecutionRequest.of("len", "{\"s\":\"ab\"}")));
        assertEquals(ToolArgumentException.KEY_MIN_LENGTH, ex.getErrorKey());
    }

    @Test
    void maxLengthThrowsWithKey() {
        ToolArgumentException ex = assertThrows(ToolArgumentException.class,
                () -> executor().execute(ToolExecutionRequest.of("len", "{\"s\":\"abcdef\"}")));
        assertEquals(ToolArgumentException.KEY_MAX_LENGTH, ex.getErrorKey());
    }

    @Test
    void minNumericThrowsWithKey() {
        ToolArgumentException ex = assertThrows(ToolArgumentException.class,
                () -> executor().execute(ToolExecutionRequest.of("num", "{\"n\":\"5\"}")));
        assertEquals(ToolArgumentException.KEY_MIN, ex.getErrorKey());
    }

    @Test
    void maxNumericThrowsWithKey() {
        ToolArgumentException ex = assertThrows(ToolArgumentException.class,
                () -> executor().execute(ToolExecutionRequest.of("num", "{\"n\":\"25\"}")));
        assertEquals(ToolArgumentException.KEY_MAX, ex.getErrorKey());
    }

    @Test
    void jsonShapeObjectThrowsWithKey() {
        ToolArgumentException ex = assertThrows(ToolArgumentException.class,
                () -> executor().execute(ToolExecutionRequest.of("obj", "{\"obj\":\"[1,2]\"}")));
        assertEquals(ToolArgumentException.KEY_JSON_OBJECT, ex.getErrorKey());
    }

    @Test
    void jsonShapeArrayThrowsWithKey() {
        ToolArgumentException ex = assertThrows(ToolArgumentException.class,
                () -> executor().execute(ToolExecutionRequest.of("arr", "{\"arr\":\"{}\"}")));
        assertEquals(ToolArgumentException.KEY_JSON_ARRAY, ex.getErrorKey());
    }

    @Test
    void customMessageOverrideUsed() {
        ToolArgumentException ex = assertThrows(ToolArgumentException.class,
                () -> executor().execute(ToolExecutionRequest.of("custom", "{}")));
        assertEquals("custom msg", ex.getMessage());
        assertEquals(ToolArgumentException.KEY_REQUIRED, ex.getErrorKey());
    }

    @Test
    void validArgsExecute() {
        String r = executor().execute(ToolExecutionRequest.of("need", "{\"name\":\"x\"}"));
        assertNotNull(r);
    }
}
