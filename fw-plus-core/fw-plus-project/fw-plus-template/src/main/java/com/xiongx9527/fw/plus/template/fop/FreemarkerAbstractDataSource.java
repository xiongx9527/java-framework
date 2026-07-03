package com.xiongx9527.fw.plus.template.fop;

import cn.hutool.http.HttpUtil;
import freemarker.template.TemplateException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
/**
 * FreemarkerAbstractDataSource 是一个抽象类，实现了 DataSource 接口，
 * 提供了基于 FreeMarker 模板引擎的数据源处理功能。
 * 子类需要实现 processTemplate 方法以完成具体的模板处理逻辑。
 */
@Slf4j
public abstract class FreemarkerAbstractDataSource implements org.dromara.pdf.fop.core.datasource.DataSource {

    /**
     * HTTP协议前缀常量定义
     */
    private static final String HTTP_PROTOCOL = "http://";

    /**
     * HTTPS协议前缀常量定义
     */
    private static final String HTTPS_PROTOCOL = "https://";


    /**
     * 模板文件的路径
     */
    protected String templatePath;

    /**
     * 模板数据：用于填充模板的键值对数据集合
     */
    protected Map<String, Object> templateData;

    /**
     * 获取数据源读取器，将输入流转换为 UTF-8 编码的字符读取器
     *
     * @return 返回 Reader 对象，可用于读取模板内容
     */
    @SneakyThrows
    @Override
    public Reader getSourceReader() {
        return new InputStreamReader(this.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * 执行文档转换操作，使用 FOP 工厂、用户代理和输出流进行 SAX 转换
     *
     * @param fopFactory   FOP 工厂实例，用于创建 FOP 处理对象
     * @param foAgent      FO 用户代理，配置渲染行为
     * @param outputStream 输出流，用于写入生成的 PDF 内容
     */
    @SneakyThrows
    @Override
    public void transform(FopFactory fopFactory, FOUserAgent foAgent, OutputStream outputStream) {
        this.saxTransform(fopFactory, foAgent, outputStream);
    }

    /**
     * 抽象方法：由子类实现具体模板处理逻辑，并返回处理后的输入流
     *
     * @return 返回处理后模板的 InputStream 输入流
     */
    protected abstract InputStream processTemplate() throws IOException, TemplateException;

    /**
     * 获取模板的输入流。如果存在模板数据则调用 processTemplate 进行处理，
     * 否则直接加载原始模板文件。
     *
     * @return 返回 InputStream 输入流，表示模板内容
     */
    @SneakyThrows
    private InputStream getInputStream() {
        // 根据是否有模板数据决定是处理模板还是直接加载
        return this.isNotEmptyTemplateData() ? this.processTemplate() : this.loadTemplateInputStream();
    }

    /**
     * 判断模板数据是否非空（即不为 null 且不为空 Map）
     *
     * @return 如果模板数据非空返回 true，否则返回 false
     */
    protected boolean isNotEmptyTemplateData() {
        return this.templateData != null && !this.templateData.isEmpty();
    }

    /**
     * 加载模板文件的输入流。首先尝试通过类路径加载资源，
     * 若失败再尝试通过绝对路径加载。
     *
     * @return 返回模板文件对应的 InputStream 输入流
     * @throws IllegalArgumentException 当无法找到或加载指定路径的模板时抛出异常
     */
    @SneakyThrows
    protected InputStream loadTemplateInputStream() {
        try {
            // 尝试从当前线程上下文类加载器中获取资源
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.templatePath);
            // 如果成功获取到输入流，则返回；否则尝试通过 Paths 方式加载
            return inputStream != null ? inputStream : Files.newInputStream(Paths.get(this.templatePath));
        } catch (Exception e) {
            // 如果两种方式都失败，则抛出非法参数异常提示路径错误
            throw new IllegalArgumentException("the template can not be loaded，the path['" + this.templatePath + "'] is error");
        }
    }


    /**
     * 执行DOM转换操作，将模板文件转换为PDF文档
     *
     * @param fopFactory FOP工厂实例，用于创建FOP对象
     * @param foAgent FO用户代理，提供转换过程中的用户环境信息
     * @param templatePath 模板文件路径，指定要使用的XSL-FO模板位置
     * @param outputStream 输出流，转换后的PDF将写入此流中
     * @return 转换后PDF文档的总页数
     * @throws RuntimeException 当转换过程中发生异常时抛出
     */
    @Override
    public final Integer domTransform(FopFactory fopFactory, FOUserAgent foAgent, String templatePath, OutputStream outputStream) {
        InputStream inputStream = null;
        try {
            inputStream = loadTemplate(templatePath);

            // 创建输入流读取器
            try (InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                // 创建XSLT转换器实例
                Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(streamReader));

                // 获取数据源读取器
                try (Reader reader = this.getSourceReader()) {
                    // 创建FOP实例，指定输出格式为PDF
                    Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foAgent, outputStream);

                    // 执行XML到PDF的转换操作
                    transformer.transform(new StreamSource(reader), new SAXResult(fop.getDefaultHandler()));

                    // 返回页数
                    return fop.getResults().getPageCount();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to transform document with template: {}", templatePath, e);
            throw new RuntimeException(e);
        } finally {
            // 安全关闭 inputStream（仅针对需要关闭的实际资源）
            if (inputStream != null && !(inputStream instanceof ByteArrayInputStream)) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    log.debug("Failed to close input stream", ignored);
                }
            }
        }
    }

    /**
     * 加载模板文件并返回输入流
     *
     * @param templatePath 模板文件路径，可以是HTTP/HTTPS URL、classpath资源路径或本地文件路径
     * @return 模板文件的输入流
     * @throws IllegalArgumentException 当模板路径为空或无法加载模板时抛出
     */
    @SneakyThrows
    private InputStream loadTemplate(String templatePath) {
        if (templatePath == null || templatePath.isEmpty()) {
            throw new IllegalArgumentException("Template path must not be null or empty.");
        }

        // 处理HTTP URL类型的模板路径
        if (templatePath.startsWith(HTTP_PROTOCOL) || templatePath.startsWith(HTTPS_PROTOCOL)) {
            try {
                byte[] bytes = HttpUtil.downloadBytes(templatePath);
                return new ByteArrayInputStream(bytes);
            } catch (Exception e) {
                log.warn("Failed to download template from URL: {}", templatePath, e);
                throw new IllegalArgumentException("The template cannot be loaded; the path ['" + templatePath + "'] is invalid.", e);
            }
        } else {
            // 首先尝试从classpath加载资源
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream resourceStream = classLoader.getResourceAsStream(templatePath);

            if (resourceStream != null) {
                return resourceStream;
            }

            // 如果classpath中找不到，则尝试作为本地文件读取
            try {
                return Files.newInputStream(Paths.get(templatePath));
            } catch (IOException ex) {
                log.warn("Failed to read local file template: {}", templatePath, ex);
                throw new IllegalArgumentException("The template cannot be loaded; the path ['" + templatePath + "'] is invalid.", ex);
            }
        }
    }

}

