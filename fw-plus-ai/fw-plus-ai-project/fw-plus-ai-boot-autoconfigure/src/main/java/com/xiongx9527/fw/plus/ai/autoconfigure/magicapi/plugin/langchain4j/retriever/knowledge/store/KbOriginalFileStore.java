package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store;

import java.io.InputStream;

/**
 * 知识库原始文档存储：上传原文先落盘（重建索引回读/下载/审计依赖），路径写回 kb_document.file_path。
 *
 * <p>默认实现 {@link LocalKbOriginalFileStore}（本地目录）；后续可扩展对象存储适配。</p>
 */
public interface KbOriginalFileStore {

    /**
     * 保存原始文档字节流。
     *
     * @param documentId 文档 id（作为存储文件名防冲突）
     * @param fileName   原始文件名（保留扩展名）
     * @param content    原始字节流（方法内自行关闭）
     * @return 存储路径标识（写回 kb_document.file_path）
     */
    String save(String documentId, String fileName, InputStream content);

    /** 保存文本内容（create-by-text 场景）：便于统一回读与下载。 */
    String saveText(String documentId, String fileName, String text);

    /** 读取原始文档字节流（调用方负责关闭）。 */
    InputStream read(String path);

    /** 删除原始文档（删除文档时同步调用；不存在时静默返回）。 */
    void delete(String path);
}
