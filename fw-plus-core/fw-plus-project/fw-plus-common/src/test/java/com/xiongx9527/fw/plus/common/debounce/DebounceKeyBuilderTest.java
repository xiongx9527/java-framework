package com.xiongx9527.fw.plus.common.debounce;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DebounceKeyBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DebounceKeyBuilder builder = new DebounceKeyBuilder(objectMapper);

    @SuppressWarnings("unused")
    static class Fixture {
        @Debounce
        public String update(Long id, String body) { return ""; }

        @Debounce(user = false)
        public String updateNoUser(Long id, String body) { return ""; }

        @Debounce(args = {"id"})
        public String byId(Long id, String body) { return ""; }

        @Debounce(spel = {"#p0 + '-' + #user"})
        public String bySpel(Long id) { return ""; }
    }

    private Debounce ann(Method method) {
        return method.getAnnotation(Debounce.class);
    }

    @Test
    void userDimensionSeparatesKeys() throws Exception {
        Method update = Fixture.class.getDeclaredMethod("update", Long.class, String.class);
        Debounce a = ann(update);
        String k1 = builder.buildKey(update, new Object[]{1L, "x"}, a, "1");
        String k2 = builder.buildKey(update, new Object[]{1L, "x"}, a, "2");
        assertNotEquals(k1, k2, "不同用户应生成不同键");
        assertEquals(k1, builder.buildKey(update, new Object[]{1L, "x"}, a, "1"), "相同输入应幂等");
    }

    @Test
    void userFalseMergesAcrossUsers() throws Exception {
        Method update = Fixture.class.getDeclaredMethod("updateNoUser", Long.class, String.class);
        Debounce a = ann(update);
        String k1 = builder.buildKey(update, new Object[]{1L, "x"}, a, "1");
        String k2 = builder.buildKey(update, new Object[]{1L, "x"}, a, "2");
        assertEquals(k1, k2, "user=false 时不同用户应共享同一键");
    }

    @Test
    void argsDimensionIsolates() throws Exception {
        Method byId = Fixture.class.getDeclaredMethod("byId", Long.class, String.class);
        Debounce a = ann(byId);
        String k1 = builder.buildKey(byId, new Object[]{1L, "x"}, a, "5");
        String k2 = builder.buildKey(byId, new Object[]{2L, "x"}, a, "5");
        assertNotEquals(k1, k2, "不同 id 应生成不同键");
        assertEquals(k1, builder.buildKey(byId, new Object[]{1L, "y"}, a, "5"), "仅 body 不同、id 相同时保持同一键");
    }

    @Test
    void spelDimensionWorks() throws Exception {
        Method bySpel = Fixture.class.getDeclaredMethod("bySpel", Long.class);
        Debounce a = ann(bySpel);
        String k1 = builder.buildKey(bySpel, new Object[]{10L}, a, "7");
        String k2 = builder.buildKey(bySpel, new Object[]{10L}, a, "8");
        assertNotEquals(k1, k2, "SpEL 含 #user，不同用户应不同键");
        String k3 = builder.buildKey(bySpel, new Object[]{11L}, a, "7");
        assertNotEquals(k1, k3, "SpEL 含 #p0，不同参数应不同键");
    }

    @Test
    void anonFallbackStable() throws Exception {
        Method update = Fixture.class.getDeclaredMethod("update", Long.class, String.class);
        Debounce a = ann(update);
        String k1 = builder.buildKey(update, new Object[]{1L, "x"}, a, null);
        String k2 = builder.buildKey(update, new Object[]{1L, "x"}, a, null);
        assertEquals(k1, k2, "未登录（null）应稳定回退为同一 anon 键");
    }

    @Test
    void nullObjectMapperFallsBackToToString() throws Exception {
        Method byId = Fixture.class.getDeclaredMethod("byId", Long.class, String.class);
        Debounce a = ann(byId);
        String key = new DebounceKeyBuilder(null).buildKey(byId, new Object[]{1L, "x"}, a, "5");
        assertEquals(key, new DebounceKeyBuilder(null).buildKey(byId, new Object[]{1L, "x"}, a, "5"));
    }
}
