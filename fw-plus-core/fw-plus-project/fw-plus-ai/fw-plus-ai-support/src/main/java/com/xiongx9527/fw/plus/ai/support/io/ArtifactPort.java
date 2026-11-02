package com.xiongxiao.fw.plus.ai.support.io;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * 制品端口：文件、导出结果等二进制对象的存取与临时访问地址。
 *
 * <p><b>作用</b>：导出 Excel、保存上传的 PDF、生成图表图片都产出「制品」。主链路只应拿到一个引用，
 * 真正的字节流与存储介质（本地盘 / 对象存储 / 数据库）由本端口屏蔽。对外暴露的必须是<b>带签名的临时地址</b>，
 * 而不是永久公开 URL——否则等于把数据裸奔。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.18.10 ArtifactPort
 * 与增强清单「图表 / 图片 URL 鉴权（签名 URL）」「大数据量导出」。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>{@link #url(String, Duration)} 返回的地址必须过期失效，且不可被枚举猜测。</li>
 *   <li>{@link #open(String)} 返回的流由调用方负责关闭。</li>
 *   <li>实现需保证同一 {@code artifactId} 的读写一致（写完再读）。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface ArtifactPort {

    /**
     * 保存制品。
     *
     * @param name        原始文件名（用于展示与推断类型）
     * @param contentType MIME 类型
     * @param content     内容流，由调用方关闭
     * @return 制品标识
     */
    String store(String name, String contentType, InputStream content);

    /**
     * 打开制品内容流。
     *
     * @param artifactId 制品标识
     * @return 内容流，调用方负责关闭
     */
    InputStream open(String artifactId);

    /**
     * 生成临时访问地址。
     *
     * @param artifactId 制品标识
     * @param ttl        有效期
     * @return 带签名的临时地址
     */
    URI url(String artifactId, Duration ttl);

    /**
     * 删除制品。
     *
     * @param artifactId 制品标识
     */
    void delete(String artifactId);
}
