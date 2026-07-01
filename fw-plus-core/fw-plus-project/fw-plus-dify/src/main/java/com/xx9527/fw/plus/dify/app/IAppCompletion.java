package com.xx9527.fw.plus.dify.app;

import com.xx9527.fw.plus.dify.app.params.ParamMessage;
import com.xx9527.fw.plus.dify.app.types.DifyChatResult;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 适用于文本生成应用的客户端
 * ChatBot、Agent、ChatFlow
 */
public interface IAppCompletion extends IAppBaseClient {

    /**
     * 发送消息
     *
     * @param message ParamMessage
     */
    DifyChatResult sendMessages(ParamMessage message);

    /**
     * @param message
     * @param listener
     * @return
     */
    EventSource sendMessagesAsync(ParamMessage message, EventSourceListener listener);

    /**
     * 发送消息（仅支持流式模式）
     * @param message
     * @return
     */
    EventSource sendMessagesAsync(ParamMessage message);

    /**
     * 停止响应（仅支持流式模式）
     *
     * @param taskId 任务 ID，可在流式返回 Chunk 中获取
     * @param user   用户标识，用于定义终端用户的身份，必须和发送消息接口传入 user 保持一致。
     */
    Boolean stopGenerate(String taskId, String user);
}
