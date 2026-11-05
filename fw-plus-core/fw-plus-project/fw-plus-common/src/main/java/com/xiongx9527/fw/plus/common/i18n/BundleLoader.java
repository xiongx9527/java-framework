package com.xiongx9527.fw.plus.common.i18n;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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

    private static final Map<String, List<ResourceBundle>> CACHE = new ConcurrentHashMap<>();

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
        List<ResourceBundle> bundles = CACHE.computeIfAbsent(cacheKey, k -> loadAll(basename, target));
        for (ResourceBundle bundle : bundles) {
            if (bundle == null || !bundle.containsKey(key)) {
                continue;
            }
            String pattern;
            try {
                pattern = bundle.getString(key);
            } catch (Exception e) {
                continue;
            }
            if (pattern == null) {
                continue;
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
        return null;
    }

    /**
     * 清空缓存（测试或热重载场景使用）。
     */
    static void clear() {
        CACHE.clear();
    }

    /**
     * 按「精确语言 → 仅语言 → 根资源」顺序收集 classpath 上所有匹配的
     * {@code i18n/messages*.properties}，聚合为 bundle 列表。这样跨模块同一基名的资源
     * （如 core / engine / 业务模块各自一份）在脱离 Spring 的静态解析下也能相互补全，
     * 行为对齐 {@code MessageSource} 的聚合语义，避免单一 bundle 遮蔽其它模块的资源 key。
     */
    private static List<ResourceBundle> loadAll(String basename, Locale locale) {
        List<ResourceBundle> result = new ArrayList<>();
        String path = basename.replace('.', '/');
        collect(result, path, localeSuffix(locale));
        if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
            collect(result, path, locale.getLanguage());
        }
        collect(result, path, null);
        return result;
    }

    /**
     * 读取 classpath 上所有匹配的 properties 文件（UTF-8），逐个加入结果列表；不存在时跳过。
     */
    private static void collect(List<ResourceBundle> out, String path, String suffix) {
        String resource = suffix == null || suffix.isEmpty() ? path + ".properties" : path + "_" + suffix + ".properties";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = BundleLoader.class.getClassLoader();
        }
        try {
            Enumeration<URL> urls = loader.getResources(resource);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
                    out.add(new PropertyResourceBundle(reader));
                } catch (IOException ignored) {
                    // 单个资源读取失败不影响其它资源
                }
            }
        } catch (IOException ignored) {
            // 无匹配资源
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
