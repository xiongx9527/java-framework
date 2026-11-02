package com.xiongx9527.fw.plus.ai.data.document;

/**
 * {@link Document} 的默认实现。
 *
 * <p><b>作用</b>：装载 / 解析链路的通用返回值，持有文本与元数据。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.document.DefaultDocument}。</p>
 *
 * @author xiongxiao
 */
public class DefaultDocument implements Document {

    private final String text;
    private final Metadata metadata;

    /**
     * 由文本创建（空元数据）。
     *
     * @param text 文本
     */
    public DefaultDocument(String text) {
        this(text, new Metadata());
    }

    /**
     * 由文本与元数据创建。
     *
     * @param text     文本
     * @param metadata 元数据，为 {@code null} 时使用空元数据
     */
    public DefaultDocument(String text, Metadata metadata) {
        this.text = text;
        this.metadata = metadata == null ? new Metadata() : metadata;
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public Metadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultDocument)) {
            return false;
        }
        DefaultDocument that = (DefaultDocument) o;
        if (text == null ? that.text != null : !text.equals(that.text)) {
            return false;
        }
        return metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        int result = text == null ? 0 : text.hashCode();
        return 31 * result + metadata.hashCode();
    }

    @Override
    public String toString() {
        return "DefaultDocument{text='" + text + "', metadata=" + metadata + '}';
    }
}
