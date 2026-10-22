package com.xiongx9527.fw.plus.common.i18n;

import com.xiongx9527.fw.plus.common.enums.ErrorCategory;
import com.xiongx9527.fw.plus.common.enums.ErrorSource;
import com.xiongx9527.fw.plus.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 国际化解析链路的回归测试。
 *
 * <p>覆盖两层解析路径：</p>
 * <ol>
 *   <li><b>独立加载</b>（无 Spring 容器）：直接读 classpath 上的
 *       {@code i18n/messages*.properties}。</li>
 *   <li><b>容器注入</b>：注入自定义 {@link org.springframework.context.MessageSource} 后优先走容器。</li>
 * </ol>
 */
@DisplayName("I18nMessageResolver 国际化解析")
class I18nMessageResolverTest {

    @AfterEach
    void reset() {
        LocaleContextHolder.resetLocaleContext();
        I18nMessageResolver.setMessageSource(null);
        BundleLoader.clear();
    }

    @Test
    @DisplayName("无容器时按状态码直读资源：中文")
    void resolveByCode_fromBundle_chinese() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("行权限配置损坏：row.xml", I18nMessageResolver.resolve(50_003_001, "row.xml"));
    }

    @Test
    @DisplayName("无容器时按状态码直读资源：英文")
    void resolveByCode_fromBundle_english() {
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("Row permission configuration is corrupted: row.xml",
                I18nMessageResolver.resolve(50_003_001, "row.xml"));
    }

    @Test
    @DisplayName("locale 为 ROOT 时回退中文默认资源")
    void resolveByCode_rootLocaleFallsBackToChinese() {
        LocaleContextHolder.setLocale(Locale.ROOT);
        assertEquals("参数缺失：id", I18nMessageResolver.resolve(40_001_001, "id"));
    }

    @Test
    @DisplayName("资源缺失时返回 null，不抛异常")
    void resolveByCode_missingKeyReturnsNull() {
        assertNull(I18nMessageResolver.resolve(12_345_678));
    }

    @Test
    @DisplayName("具名 key 解析（非状态码场景）")
    void resolveNamedKey() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("读取请求体失败", I18nMessageResolver.resolveKey("fw.common.web.readRequestBodyFailed"));
    }

    @Test
    @DisplayName("BizException 以状态码 + 参数构造时，getMessage 按当前语言解析")
    void bizException_resolvesMessageByLocale() {
        BizException exception = BizException.i18n(ErrorSource.CLIENT, ErrorCategory.REQUEST, 1, "id");

        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("参数缺失：id", exception.getMessage());

        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("Missing parameter: id", exception.getMessage());
    }

    @Test
    @DisplayName("BizException 显式指定语言解析")
    void bizException_resolvesMessageForExplicitLocale() {
        BizException exception = BizException.i18n(ErrorSource.SERVER, ErrorCategory.AUTHORIZATION, 1, "row.xml");
        assertEquals("Row permission configuration is corrupted: row.xml", exception.getMessage(Locale.US));
        assertEquals("行权限配置损坏：row.xml", exception.getMessage(Locale.SIMPLIFIED_CHINESE));
    }

    @Test
    @DisplayName("BizException 使用字面量文案时保持原样（向后兼容）")
    void bizException_literalMessageUntouched() {
        BizException exception = new BizException(40_001_001, "自定义文案");
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("自定义文案", exception.getMessage());
        assertEquals("自定义文案", exception.getRawMessage());
        assertNull(exception.getArgs());
    }

    @Test
    @DisplayName("注入 MessageSource 后优先走容器")
    void resolve_usesInjectedMessageSource() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("40001001", Locale.SIMPLIFIED_CHINESE, "来自容器的文案");
        I18nMessageResolver.setMessageSource(source);

        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("来自容器的文案", I18nMessageResolver.resolve(40_001_001));
    }

    @Test
    @DisplayName("中文与英文资源内容不同")
    void bundleVariantsDiffer() {
        String zh = I18nMessageResolver.resolve(50_009_001, Locale.SIMPLIFIED_CHINESE, "x");
        String en = I18nMessageResolver.resolve(50_009_001, Locale.US, "x");
        assertNotNull(zh);
        assertTrue(zh.contains("数据库"), "中文应包含关键字： " + zh);
        assertNotEquals(zh, en);
    }

    @Test
    @DisplayName("解析时可带占位参数")
    void resolve_withArguments() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("Redis 连接地址非法：redis://bad",
                I18nMessageResolver.resolve(50_016_001, "redis://bad"));
    }
}
