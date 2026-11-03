package com.xiongx9527.fw.plus.ai.base.io;

/**
 * 产物端口：存文件、取文件、签发临时访问链接。
 */
public interface ArtifactPort {

    /** 保存内容，返回产物 id。 */
    String store(String name, byte[] content, String contentType);

    /** 读取内容；不存在返回 {@code null}。 */
    byte[] load(String artifactId);

    /** 签发带时效的访问链接。 */
    String signedUrl(String artifactId, long ttlSeconds);
}
