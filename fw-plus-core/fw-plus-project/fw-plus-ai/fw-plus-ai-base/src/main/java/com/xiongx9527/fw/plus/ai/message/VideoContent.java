package com.xiongx9527.fw.plus.ai.message;

import com.xiongx9527.fw.plus.ai.data.video.Video;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 视频消息内容（多模态输入）。
 *
 * <p><b>作用</b>：让消息携带视频。仅支持视频输入的模型才会消费该内容，其余模型应报错或忽略。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.message.VideoContent}。</p>
 *
 * @author xiongxiao
 */
public class VideoContent implements Content {

    private final Video video;

    /**
     * 由视频地址创建。
     *
     * @param url 视频地址
     */
    public VideoContent(URI url) {
        this(Video.builder().url(url).build());
    }

    /**
     * 由视频地址字符串创建。
     *
     * @param url 视频地址
     */
    public VideoContent(String url) {
        this(URI.create(url));
    }

    /**
     * 由 base64 数据与 MIME 类型创建。
     *
     * @param base64Data base64 编码的视频
     * @param mimeType   MIME 类型
     */
    public VideoContent(String base64Data, String mimeType) {
        this(Video.builder().base64Data(base64Data).mimeType(mimeType).build());
    }

    /**
     * 由 {@link Video} 创建。
     *
     * @param video 视频
     */
    public VideoContent(Video video) {
        if (video == null) {
            throw new IllegalArgumentException("video must not be null");
        }
        this.video = video;
    }

    @Override
    public ContentType type() {
        return ContentType.VIDEO;
    }

    /**
     * 视频。
     *
     * @return 视频对象，永不为 {@code null}
     */
    public Video video() {
        return video;
    }

    @Override
    public String toString() {
        return "VideoContent{video=" + video + '}';
    }

    /**
     * 由视频地址创建。
     *
     * @param url 视频地址
     * @return 视频内容
     */
    public static VideoContent from(URI url) {
        return new VideoContent(url);
    }

    /**
     * 由视频地址字符串创建。
     *
     * @param url 视频地址
     * @return 视频内容
     */
    public static VideoContent from(String url) {
        return new VideoContent(url);
    }

    /**
     * 由 base64 数据与 MIME 类型创建。
     *
     * @param base64Data base64 编码的视频
     * @param mimeType   MIME 类型
     * @return 视频内容
     */
    public static VideoContent from(String base64Data, String mimeType) {
        return new VideoContent(base64Data, mimeType);
    }

    /**
     * 由本地文件创建（读取为 base64）。
     *
     * @param path     文件路径
     * @param mimeType MIME 类型
     * @return 视频内容
     */
    public static VideoContent from(Path path, String mimeType) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new VideoContent(Base64.getEncoder().encodeToString(bytes), mimeType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read video from " + path, e);
        }
    }

    /**
     * 由 {@link Video} 创建。
     *
     * @param video 视频
     * @return 视频内容
     */
    public static VideoContent from(Video video) {
        return new VideoContent(video);
    }
}
