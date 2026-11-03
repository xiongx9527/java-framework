package com.xiongx9527.fw.plus.ai.base.model.content;

/**
 * 音频片段：URL 或原始字节二选一。
 */
public final class AudioContent implements ContentPart {

    private final String url;

    private final byte[] data;

    private final String mimeType;

    private AudioContent(String url, byte[] data, String mimeType) {
        this.url = url;
        this.data = data;
        this.mimeType = mimeType;
    }

    public static AudioContent ofUrl(String url) {
        return new AudioContent(url, null, null);
    }

    public static AudioContent ofBytes(byte[] data, String mimeType) {
        return new AudioContent(null, data, mimeType);
    }

    @Override
    public ContentType type() {
        return ContentType.AUDIO;
    }

    public String getUrl() {
        return url;
    }

    public byte[] getData() {
        return data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isUrl() {
        return url != null && !url.trim().isEmpty();
    }
}
