package com.xiongx9527.fw.plus.ai.data.audio;

import java.net.URI;

/**
 * 音频：URL、二进制或 base64 承载的音频数据。
 *
 * <p><b>作用</b>：多模态消息（{@code AudioContent}）与语音识别 / 合成模型共用的音频载体。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.audio.Audio}
 * （{@code url()/binaryData()/base64Data()/mimeType()/builder()}）。</p>
 *
 * @author xiongxiao
 */
public class Audio {

    private final URI url;
    private final byte[] binaryData;
    private final String base64Data;
    private final String mimeType;

    private Audio(Builder builder) {
        this.url = builder.url;
        this.binaryData = builder.binaryData;
        this.base64Data = builder.base64Data;
        this.mimeType = builder.mimeType;
    }

    /**
     * 音频地址。
     *
     * @return URI，可能为 {@code null}
     */
    public URI url() { return url; }

    /**
     * 原始字节。
     *
     * @return 字节数组，可能为 {@code null}
     */
    public byte[] binaryData() { return binaryData; }

    /**
     * base64 编码的音频数据。
     *
     * @return base64 字符串，可能为 {@code null}
     */
    public String base64Data() { return base64Data; }

    /**
     * MIME 类型，如 {@code audio/wav}。
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
        return "Audio{url=" + url + ", mimeType='" + mimeType + "'}";
    }

    /** {@link Audio} 构建器。 */
    public static final class Builder {
        private URI url;
        private byte[] binaryData;
        private String base64Data;
        private String mimeType;

        public Builder url(URI url) { this.url = url; return this; }
        public Builder binaryData(byte[] binaryData) { this.binaryData = binaryData; return this; }
        public Builder base64Data(String base64Data) { this.base64Data = base64Data; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Audio build() { return new Audio(this); }
    }
}
