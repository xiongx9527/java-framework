package com.xiongx9527.fw.plus.common.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多模块资源合并测试。
 *
 * <p>验证 {@link ComposableMessageSource} 能从 classpath 扫描出 {@code i18n/} 下的资源基名，
 * 并支持中文默认（{@code messages.properties}）与英文（{@code messages_en.properties}）两级回退。</p>
 */
@DisplayName("ComposableMessageSource 多模块资源合并")
class ComposableMessageSourceTest {

    private ComposableMessageSource source;

    @AfterEach
    void tearDown() {
        source = null;
    }

    @Test
    @DisplayName("初始化后能解析默认（中文）资源")
    void discoversDefaultResource() {
        source = ComposableMessageSource.create(StandardCharsets.UTF_8.name(),
                Locale.SIMPLIFIED_CHINESE, 0, "classpath*:i18n/");
        assertEquals("读取请求体失败",
                source.getMessage("fw.common.web.readRequestBodyFailed", null, Locale.SIMPLIFIED_CHINESE));
    }

    @Test
    @DisplayName("按状态码取中文默认文案（messages.properties）")
    void resolvesChineseDefault() {
        source = ComposableMessageSource.create(StandardCharsets.UTF_8.name(),
                Locale.SIMPLIFIED_CHINESE, 0, "classpath*:i18n/");
        String message = source.getMessage("50003001", new Object[]{"row.xml"}, Locale.SIMPLIFIED_CHINESE);
        assertEquals("行权限配置损坏：row.xml", message);
    }

    @Test
    @DisplayName("按状态码取英文文案（messages_en.properties）")
    void resolvesEnglish() {
        source = ComposableMessageSource.create(StandardCharsets.UTF_8.name(),
                Locale.SIMPLIFIED_CHINESE, 0, "classpath*:i18n/");
        String message = source.getMessage("50003001", new Object[]{"row.xml"}, Locale.US);
        assertEquals("Row permission configuration is corrupted: row.xml", message);
    }

    @Test
    @DisplayName("具名 key 同样可解析")
    void resolvesNamedKey() {
        source = ComposableMessageSource.create(StandardCharsets.UTF_8.name(),
                Locale.SIMPLIFIED_CHINESE, 0, "classpath*:i18n/");
        assertEquals("读取请求体失败",
                source.getMessage("fw.common.web.readRequestBodyFailed", null, Locale.SIMPLIFIED_CHINESE));
    }

    @Test
    @DisplayName("中文默认资源无中文乱码")
    void chineseNotGarbled() {
        source = ComposableMessageSource.create(StandardCharsets.UTF_8.name(),
                Locale.SIMPLIFIED_CHINESE, 0, "classpath*:i18n/");
        String message = source.getMessage("40001001", new Object[]{"id"}, Locale.SIMPLIFIED_CHINESE);
        assertNotNull(message);
        assertTrue(message.contains("参数缺失"), "中文应正确解码，实际: " + message);
    }
}
