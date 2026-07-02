package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FreemarkerMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FtlInfoMagicTemplateLoader;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@MagicModule("freemarker")
public class FreemarkerModule {

    /**
     * 包含指令的正则表达式，用于匹配FreeMarker模板中的include标签
     * 该正则表达式匹配形如<#include "templateName">或<#include 'templateName'>的标签
     * 并捕获其中的模板名称部分
     */
    private static final String INCLUDE_REG = "<#include[^<>]*?\\s['\"]?@([^'\"]*)['\"]?\\s?.*?>";

    /**
     * 包含指令的编译正则表达式模式对象
     * 用于高效地匹配和提取模板中的include指令
     */
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(INCLUDE_REG);

    /**
     * 模板缓存对象，用于存储已解析的模板
     * 缓存配置：
     * - 最大缓存大小为1000个模板
     * - 写入后10分钟过期
     */
    private static final Cache<String, Template> templateCache =
            CacheBuilder.newBuilder()
                    .maximumSize(1000) // 限制缓存大小
                    .expireAfterWrite(10, TimeUnit.MINUTES) // 设置过期时间
                    .build();


    /**
     * 配置对象，用于存储和管理应用程序的配置信息
     * 该字段被声明为final，确保配置对象的引用不可变
     * 使用private修饰符限制访问范围，保证封装性
     */
    private final Configuration config;


    /**
     * 构造函数，初始化FreeMarker模块配置
     *
     * <p>该构造函数创建并配置FreeMarker模板引擎，包括：
     * <ul>
     *   <li>设置FreeMarker版本为2.3.31</li>
     *   <li>配置模板加载器</li>
     *   <li>设置编码格式和异常处理策略</li>
     * </ul>
     *
     * @since 1.0
     */
    public FreemarkerModule() {
        // 创建FreeMarker配置实例，指定版本为2.3.31
        freemarker.template.Configuration config = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_31);

        // 转换模板加载器映射为数组并设置复合模板加载器
        TemplateLoader[] templateLoaders = {new FtlInfoMagicTemplateLoader()};
        config.setTemplateLoader(new MultiTemplateLoader(templateLoaders));

        // 设置默认编码格式和异常处理策略
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setLogTemplateExceptions(false);
        config.setWrapUncheckedExceptions(true);
        this.config = config;
    }


    /**
     * 清除指定路径的缓存
     *
     * @param path 需要清除缓存的路径
     */
    public void clearCache(String path) {
        templateCache.invalidate(path);
    }

    /**
     * 清除所有缓存数据
     *
     * 该方法会调用templateCache的invalidateAll方法，将缓存中的所有数据清除。
     * 执行此操作后，所有缓存的模板数据都将被移除，下次访问时需要重新加载。
     */
    public void clearCache() {
        templateCache.invalidateAll();
    }

    /**
     * 根据路径创建模板对象
     *
     * @param path 模板文件的路径
     * @return 返回指定路径对应的模板对象
     * @throws MagicAPIException 当缓存获取模板出现异常时抛出
     */
    public Template createTemplateByPath(String path) {
        try {
            // 从缓存中获取模板，如果缓存中不存在则通过createTemplate方法创建并放入缓存
            return templateCache.get(path, () -> {
                return createTemplate(path);
            });
        } catch (Exception e) {
            log.error("缓存获取模板异常", e);
            throw new MagicAPIException("缓存获取模板异常", e);
        }
    }

    /**
     * 根据给定的路径创建一个 Freemarker 模板对象。
     * <p>
     * 该方法首先从注册中心获取与路径对应的模板信息（FtlInfo），如果存在，则进一步处理其中的 <#include> 指令，
     * 将引用的其他模板内容进行替换展开。最终使用处理后的脚本内容构建并返回一个 Template 对象。
     * </p>
     *
     * @param path 模板资源的路径，用于查找对应的 FtlInfo 信息
     * @return 处理后生成的 Template 对象；若未找到对应模板信息则返回 null
     * @throws IOException 如果在读取或处理模板过程中发生 IO 异常
     */
    private Template createTemplate(String path)  {
        // 获取 Freemarker 动态注册表实例
        FreemarkerMagicDynamicRegistry freemarkerMagicDynamicRegistry =
                SpringUtils.getBean(FreemarkerMagicDynamicRegistry.class);

        // 获取路径对应的模板信息
        FtlInfo ftlInfo = freemarkerMagicDynamicRegistry.getMapping(path);

        // 如果从动态注册表中未获取到模板信息，则尝试从资源配置服务中获取
        if (Objects.isNull(ftlInfo)) {
            ftlInfo = MagicConfiguration.getMagicResourceService().file(path);
        }

        // 如果仍未获取到模板信息，则返回null
        if (Objects.isNull(ftlInfo)) {
            return null;
        }

        // 获取原始模板脚本内容，并匹配所有 include 指令
        String script = ftlInfo.getScript();
        Matcher m = INCLUDE_PATTERN.matcher(script);

        // 存储处理后的模板内容
        StringBuffer resultBuffer = new StringBuffer();

        // 遍历所有匹配到的 include 指令，尝试替换其引用的内容
        while (m.find()) {
            try {
                String r = m.group(1); // 获取 @xxx 中的 xxx
                // 安全转义 r，防止其影响正则结构
                String escapedR = Pattern.quote(r);

                // 查找被引用的模板信息
                FtlInfo mapping = freemarkerMagicDynamicRegistry.getMapping(r);
                if (mapping != null) {
                    String rep = mapping.getScript();
                    if (rep != null) {
                        // 构造准确匹配当前 include 标签的正则
                        String fullTagRegex = "<#include[^<>]*?\\s['\"]?@" + escapedR + "['\"]?\\s?.*?>";
                        m.appendReplacement(resultBuffer, rep.replaceAll("\\$", "\\\\\\$"));
                    } else {
                        m.appendReplacement(resultBuffer, m.group(0)); // 不替换保留原样
                    }
                } else {
                    log.warn("找不到对应的模板映射: {}", r);
                    m.appendReplacement(resultBuffer, m.group(0));
                }
            } catch (IllegalStateException ise) {
                log.error("正则状态异常，可能由于并发修改引起", ise);
            } catch (Exception e) {
                log.warn("处理模板引用 {} 时发生未知错误: {}", m.group(0), e.getMessage(), e);
                m.appendReplacement(resultBuffer, m.group(0)); // 出错也保持原文本
            }
        }
        // 将剩余未匹配的部分追加至结果缓冲区
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
     * @param path 模板文件路径，用于定位和加载对应的模板文件
     * @param params 模板渲染所需的参数对象，可为null，为null时将使用空映射
     * @return 包含渲染后HTML内容的ResponseEntity对象，设置正确的Content-Type头部
     * @throws IOException 当模板文件读取或处理过程中发生IO错误时抛出
     * @throws TemplateException 当模板解析或渲染过程中发生错误时抛出
     */
    public ResponseEntity<String> view(String path, Object params) throws IOException, TemplateException {
        // 安全检查：防止路径遍历攻击
        if (!isValidPath(path)) {
            throw new MagicAPIException("非法的模板路径：" + path);
        }
        // 参数检查
        if (Objects.isNull(params)) {
            params = Collections.emptyMap(); // 使用不可变空映射代替 new HashMap<>()
        }
        // 获取模板
        Template template = createTemplateByPath(path);
        if (Objects.isNull(template)) {
            throw new MagicAPIException("未找到模板：" + path);
        }

        // 使用 try-with-resources 确保资源正确关闭
        try (StringWriter out = new StringWriter()) {
            // 处理模板生成 HTML
            template.process(params, out);
            // 返回构建好的响应实体
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html;charset=utf-8")
                    .body(out.toString());
        } catch (TemplateException | IOException e) {
            // 明确捕获特定异常以便后续扩展不同处理逻辑
            log.error("模板渲染失败，路径：{}", path, e); // 避免直接输出 params 敏感信息
            throw e;
        } catch (Exception e) {
            log.error("未知异常导致模板渲染失败，路径：{}", path, e);
            throw new RuntimeException(e); // 将未预期异常包装成运行时异常抛出
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
