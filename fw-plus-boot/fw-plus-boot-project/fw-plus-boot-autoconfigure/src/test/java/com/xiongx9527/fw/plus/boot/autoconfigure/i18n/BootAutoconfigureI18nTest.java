package com.xiongx9527.fw.plus.boot.autoconfigure.i18n;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * fw-plus-boot-autoconfigure 国际化文案测试。
 *
 * <p>验证迁移后的用户可见异常文案能按指定语言从 {@code i18n/messages*.properties} 解析出
 * 中文 / 英文，且与迁移前的原文案保持一致。</p>
 */
@DisplayName("Boot 自动配置国际化文案")
class BootAutoconfigureI18nTest {

    private static final Locale EN = Locale.US;

    /**
     * 在指定语言下按 key 解析文案。
     *
     * <p>{@link I18nMessageResolver#resolveKey(String, Object...)} 走
     * {@link LocaleContextHolder}，因此测试期间显式切换并恢复语言。</p>
     */
    private static String resolve(String key, Locale locale, Object... args) {
        Locale previous = LocaleContextHolder.getLocale();
        try {
            LocaleContextHolder.setLocale(locale, true);
            return I18nMessageResolver.resolveKey(key, args);
        } finally {
            LocaleContextHolder.setLocale(previous, true);
        }
    }

    @Test
    @DisplayName("无参数文案：中文（默认）与英文")
    void withoutArgs() {
        assertEquals("key不可修改",
                resolve("fw.boot.resource.keyImmutable", Locale.SIMPLIFIED_CHINESE));
        assertEquals("key cannot be modified",
                resolve("fw.boot.resource.keyImmutable", EN));
    }

    @Test
    @DisplayName("单参数文案：中文与英文")
    void withOneArg() {
        assertEquals("数据源不存在: db1",
                resolve("fw.boot.uid.datasourceNotExists", Locale.SIMPLIFIED_CHINESE, "db1"));
        assertEquals("Datasource does not exist: db1",
                resolve("fw.boot.uid.datasourceNotExists", EN, "db1"));
    }

    @Test
    @DisplayName("占位符内含单引号的文案：中文与英文")
    void withQuotedPlaceholder() {
        assertEquals("未找到key为 'evaluation' 的S3文件存储服务",
                resolve("fw.boot.s3.serviceNotFoundByKey", Locale.SIMPLIFIED_CHINESE, "evaluation"));
        assertEquals("No S3 file storage service found for key 'evaluation'",
                resolve("fw.boot.s3.serviceNotFoundByKey", EN, "evaluation"));
    }

    @Test
    @DisplayName("端口校验文案：中文与英文")
    void portInvalid() {
        assertEquals("端口(port)必须是有效数字: abc",
                resolve("fw.boot.redis.portInvalid", Locale.SIMPLIFIED_CHINESE, "abc"));
        assertEquals("port must be a valid number: abc",
                resolve("fw.boot.redis.portInvalid", EN, "abc"));
    }
}
