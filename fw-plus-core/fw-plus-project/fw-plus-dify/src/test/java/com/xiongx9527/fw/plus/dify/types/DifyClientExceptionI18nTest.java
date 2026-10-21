package com.xiongx9527.fw.plus.dify.types;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dify 客户端异常的国际化文案测试。
 *
 * <p>验证 {@link DifyClientException#of(int, Object...)} 以「具体错误码 + 参数」构造时，
 * 能按指定语言解析出中文 / 英文文案。</p>
 */
@DisplayName("Dify 客户端异常国际化文案")
class DifyClientExceptionI18nTest {

    private static final Locale ZH = Locale.SIMPLIFIED_CHINESE;
    private static final Locale EN = Locale.US;

    @AfterEach
    void reset() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("登录失败：中文")
    void loginFailed_chinese() {
        DifyClientException exception = DifyClientException.of(
                DifyClientException.LOGIN_FAILED, "{\"result\":\"fail\"}");
        assertEquals(50_008_101, exception.getCode());
        assertEquals("登录失败: {\"result\":\"fail\"}", exception.getMessage(ZH));
    }

    @Test
    @DisplayName("登录失败：英文")
    void loginFailed_english() {
        DifyClientException exception = DifyClientException.of(
                DifyClientException.LOGIN_FAILED, "{\"result\":\"fail\"}");
        assertEquals("Login failed: {\"result\":\"fail\"}", exception.getMessage(EN));
    }

    @Test
    @DisplayName("字面量构造器仍保留原有行为")
    void literalConstructorKeepsBehavior() {
        DifyClientException exception = new DifyClientException("timeout");
        assertEquals(50_008_002, exception.getCode());
        assertEquals("timeout", exception.getMessage());
    }
}
