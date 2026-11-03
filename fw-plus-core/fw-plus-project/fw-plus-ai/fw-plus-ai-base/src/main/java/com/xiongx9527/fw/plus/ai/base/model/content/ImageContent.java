package com.xiongx9527.fw.plus.ai.base.model.content;

/**
 * 图片片段：URL 或原始字节二选一。
 *
 * <p>{@code detail} 只对支持的厂商生效（OpenAI 的 {@code low/high/auto}），其余厂商忽略。</p>
 */
public final class ImageContent implements ContentPart {

    /** 图片精度提示。 */
    public enum Detail {

        AUTO,

        LOW,

        HIGH
    }

    private final String url;

    private final byte[] data;

    private final String mimeType;

    private final Detail detail;

    private ImageContent(String url, byte[] data, String mimeType, Detail detail) {
        this.url = url;
        this.data = data;
        this.mimeType = mimeType;
        this.detail = detail;
    }

    public static ImageContent ofUrl(String url) {
        return new ImageContent(url, null, null, Detail.AUTO);
    }

    public static ImageContent ofUrl(String url, Detail detail) {
        return new ImageContent(url, null, null, detail == null ? Detail.AUTO : detail);
    }

    public static ImageContent ofBytes(byte[] data, String mimeType) {
        return new ImageContent(null, data, mimeType, Detail.AUTO);
    }

    @Override
    public ContentType type() {
        return ContentType.IMAGE;
    }

    public String getUrl() {
        return url;
    }

    /** 原始字节；{@code null} 表示用 URL。 */
    public byte[] getData() {
        return data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Detail getDetail() {
        return detail;
    }

    public boolean isUrl() {
        return url != null && !url.trim().isEmpty();
    }
}
