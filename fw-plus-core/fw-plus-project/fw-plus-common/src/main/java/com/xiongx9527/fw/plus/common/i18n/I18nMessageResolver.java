package com.xiongx9527.fw.plus.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 国际化消息解析器：把「8 位统一状态码 + 参数」解析为当前请求语言下的文案。
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li><b>按状态码查文案</b>：资源文件以状态码为 key（如 {@code 40001001=参数不能为空}），
 *       与 {@code BizException} 的 8 位码一一对应，异常类无需内置任何语言文案。</li>
 *   <li><b>语言协商</b>：语言取自 {@link LocaleContextHolder}（Spring MVC 会按
 *       {@code Accept-Language} 请求头写入），默认回退到中文。</li>
 *   <li><b>静态可用</b>：异常类可能在非 Spring 上下文中被构造（如静态工具、单元测试），
 *       因此通过 {@link #setMessageSource(MessageSource)} 注入而非强制依赖容器；
 *       未注入时回退到中文资源文件的直读（{@link BundleLoader}），保证脱离容器也能出中文。</li>
 * </ul>
 *
 * <h3>资源文件命名</h3>
 * <pre>
 *   i18n/messages.properties         # 中文（默认，不指定 locale 时命中）
 *   i18n/messages_zh_CN.properties   # 中文
 *   i18n/messages_en.properties      # 英文
 * </pre>
 *
 * @author xiongxiao
 */
public final class I18nMessageResolver {

    /** 默认资源文件基名（不含语言后缀） */
    public static final String DEFAULT_BASENAME = "i18n/messages";

    /** 默认语言：中文 */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    private static volatile MessageSource messageSource;

    private I18nMessageResolver() {
    }

    /**
     * 注入 Spring 的 MessageSource（由自动配置调用）。
     *
     * @param source 容器中的 MessageSource，可为 null（表示回退到独立加载）
     */
    public static void setMessageSource(MessageSource source) {
        messageSource = source;
    }

    /**
     * 获取当前注入的 MessageSource，可能为 null。
     */
    public static MessageSource getMessageSource() {
        return messageSource;
    }

    /**
     * 按状态码解析文案（使用当前请求语言）。
     *
     * @param code 8 位统一状态码
     * @param args 文案中的占位参数，对应资源文件里的 {@code {0}}、{@code {1}}
     * @return 解析后的文案；资源缺失时返回 {@code null}
     */
    public static String resolve(int code, Object... args) {
        return resolve(code, LocaleContextHolder.getLocale(), args);
    }

    /**
     * 按状态码 + 指定语言解析文案。
     *
     * @param code   8 位统一状态码
     * @param locale 目标语言，null 时使用默认语言（中文）
     * @param args   文案中的占位参数
     * @return 解析后的文案；资源缺失时返回 {@code null}
     */
    public static String resolve(int code, Locale locale, Object... args) {
        String key = String.valueOf(code);
        Locale target = locale == null ? DEFAULT_LOCALE : locale;
        MessageSource source = messageSource;
        if (source != null) {
            try {
                return source.getMessage(key, args, target);
            } catch (Exception ignored) {
                // 资源缺失或占位符不匹配，回退到独立加载
            }
        }
        return BundleLoader.get(DEFAULT_BASENAME, target, key, args);
    }

    /**
     * 按状态码解析文案并指定默认文案。
     *
     * @param code         8 位统一状态码
     * @param defaultText  资源缺失时使用的兜底文案
     * @param args         文案中的占位参数
     * @return 解析后的文案，缺失时返回 {@code defaultText}
     */
    public static String resolveOrDefault(int code, String defaultText, Object... args) {
        String resolved = resolve(code, args);
        return resolved != null ? resolved : defaultText;
    }

    /**
     * 按 key 解析文案（用于非状态码场景，如校验消息）。
     *
     * @param key  资源 key
     * @param args 文案中的占位参数
     * @return 解析后的文案；资源缺失时返回 {@code null}
     */
    public static String resolveKey(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        MessageSource source = messageSource;
        if (source != null) {
            try {
                return source.getMessage(key, args, locale);
            } catch (Exception ignored) {
                // 回退
            }
        }
        return BundleLoader.get(DEFAULT_BASENAME, locale, key, args);
    }
}
