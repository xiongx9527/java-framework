package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FreemarkerMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FtlInfoMagicTemplateLoader;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.exception.MagicAPIException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@MagicModule("freemarker")
public class FreemarkerModule {

    private static final String INCLUDE_REG = "<#include[^<>]*?\\s['\"]?@([^'\"]*)['\"]?\\s?.*?>";
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(INCLUDE_REG);

    /** 反向依赖索引：被 include 的子模板路径 → 引用它的父模板路径集合，用于级联缓存失效 */
    private static final Map<String, Set<String>> includeReverseIndex = new ConcurrentHashMap<>();

    private static final Cache<String, Template> templateCache =
            CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build();

    private final Configuration config;

    public FreemarkerModule() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_31);

        TemplateLoader[] templateLoaders = {new FtlInfoMagicTemplateLoader()};
        config.setTemplateLoader(new MultiTemplateLoader(templateLoaders));

        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setLogTemplateExceptions(false);
        config.setWrapUncheckedExceptions(true);
        config.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
        this.config = config;
    }

    /**
     * 清除指定模板的缓存，并级联清除所有引用此模板的父模板缓存
     */
    public void clearCache(String path) {
        templateCache.invalidate(path);
        Set<String> parents = includeReverseIndex.remove(path);
        if (parents != null) {
            for (String parentPath : parents) {
                templateCache.invalidate(parentPath);
                log.debug("级联清除父模板缓存: {} (因子模板 {} 变更)", parentPath, path);
            }
        }
    }

    /**
     * 清除所有缓存数据
     */
    public void clearCache() {
        templateCache.invalidateAll();
        includeReverseIndex.clear();
    }

    /**
     * 根据路径创建模板对象
     *
     * @param path 模板文件的路径
     * @return 模板对象
     * @throws MagicAPIException 当缓存获取模板出现异常时抛出
     */
    public Template createTemplateByPath(String path) {
        try {
            return templateCache.get(path, () -> {
                Template t = createTemplate(path);
                if (t == null) {
                    throw new MagicAPIException(I18nMessageResolver.resolveKey("fw.boot.freemarker.templateNotFound", path));
                }
                return t;
            });
        } catch (Exception e) {
            // 解包 Guava Cache 的 ExecutionException，获取原始异常
            Throwable cause = e.getCause();
            if (cause instanceof MagicAPIException) {
                throw (MagicAPIException) cause;
            }
            log.error("获取模板异常, path={}", path, e);
            throw new MagicAPIException(I18nMessageResolver.resolveKey("fw.boot.freemarker.templateFetchFailed", path), e);
        }
    }

    /**
     * 根据路径创建模板，同时解析模板中的 <#include @xxx> 指令并记录反向依赖关系
     */
    private Template createTemplate(String path) {
        FreemarkerMagicDynamicRegistry registry =
                SpringUtils.getBean(FreemarkerMagicDynamicRegistry.class);

        FtlInfo ftlInfo = registry.getMapping(path);

        if (ftlInfo == null) {
            ftlInfo = MagicConfiguration.getMagicResourceService().file(path);
        }

        if (ftlInfo == null) {
            return null;
        }

        String script = ftlInfo.getScript();

        // 清除此模板的旧反向依赖（重新创建模板时重建）
        includeReverseIndex.values().forEach(set -> set.remove(path));

        Matcher m = INCLUDE_PATTERN.matcher(script);
        StringBuffer resultBuffer = new StringBuffer();

        while (m.find()) {
            try {
                String includePath = m.group(1);
                // 记录 include 关系
                includeReverseIndex.computeIfAbsent(includePath, k -> ConcurrentHashMap.newKeySet()).add(path);

                FtlInfo mapping = registry.getMapping(includePath);
                if (mapping != null && mapping.getScript() != null) {
                    m.appendReplacement(resultBuffer, Matcher.quoteReplacement(mapping.getScript()));
                } else {
                    log.warn("找不到对应的模板映射: {}", includePath);
                    m.appendReplacement(resultBuffer, Matcher.quoteReplacement(m.group(0)));
                }
            } catch (Exception e) {
                log.warn("处理模板引用 {} 时发生错误: {}", m.group(0), e.getMessage(), e);
                m.appendReplacement(resultBuffer, Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(resultBuffer);
        try {
            return new Template(path, resultBuffer.toString(), config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 渲染指定路径的模板并返回HTML响应
     *
     * @param path   模板文件路径
     * @param params 模板渲染参数，可为 null
     * @return 包含渲染后HTML内容的 ResponseEntity
     * @throws IOException       当模板读取发生IO错误时抛出
     * @throws TemplateException 当模板解析或渲染发生错误时抛出
     */
    public ResponseEntity<String> view(String path, Object params) throws IOException, TemplateException {
        if (!isValidPath(path)) {
            throw new MagicAPIException(I18nMessageResolver.resolveKey("fw.boot.freemarker.templatePathIllegal", path));
        }
        if (Objects.isNull(params)) {
            params = Collections.emptyMap();
        }
        Template template = createTemplateByPath(path);

        try (StringWriter out = new StringWriter()) {
            template.process(params, out);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html;charset=utf-8")
                    .body(out.toString());
        } catch (TemplateException | IOException e) {
            log.error("模板渲染失败，路径：{}", path, e);
            throw e;
        } catch (Exception e) {
            log.error("未知异常导致模板渲染失败，路径：{}", path, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 校验路径是否合法（防路径穿越）
     */
    private boolean isValidPath(String path) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        return true;
    }
}