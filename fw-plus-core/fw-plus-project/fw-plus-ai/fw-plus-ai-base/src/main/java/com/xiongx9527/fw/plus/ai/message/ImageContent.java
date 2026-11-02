package com.xiongx9527.fw.plus.ai.message;

import com.xiongx9527.fw.plus.ai.data.image.Image;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 图片消息内容（多模态输入）。
 *
 * <p><b>作用</b>：让用户消息携带图片。图片既可用 URL 引用，也可用 base64 内联；
 * {@link DetailLevel} 控制视觉模型的处理精度（低精度省 token，高精度保细节）。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.message.ImageContent}：
 * 内部持有 {@link Image} 对象（而非裸 url 字符串），并提供 {@code image()/detailLevel()/detailLevelSpecified()}
 * 与 {@code from(...)} 系列工厂。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #image()} 永不为 {@code null}；
 * 未显式指定精度时 {@link #detailLevelSpecified()} 返回 {@code false}，由模型侧使用默认值。</p>
 *
 * @author xiongxiao
 */
public class ImageContent implements Content {

    /** 视觉处理精度。 */
    public enum DetailLevel {
        /** 由模型自行决定。 */
        AUTO,
        /** 低精度（更快、更省 token）。 */
        LOW,
        /** 高精度（保留细节）。 */
        HIGH
    }

    private final Image image;
    private final DetailLevel detailLevel;
    private final boolean detailLevelSpecified;

    /**
     * 由图片地址创建。
     *
     * @param url 图片地址
     */
    public ImageContent(URI url) {
        this(Image.builder().url(url).build(), null);
    }

    /**
     * 由图片地址字符串创建。
     *
     * @param url 图片地址
     */
    public ImageContent(String url) {
        this(URI.create(url));
    }

    /**
     * 由 base64 数据与 MIME 类型创建。
     *
     * @param base64Data base64 编码的图片
     * @param mimeType   MIME 类型
     */
    public ImageContent(String base64Data, String mimeType) {
        this(Image.builder().base64Data(base64Data).mimeType(mimeType).build(), null);
    }

    /**
     * 由 {@link Image} 创建。
     *
     * @param image 图片
     */
    public ImageContent(Image image) {
        this(image, null);
    }

    /**
     * 由 {@link Image} 与精度创建。
     *
     * @param image       图片
     * @param detailLevel 精度，为 {@code null} 表示不指定
     */
    public ImageContent(Image image, DetailLevel detailLevel) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        this.image = image;
        this.detailLevel = detailLevel;
        this.detailLevelSpecified = detailLevel != null;
    }

    /**
     * 图片。
     *
     * @return 图片对象，永不为 {@code null}
     */
    public Image image() {
        return image;
    }

    /**
     * 精度。
     *
     * @return 精度；未指定时为 {@code null}
     */
    public DetailLevel detailLevel() {
        return detailLevel;
    }

    /**
     * 是否显式指定了精度。
     *
     * @return 显式指定返回 {@code true}
     */
    public boolean detailLevelSpecified() {
        return detailLevelSpecified;
    }

    @Override
    public ContentType type() {
        return ContentType.IMAGE;
    }

    @Override
    public String toString() {
        return "ImageContent{image=" + image + ", detailLevel=" + detailLevel + '}';
    }

    /**
     * 由图片地址创建。
     *
     * @param url 图片地址
     * @return 图片内容
     */
    public static ImageContent from(URI url) {
        return new ImageContent(url);
    }

    /**
     * 由图片地址字符串创建。
     *
     * @param url 图片地址
     * @return 图片内容
     */
    public static ImageContent from(String url) {
        return new ImageContent(url);
    }

    /**
     * 由 base64 数据与 MIME 类型创建。
     *
     * @param base64Data base64 编码的图片
     * @param mimeType   MIME 类型
     * @return 图片内容
     */
    public static ImageContent from(String base64Data, String mimeType) {
        return new ImageContent(base64Data, mimeType);
    }

    /**
     * 由本地文件创建（读取为 base64）。
     *
     * @param path     文件路径
     * @param mimeType MIME 类型
     * @return 图片内容
     */
    public static ImageContent from(Path path, String mimeType) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new ImageContent(Base64.getEncoder().encodeToString(bytes), mimeType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read image from " + path, e);
        }
    }

    /**
     * 由 {@link Image} 创建。
     *
     * @param image 图片
     * @return 图片内容
     */
    public static ImageContent from(Image image) {
        return new ImageContent(image);
    }

    /**
     * 由 {@link Image} 与精度创建。
     *
     * @param image       图片
     * @param detailLevel 精度
     * @return 图片内容
     */
    public static ImageContent from(Image image, DetailLevel detailLevel) {
        return new ImageContent(image, detailLevel);
    }
}
