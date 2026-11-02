package com.xiongx9527.fw.plus.ai.data.image;

import java.net.URI;

/**
 * 图片：URL 或 base64 承载的图片数据。
 *
 * <p><b>作用</b>：多模态消息（{@code ImageContent}）与图片生成模型（{@code ImageModel}）共用的图片载体；
 * 只描述「图从哪来」，不含像素处理。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.image.Image}
 * （{@code url() -> URI}、{@code base64Data()}、{@code mimeType()}、{@code revisedPrompt()}、{@code builder()}）。</p>
 *
 * <p><b>实现约定</b>：不可变；{@code url} 与 {@code base64Data} 至少有一个非空。</p>
 *
 * @author xiongxiao
 */
public final class Image {

    private final URI url;
    private final String base64Data;
    private final String mimeType;
    private final String revisedPrompt;

    private Image(Builder builder) {
        this.url = builder.url;
        this.base64Data = builder.base64Data;
        this.mimeType = builder.mimeType;
        this.revisedPrompt = builder.revisedPrompt;
    }

    /**
     * 图片地址。
     *
     * @return URI；以 base64 承载时返回 {@code null}
     */
    public URI url() {
        return url;
    }

    /**
     * base64 编码的图片数据。
     *
     * @return base64 字符串；以 URL 承载时返回 {@code null}
     */
    public String base64Data() {
        return base64Data;
    }

    /**
     * MIME 类型，如 {@code image/png}。
     *
     * @return MIME 类型，可能为 {@code null}
     */
    public String mimeType() {
        return mimeType;
    }

    /**
     * 图片生成模型改写后的提示词（生成场景回填）。
     *
     * @return 改写后的提示词，可能为 {@code null}
     */
    public String revisedPrompt() {
        return revisedPrompt;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Image)) {
            return false;
        }
        Image that = (Image) o;
        return eq(url, that.url) && eq(base64Data, that.base64Data)
                && eq(mimeType, that.mimeType) && eq(revisedPrompt, that.revisedPrompt);
    }

    @Override
    public int hashCode() {
        int result = url == null ? 0 : url.hashCode();
        result = 31 * result + (base64Data == null ? 0 : base64Data.hashCode());
        result = 31 * result + (mimeType == null ? 0 : mimeType.hashCode());
        result = 31 * result + (revisedPrompt == null ? 0 : revisedPrompt.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Image{url=" + url + ", mimeType='" + mimeType + "'}";
    }

    private static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /** {@link Image} 构建器。 */
    public static final class Builder {
        private URI url;
        private String base64Data;
        private String mimeType;
        private String revisedPrompt;

        public Builder url(URI url) { this.url = url; return this; }
        public Builder base64Data(String base64Data) { this.base64Data = base64Data; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder revisedPrompt(String revisedPrompt) { this.revisedPrompt = revisedPrompt; return this; }
        public Image build() { return new Image(this); }
    }
}
