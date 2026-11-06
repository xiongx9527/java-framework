package com.xiongx9527.fw.plus.ai.base.memory;

/**
 * 对话记忆后端异常：网络不可达、鉴权失败、后端 4xx/5xx、报文解析失败统一收敛为此类。
 *
 * <p>设计为 {@link RuntimeException}，便于在 adapter 调用链中透传，由上层统一 try/catch
 * 或降级，而不是在每个端口方法签名上扩散受检异常。</p>
 */
public class ConversationMemoryException extends RuntimeException {

    public ConversationMemoryException(String message) {
        super(message);
    }

    public ConversationMemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
