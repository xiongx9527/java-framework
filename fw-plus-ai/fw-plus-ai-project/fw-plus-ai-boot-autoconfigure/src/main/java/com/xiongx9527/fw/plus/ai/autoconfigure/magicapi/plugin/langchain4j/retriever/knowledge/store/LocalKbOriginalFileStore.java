package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store;

import org.springframework.util.StringUtils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地目录存储实现：目录由 {@code fw.plus.ai.knowledge.storage-path} 配置，默认 {@code ${user.home}/.fw-plus/kb}。
 *
 * <p>文件名为 documentId + 原扩展名，防冲突；目录按数据集 id 一级分组。</p>
 */
public class LocalKbOriginalFileStore implements KbOriginalFileStore {

    private final Path root;

    public LocalKbOriginalFileStore(String storagePath) {
        this.root = Paths.get(StringUtils.hasText(storagePath)
                ? storagePath
                : System.getProperty("user.home") + "/.fw-plus/kb");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("初始化知识库原始文件目录失败: " + root, e);
        }
    }

    @Override
    public String save(String documentId, String fileName, InputStream content) {
        String ext = extensionOf(fileName);
        Path target = resolve(documentId, ext);
        try (InputStream in = content;
             OutputStream out = new BufferedOutputStream(new FileOutputStream(target.toFile()))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new IllegalStateException("保存知识库原始文件失败: " + fileName, e);
        }
        return relativeKey(target);
    }

    @Override
    public String saveText(String documentId, String fileName, String text) {
        return save(documentId, fileName, new java.io.ByteArrayInputStream(
                text.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public InputStream read(String path) {
        try {
            return Files.newInputStream(safeResolve(path));
        } catch (IOException e) {
            throw new IllegalStateException("读取知识库原始文件失败: " + path, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(safeResolve(path));
        } catch (IOException e) {
            throw new IllegalStateException("删除知识库原始文件失败: " + path, e);
        }
    }

    private Path resolve(String documentId, String ext) {
        String fileName = documentId + (StringUtils.hasText(ext) ? "." + ext : "");
        return root.resolve(fileName);
    }

    private Path safeResolve(String path) {
        Path resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root.normalize())) {
            throw new IllegalArgumentException("非法的存储路径: " + path);
        }
        return resolved;
    }

    private String relativeKey(Path target) {
        return root.relativize(target.normalize()).toString().replace('\\', '/');
    }

    private String extensionOf(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : null;
    }
}
