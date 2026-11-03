package com.xiongx9527.fw.plus.ai.base.model.content;

/**
 * 文件片段（PDF / 文档等）：URL 或原始字节二选一。
 */
public final class FileContent implements ContentPart {

    private final String url;

    private final byte[] data;

    private final String mimeType;

    private final String fileName;

    private FileContent(String url, byte[] data, String mimeType, String fileName) {
        this.url = url;
        this.data = data;
        this.mimeType = mimeType;
        this.fileName = fileName;
    }

    public static FileContent ofUrl(String url, String fileName) {
        return new FileContent(url, null, null, fileName);
    }

    public static FileContent ofBytes(byte[] data, String mimeType, String fileName) {
        return new FileContent(null, data, mimeType, fileName);
    }

    @Override
    public ContentType type() {
        return ContentType.FILE;
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

    public String getFileName() {
        return fileName;
    }

    public boolean isUrl() {
        return url != null && !url.trim().isEmpty();
    }
}
