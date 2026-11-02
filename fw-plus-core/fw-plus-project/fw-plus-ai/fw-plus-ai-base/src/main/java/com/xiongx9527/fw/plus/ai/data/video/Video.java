package com.xiongx9527.fw.plus.ai.data.video;

import java.net.URI;

/**
 * 视频：URL 或 base64 承载的视频数据。
 *
 * <p><b>作用</b>：多模态消息（{@code VideoContent}）的视频载体。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.video.Video}。</p>
 *
 * @author xiongxiao
 */
public class Video {

    private final URI url;
    private final String base64Data;
    private final String mimeType;

    private Video(Builder builder) {
        this.url = builder.url;
        this.base64Data = builder.base64Data;
        this.mimeType = builder.mimeType;
    }

    /**
     * 视频地址。
     *
     * @return URI，可能为 {@code null}
     */
    public URI url() { return url; }

    /**
     * base64 编码的视频数据。
     *
     * @return base64 字符串，可能为 {@code null}
     */
    public String base64Data() { return base64Data; }

    /**
     * MIME 类型，如 {@code video/mp4}。
     *
     * @return MIME 类型，可能为 {@code null}
     */
    public String mimeType() { return mimeType; }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() { return new Builder(); }

    @Override
    public String toString() {
        return "Video{url=" + url + ", mimeType='" + mimeType + "'}";
    }

    /** {@link Video} 构建器。 */
    public static final class Builder {
        private URI url;
        private String base64Data;
        private String mimeType;

        public Builder url(URI url) { this.url = url; return this; }
        public Builder base64Data(String base64Data) { this.base64Data = base64Data; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Video build() { return new Video(this); }
    }
}
