package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import java.util.List;

/**
 * 检索增强挂钩：把检索到的文档注入对话，使 LLM 基于上下文回答。
 *
 * <p>base 只定义中性契约（输入消息 + 查询 → 增强后的消息），具体编排（融合 / 重排 / 截断）
 * 由 support 的检索管线提供；AiServices 等 base 能力通过本接口挂检索而不依赖 support。</p>
 */
public interface RetrievalAugmentor {

    /**
     * 增强消息列表。
     *
     * @param messages 已有的消息（含 system / 历史 / user）
     * @param query    检索查询（文本 + 集合 + topK 等）
     * @return 增强后的新列表（实现不得修改入参）
     */
    List<ChatMessage> augment(List<ChatMessage> messages, RetrievalQuery query);
}
