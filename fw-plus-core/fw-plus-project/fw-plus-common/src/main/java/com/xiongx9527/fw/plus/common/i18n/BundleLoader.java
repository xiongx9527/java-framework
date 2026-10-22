package com.xiongx9527.fw.plus.common.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源包直读工具：在 Spring 容器尚不可用（静态上下文、单元测试、启动早期）时，
 * 直接从 classpath 读取 {@code i18n/messages*.properties} 并完成 {@link MessageFormat} 格式化。
 *
 * <p><b>编码</b>：{@link ResourceBundle#getBundle(String, Locale)} 默认按 ISO-8859-1
 * 读取 properties（Java 8 规范），中文会乱码。因此这里显式以 UTF-8
 * 构造 {@link PropertyResourceBundle}。</p>
 *
 * <p>多语言资源按模块分散放置，因此按「基名 + 语言」维度缓存 {@link ResourceBundle}，避免重复 IO。</p>
 *
 * @author xiongxiao
 */
final class BundleLoader {

    private static final Map<String, ResourceBundle> CACHE = new ConcurrentHashMap<>();

    private BundleLoader() {
    }

    /**
     * 读取资源并格式化。
     *
     * @param basename 资源基名，如 {@code i18n/messages}
     * @param locale   目标语言
     * @param key      资源 key
     * @param args     格式化参数
     * @return 格式化后的文案；资源缺失时返回 {@code null}
     */
    static String get(String basename, Locale locale, String key, Object... args) {
        if (key == null) {
            return null;
        }
        Locale target = locale == null ? I18nMessageResolver.DEFAULT_LOCALE : locale;
        String cacheKey = basename + ':' + target;
        ResourceBundle bundle = CACHE.computeIfAbsent(cacheKey, k -> load(basename, target));
        if (bundle == null || !bundle.containsKey(key)) {
            return null;
        }
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (Exception e) {
            return null;
        }
        if (pattern == null) {
            return null;
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (IllegalArgumentException e) {
            // 占位符与参数不匹配时原样返回，避免因文案问题掩盖真实错误
            return pattern;
        }
    }

    /**
     * 清空缓存（测试或热重载场景使用）。
     */
    static void clear() {
        CACHE.clear();
    }

    /**
     * 按「精确语言 → 根资源」顺序加载，全部以 UTF-8 解码。
     */
    private static ResourceBundle load(String basename, Locale locale) {
        String path = basename.replace('.', '/');
        // 1) 精确语言：messages_zh_CN.properties / messages_en.properties
        ResourceBundle bundle = loadSingle(path, localeSuffix(locale));
        if (bundle != null) {
            return bundle;
        }
        // 2) 仅语言：messages_zh.properties
        if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
            bundle = loadSingle(path, locale.getLanguage());
            if (bundle != null) {
                return bundle;
            }
        }
        // 3) 根资源：messages.properties（中文默认）
        return loadSingle(path, null);
    }

    /**
     * 读取单个 properties 文件（UTF-8）；不存在时返回 null。
     */
    private static ResourceBundle loadSingle(String path, String suffix) {
        String resource = suffix == null || suffix.isEmpty() ? path + ".properties" : path + "_" + suffix + ".properties";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = BundleLoader.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static String localeSuffix(Locale locale) {
        if (locale == null) {
            return null;
        }
        String language = locale.getLanguage();
        String country = locale.getCountry();
        if (language == null || language.isEmpty()) {
            return null;
        }
        if (country == null || country.isEmpty()) {
            return language;
        }
        return language + "_" + country;
    }
}
