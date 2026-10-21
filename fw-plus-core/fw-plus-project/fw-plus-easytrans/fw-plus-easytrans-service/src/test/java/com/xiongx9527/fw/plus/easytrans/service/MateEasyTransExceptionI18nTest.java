package com.xiongx9527.fw.plus.easytrans.service;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * fw-plus-easytrans-service 模块异常文案的国际化测试。
 *
 * <p>该模块抛的是 JDK 异常（保持类型不变），文案使用具名 dotted key，
 * 这里验证中英文资源均存在且占位符正确替换。</p>
 */
@DisplayName("easytrans-service 异常国际化文案")
class MateEasyTransExceptionI18nTest {

    private static final Locale ZH = Locale.SIMPLIFIED_CHINESE;
    private static final Locale EN = Locale.US;

    @AfterEach
    void reset() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("类找不到：中文")
    void classNotFound_chinese() {
        assertEquals("类找不到：com.demo.Foo",
                I18nMessageResolver.resolveKey("fw.easytrans.MateEasyTransRpcTransService.classNotFound", "com.demo.Foo"));
    }

    @Test
    @DisplayName("类找不到：英文")
    void classNotFound_english() {
        LocaleContextHolder.setLocale(EN);
        assertEquals("Class not found: com.demo.Foo",
                I18nMessageResolver.resolveKey("fw.easytrans.MateEasyTransRpcTransService.classNotFound", "com.demo.Foo"));
    }

    @Test
    @DisplayName("@RpcTrans 注解缺失：中文 / 英文")
    void notMarkedRpcTrans() {
        LocaleContextHolder.setLocale(ZH);
        assertEquals("com.demo.Foo没有被标记@RpcTrans 注解",
                I18nMessageResolver.resolveKey("fw.easytrans.MateEasyTransProxyController.notMarkedRpcTrans", "com.demo.Foo"));
        LocaleContextHolder.setLocale(EN);
        assertEquals("com.demo.Foo is not annotated with @RpcTrans",
                I18nMessageResolver.resolveKey("fw.easytrans.MateEasyTransProxyController.notMarkedRpcTrans", "com.demo.Foo"));
    }

    @Test
    @DisplayName("Redis 未启用：中文 / 英文（无占位符）")
    void redisNotEnabled() {
        LocaleContextHolder.setLocale(ZH);
        assertEquals("使用redis 请将 easy-trans.is-enable-redis 设置为true",
                I18nMessageResolver.resolveKey("fw.easytrans.MateEasyTransDictionaryTransService.redisNotEnabled"));
        LocaleContextHolder.setLocale(EN);
        assertEquals("To use redis, please set easy-trans.is-enable-redis to true",
                I18nMessageResolver.resolveKey("fw.easytrans.MateEasyTransDictionaryTransService.redisNotEnabled"));
    }

    @Test
    @DisplayName("未知 key 解析为 null，不抛异常")
    void unknownKeyReturnsNull() {
        assertNull(I18nMessageResolver.resolveKey("fw.easytrans.NoSuchClass.noSuchCase"));
    }
}
