package com.xx9527.fw.plus.dify.app.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xx9527.fw.plus.dify.SimpleHttpClient;
import com.xx9527.fw.plus.dify.app.DifyEventSourceBaseListener;
import com.xx9527.fw.plus.dify.app.IAppCompletion;
import com.xx9527.fw.plus.dify.app.params.ParamMessage;
import com.xx9527.fw.plus.dify.app.types.DifyChatEvent;
import com.xx9527.fw.plus.dify.app.types.DifyChatResult;
import com.xx9527.fw.plus.dify.routes.AppRoutes;
import com.xx9527.fw.plus.dify.types.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.HashMap;

@Slf4j
public class AppCompletionImpl extends AppBaseClientImpl implements IAppCompletion {
    /**
     * constructor
     *
     * @param server Dify Server URL
     * @param apiKey The App Api Key
     */
    public AppCompletionImpl(String server, String apiKey) {
        super(server, apiKey);
    }

    /**
     * 发送消息到 Dify 应用并获取响应结果
     *
     * <p>该方法用于向 Dify 应用发送对话消息，通过阻塞式请求获取应用的响应，
     * 并将响应结果封装为 DifyChatResult 对象返回。</p>
     *
     * @param message 参数消息对象，包含发送消息的详细信息和配置参数
     *                该对象会被格式化后转换为 Map 结构发送到 Dify 服务端点
     * @return DifyChatResult 对象，包含事件类型和响应数据
     *         - event: 固定为 DifyChatEvent.message，表示这是一个消息事件
     *         - payload: 解析后的 JSON 响应数据，包含 Dify 应用的处理结果
     */
    @Override
    public DifyChatResult sendMessages(ParamMessage message) {
        String result = requestBlocking(AppRoutes.COMPLETION_MESSAGES, null, message.toMap());
        return DifyChatResult.builder().event(DifyChatEvent.message).payload(JSON.parseObject(result)).build();
    }

    /**
     * 异步发送消息到 Dify 应用并监听流式响应事件
     *
     * <p>该方法用于向 Dify 应用发送对话消息，通过流式请求获取应用的实时响应，
     * 并通过事件监听器异步处理响应事件。适用于需要实时反馈的对话场景。</p>
     *
     * @param message 参数消息对象，包含发送消息的详细信息和配置参数
     *                该对象会被格式化后转换为 Map 结构发送到 Dify 服务端点
     * @param listener 事件源监听器，用于接收和处理流式响应的事件回调
     *                 当 Dify 应用返回响应数据时，会通过此监听器异步通知调用方
     * @return EventSource 事件源对象，用于管理流式连接的生命周期
     *         可通过该对象控制连接的开启、关闭等操作
     */
    @Override
    public EventSource sendMessagesAsync(ParamMessage message, EventSourceListener listener) {
        return requestStreaming(AppRoutes.COMPLETION_MESSAGES, null, message.toMap(), listener);
    }
    
    /**
     * 异步发送消息到 Dify 应用并监听流式响应事件（使用默认监听器）
     * 
     * <p>该方法用于向 Dify 应用发送对话消息，通过流式请求获取应用的实时响应，
     * 并使用默认的 DifyEventSourceListener 处理响应事件。适用于不需要自定义
     * 事件处理逻辑的常规流式对话场景。</p>
     *
     * @param message 参数消息对象，包含发送消息的详细信息和配置参数
     *                该对象会被格式化后转换为 Map 结构发送到 Dify 服务端点
     * @return EventSource 事件源对象，用于管理流式连接的生命周期
     *         可通过该对象控制连接的开启、关闭等操作
     */
    @Override
    public EventSource sendMessagesAsync(ParamMessage message) {
        return sendMessagesAsync(message, new DifyEventSourceBaseListener());
    }

    /**
     * 停止Dify应用的消息生成任务
     *
     * <p>该方法用于向Dify服务端发送停止请求，终止指定任务的message生成过程。
     * 通过POST请求调用Dify API的stop端点，并解析返回结果判断操作是否成功。</p>
     *
     * @param taskId 任务ID，标识需要停止生成的特定任务
     * @param user 用户标识，用于指明执行停止操作的用户
     * @return Boolean值，表示停止操作是否成功
     *         - true: 停止操作成功执行
     *         - false: 停止操作失败或返回结果不是success
     */
    @Override
    public Boolean stopGenerate(String taskId, String user) {
        String result = SimpleHttpClient.get(config).requestJson(
                String.format("%s/%s/stop", AppRoutes.COMPLETION_MESSAGES.getUrl(), taskId),
                HttpMethod.POST,
                null,
                new HashMap<String, Object>() {{
                    put("user", user);
                }}
        );

        JSONObject json = JSON.parseObject(result);
        return "success".equalsIgnoreCase(json.getString("result"));
    }

}
