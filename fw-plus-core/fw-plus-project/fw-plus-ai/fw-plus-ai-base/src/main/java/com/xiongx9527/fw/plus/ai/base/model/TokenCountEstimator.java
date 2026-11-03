package com.xiongx9527.fw.plus.ai.base.model;

import java.util.List;

/**
 * token 估算端口：为上下文窗口裁剪提供「这段消息大概占多少 token」。
 *
 * <p>框架不内置厂商分词器（各模型词表不同、体积也大），默认用字符数近似；
 * 产品接入真实分词器后注入即可，窗口策略本身不变。</p>
 */
public interface TokenCountEstimator {

    /** 估算单段文本的 token 数。 */
    int estimate(String text);

    /** 估算一组消息的 token 数（默认逐条累加，含角色开销）。 */
    default int estimateInMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ChatMessage message : messages) {
            if (message == null) {
                continue;
            }
            total += 4;
            String content = message.getContent();
            if (content != null) {
                total += estimate(content);
            }
            if (message.hasMedia()) {
                // 图片 / 音频 / 文件的 token 与具体模型相关，这里按固定占位估算
                total += 256;
            }
        }
        return total;
    }
}
