package com.xiongx9527.fw.plus.boot.common.debounce;

import com.xiongx9527.fw.plus.common.debounce.Debounce;
import com.xiongx9527.fw.plus.common.debounce.DebounceProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 校验 {@link DebounceAspect#effectiveWindow} 的窗口解析：注解值覆盖全局，并收敛到上限。 */
class DebounceEffectiveWindowTest {

    private Debounce annotation(long windowMs) {
        return new Debounce() {
            @Override public Class<Debounce> annotationType() { return Debounce.class; }
            @Override public boolean user() { return true; }
            @Override public String[] args() { return new String[0]; }
            @Override public String[] spel() { return new String[0]; }
            @Override public String message() { return ""; }
            @Override public long windowMs() { return windowMs; }
        };
    }

    @Test
    @DisplayName("未指定 windowMs（缺省 -1）时沿用全局窗口")
    void defaultFallsBackToGlobal() {
        assertEquals(3000L, DebounceAspect.effectiveWindow(annotation(-1L), 3000L));
    }

    @Test
    @DisplayName("windowMs=0 也回退全局窗口")
    void zeroFallsBackToGlobal() {
        assertEquals(3000L, DebounceAspect.effectiveWindow(annotation(0L), 3000L));
    }

    @Test
    @DisplayName("正数 windowMs 覆盖全局")
    void positiveOverridesGlobal() {
        assertEquals(800L, DebounceAspect.effectiveWindow(annotation(800L), 3000L));
    }

    @Test
    @DisplayName("超过上限的 windowMs 被收敛到上限")
    void clampedToMax() {
        assertEquals(DebounceProperties.MAX_WINDOW_MS,
                DebounceAspect.effectiveWindow(annotation(DebounceProperties.MAX_WINDOW_MS + 1_000_000L), 3000L));
    }
}
