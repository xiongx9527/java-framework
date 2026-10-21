package com.xiongx9527.fw.plus.common.i18n;

import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可合并多模块资源的 {@link org.springframework.context.MessageSource}。
 *
 * <h3>为什么不用 {@code ReloadableResourceBundleMessageSource}</h3>
 * <p>它按「文件名」缓存已加载的 properties，classpath 上多个 jar 内同名文件
 * （各模块各自的 {@code i18n/messages.properties}）只会命中其中一个，无法合并；
 * 且其默认以 ISO-8859-1 读取，中文会乱码。</p>
 *
 * <p>本实现直接扫描 classpath 上所有 {@code i18n/} 下的 {@code messages*.properties}，
 * 按「文件名（基名+语言变体）」聚合、以 UTF-8 解码，并把同一 key 的多个来源
 * **按扫描顺序合并（后者覆盖前者）**。</p>
 *
 * <h3>命名约定</h3>
 * <pre>
 *   i18n/messages.properties                  # 默认（中文）文案
 *   i18n/messages_en.properties               # 英文
 *   i18n/messages-core.properties             # core 层专属（同一命名空间内可直接合并）
 *   i18n/messages-boot.properties             # boot 层专属
 * </pre>
 *
 * <p>因为按「文件名」而非「基名」聚合，各模块放同名文件即可自动合并，
 * 无需为每个模块起不同名字。</p>
 *
 * @author xiongxiao
 */
public class ComposableMessageSource extends AbstractMessageSource {

    /** 扫描目录（classpath 通配，指向 i18n 目录） */
    private final String[] scanLocations;

    /** 资源缓存：key = 语言变体（如 {@code ""}、{@code "_en"}、{@code "_en_US"}），value = 合并后的属性 */
    private final Map<String, Properties> cache = new LinkedHashMap<>();

    private volatile boolean loaded = false;

    private String encoding = StandardCharsets.UTF_8.name();

    /** 缺省语言，资源缺失时回退使用 */
    private Locale defaultLocale = Locale.SIMPLIFIED_CHINESE;

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * @param scanLocations 扫描目录，如 {@code classpath*:i18n/}
     */
    public ComposableMessageSource(String... scanLocations) {
        this.scanLocations = (scanLocations == null || scanLocations.length == 0)
                ? new String[]{"classpath*:i18n/"}
                : scanLocations;
        setUseCodeAsDefaultMessage(false);
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * 扫描并加载所有资源。重复调用为幂等操作。
     */
    public synchronized void initialize() {
        if (loaded) {
            return;
        }
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Charset charset = resolveCharset();
        for (String location : scanLocations) {
            String dir = location.endsWith("/") ? location : location + "/";
            try {
                Resource[] resources = resolver.getResources(dir + "messages*.properties");
                for (Resource resource : resources) {
                    mergeResource(resource, charset);
                }
            } catch (IOException ignored) {
                // 目录不存在或不可读：跳过
            }
        }
        loaded = true;
    }

    /**
     * 读取单个资源文件并合并进对应语言变体的属性集合。
     */
    private void mergeResource(Resource resource, Charset charset) {
        String variant = variantOf(resource.getFilename());
        if (variant == null) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = resource.getInputStream()) {
            if (in == null) {
                return;
            }
            try (Reader reader = new InputStreamReader(in, charset)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            return;
        }
        synchronized (cache) {
            Properties target = cache.computeIfAbsent(variant, k -> new Properties());
            // 后者覆盖前者：扫描顺序决定优先级
            for (String name : properties.stringPropertyNames()) {
                target.setProperty(name, properties.getProperty(name));
            }
        }
    }

    /**
     * 由文件名推导语言变体。
     *
     * <pre>
     *   messages.properties       -> ""
     *   messages_en.properties    -> "_en"
     *   messages_zh_CN.properties -> "_zh_CN"
     *   messages-boot_en.properties -> "_en"
     * </pre>
     */
    private String variantOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.endsWith(".properties")) {
            return null;
        }
        String stem = filename.substring(0, filename.length() - ".properties".length());
        Matcher matcher = VARIANT.matcher(stem);
        if (matcher.matches()) {
            return matcher.group(1) == null ? "" : matcher.group(1);
        }
        return "";
    }

    /** 匹配 {@code <任意基名>(_语言(_国家)?)?} */
    private static final Pattern VARIANT = Pattern.compile("^.*?((?:_[a-zA-Z]{2,3}(?:_[a-zA-Z]{2}|_[0-9]{3})?))?$");

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        String pattern = getPattern(code, locale);
        if (pattern == null) {
            return null;
        }
        try {
            return new MessageFormat(pattern, locale);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 按语言回退链查找文案：精确（zh_CN）→ 仅语言（zh）→ 默认（无后缀）。
     */
    private String getPattern(String code, Locale locale) {
        initialize();
        for (String variant : variantsFor(locale)) {
            Properties properties = cache.get(variant);
            if (properties != null) {
                String value = properties.getProperty(code);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * 生成语言回退链。
     */
    private Set<String> variantsFor(Locale locale) {
        Set<String> variants = new LinkedHashSet<>();
        if (locale != null) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (StringUtils.hasText(language)) {
                if (StringUtils.hasText(country)) {
                    variants.add("_" + language + "_" + country);
                }
                variants.add("_" + language);
            }
        }
        variants.add("");
        return variants;
    }

    private Charset resolveCharset() {
        try {
            return Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * 清空缓存并重新加载（热更新 / 测试使用）。
     */
    public synchronized void refresh() {
        cache.clear();
        loaded = false;
        initialize();
    }

    /**
     * 便捷工厂：创建并完成初始化。
     */
    public static ComposableMessageSource create(String encoding, Locale defaultLocale, int cacheSeconds,
                                                 String... scanLocations) {
        ComposableMessageSource source = new ComposableMessageSource(scanLocations);
        source.setEncoding(encoding);
        source.setDefaultLocale(defaultLocale);
        source.initialize();
        return source;
    }
}
