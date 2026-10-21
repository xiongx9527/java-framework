package com.xiongx9527.fw.plus.easytrans.common;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * fw-plus-easytrans-common 模块异常文案的国际化测试。
 *
 * <p>该模块抛的是 JDK 异常（保持类型不变），文案使用具名 dotted key。</p>
 */
@DisplayName("easytrans-common 异常国际化文案")
class MateEasyTransCommonExceptionI18nTest {

    private static final Locale ZH = Locale.SIMPLIFIED_CHINESE;
    private static final Locale EN = Locale.US;

    @AfterEach
    void reset() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("唯一 key 缺失：中文")
    void uniqueKeyNotFound_chinese() {
        assertEquals("未找到com.demo.Foo的唯一key,请设置id或者实现getUniqueValue方法",
                I18nMessageResolver.resolveKey("fw.easytrans.MateEasyTransDTO.uniqueKeyNotFound", "com.demo.Foo"));
    }

    @Test
    @DisplayName("唯一 key 缺失：英文")
    void uniqueKeyNotFound_english() {
        LocaleContextHolder.setLocale(EN);
        assertEquals("Unique key not found for com.demo.Foo, please set an id or implement getUniqueValue",
                I18nMessageResolver.resolveKey("fw.easytrans.MateEasyTransDTO.uniqueKeyNotFound", "com.demo.Foo"));
    }

    @Test
    @DisplayName("未知 key 解析为 null，不抛异常")
    void unknownKeyReturnsNull() {
        assertNull(I18nMessageResolver.resolveKey("fw.easytrans.MateEasyTransDTO.noSuchCase"));
    }
}
