package com.xiongx9527.fw.plus.dify.app.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.dify.SimpleHttpClient;
import com.xiongx9527.fw.plus.dify.app.DifyEventSourceBaseListener;
import com.xiongx9527.fw.plus.dify.app.IAppChatClient;
import com.xiongx9527.fw.plus.dify.app.params.ParamMessage;
import com.xiongx9527.fw.plus.dify.app.types.DifyChatResult;
import com.xiongx9527.fw.plus.dify.routes.AppRoutes;
import com.xiongx9527.fw.plus.dify.types.HttpMethod;
import com.xiongx9527.fw.plus.dify.app.types.DifyChatEvent;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class AppChatClientImpl extends AppBaseClientImpl implements IAppChatClient {
    /**
     * constructor
     *
     * @param server Dify Server URL
     * @param apiKey The App Api Key
     */
    public AppChatClientImpl(String server, String apiKey) {
        super(server, apiKey);
    }

    /**
     * 发送消息到 Dify 聊天应用并获取响应结果
     * 
     * <p>该方法用于向 Dify 聊天应用发送对话消息，通过阻塞式请求获取应用的响应，
     * 并将响应结果封装为 DifyChatResult 对象返回。</p>
     *
     * @param message 参数消息对象，包含发送消息的详细信息和配置参数
     *                该对象会被转换为 Map 结构发送到 Dify 服务端点
     * @return DifyChatResult 对象，包含事件类型和响应数据
     *         - event: 固定为 DifyChatEvent.message，表示这是一个消息事件
     *         - payload: 解析后的 JSON 响应数据，包含 Dify 应用的处理结果
     */
    @Override
    public DifyChatResult sendMessages(ParamMessage message) {
        String result = requestBlocking(AppRoutes.CHAT_MESSAGES, null, message.toMap());
        return DifyChatResult.builder().event(DifyChatEvent.message).payload(JSON.parseObject(result)).build();
    }

    /**
     * 异步发送消息到 Dify 聊天应用并监听流式响应事件
     * 
     * <p>该方法用于向 Dify 聊天应用发送对话消息，通过流式请求获取应用的实时响应，
     * 并通过事件监听器异步处理响应事件。适用于需要实时反馈的对话场景。</p>
     *
     * @param message 参数消息对象，包含发送消息的详细信息和配置参数
     *                该对象会被转换为 Map 结构发送到 Dify 服务端点
     * @param listener 事件源监听器，用于接收和处理流式响应的事件回调
     *                 当 Dify 应用返回响应数据时，会通过此监听器异步通知调用方
     * @return EventSource 事件源对象，用于管理流式连接的生命周期
     *         可通过该对象控制连接的开启、关闭等操作
     */
    @Override
    public EventSource sendMessagesAsync(ParamMessage message, EventSourceListener listener) {
        return requestStreaming(AppRoutes.CHAT_MESSAGES, null, message.toMap(), listener);
    }
    
    /**
     * 异步发送消息到 Dify 聊天应用并监听流式响应事件（使用默认监听器）
     * 
     * <p>该方法用于向 Dify 聊天应用发送对话消息，通过流式请求获取应用的实时响应，
     * 并使用默认的 DifyEventSourceListener 处理响应事件。适用于不需要自定义
     * 事件处理逻辑的常规流式对话场景。</p>
     *
     * @param message 参数消息对象，包含发送消息的详细信息和配置参数
     *                该对象会被转换为 Map 结构发送到 Dify 服务端点
     * @return EventSource 事件源对象，用于管理流式连接的生命周期
     *         可通过该对象控制连接的开启、关闭等操作
     */
    @Override
    public EventSource sendMessagesAsync(ParamMessage message) {
        return sendMessagesAsync(message, new DifyEventSourceBaseListener());
    }

    /**
     * 停止 Dify 聊天应用正在生成的响应
     * 
     * <p>该方法通过向 Dify 服务端发送停止请求，中断指定任务 ID 的响应生成过程。
     * 适用于用户主动取消生成长回复或需要紧急停止计算资源的场景。</p>
     *
     * @param taskId 任务唯一标识符，用于定位需要停止的生成任务
     *               该 ID 通常在启动生成任务时由服务端返回
     * @param user 用户标识符，用于记录操作者身份和权限验证
     *               可以是用户 ID、用户名或其他唯一标识
     * @return Boolean 表示停止操作是否成功
     *         - true: 服务端成功处理停止请求，响应生成已被中断
     *         - false: 停止操作失败，可能原因包括任务不存在、已完成或网络异常
     */
    @Override
    public Boolean stopResponse(String taskId, String user) {
        String result = SimpleHttpClient.get(config).requestJson(
                String.format("%s/%s/stop", AppRoutes.CHAT_MESSAGES.getUrl(), taskId),
                HttpMethod.POST,
                null,
                new HashMap<String,String>() {{
                    put("user", user);
                }}
        );

        JSONObject json = JSON.parseObject(result);
        return "success".equals(json.getString("result"));
    }

    /**
     * 获取针对指定消息的建议回复列表
     * 
     * <p>该方法用于从 Dify 服务端获取针对特定消息的智能建议回复选项，
     * 适用于为用户提供快捷回复建议或引导对话方向的场景。</p>
     *
     * @param messageId 消息唯一标识符，用于获取针对该消息的建议回复
     *                  该 ID 通常是历史对话中的某条消息 ID
     * @param user 用户标识符，用于记录操作者身份和个性化推荐
     *               可以是用户 ID、用户名或其他唯一标识
     * @return List<String> 建议回复的文本列表
     *         包含多条智能生成的建议回复内容，按相关性排序
     *         如果获取失败或服务端返回错误则返回空列表
     */
    @Override
    public List<String> suggestedList(String messageId, String user) {
        String result = SimpleHttpClient.get(config).requestJson(
                String.format("%s/%s/suggested", AppRoutes.MESSAGES.getUrl(), messageId),
                HttpMethod.GET,
                new HashMap<String, Object>() {{
                    put("user", user);
                }},
                null

        );

        JSONObject json = JSON.parseObject(result);

        if ("success".equals(json.getString("result"))) {
            return json.getJSONArray("data").toList(String.class);
        }

        return new ArrayList<>();
    }

    /**
     * 查询指定会话的历史消息记录
     * 
     * <p>该方法用于获取 Dify 聊天应用中某个会话的对话历史记录，支持分页查询和游标定位。
     * 适用于加载聊天记录、回溯对话内容或审计对话历史的场景。</p>
     *
     * @param conversationId 会话唯一标识符，用于定位需要查询的对话
     *                       该 ID 通常在创建会话时由服务端返回
     * @param user 用户标识符，用于记录操作者身份和权限验证
     *               可以是用户 ID、用户名或其他唯一标识
     * @param limit 每页记录数，限制单次查询返回的最大消息条数
     *              用于控制响应数据量，避免一次性加载过多数据
     * @param firstId 起始消息 ID，用作分页查询的游标
     *                指定从哪条消息开始返回历史记录，为空则从最新消息开始
     * @return JSONObject 历史消息列表响应对象
     *         包含消息列表总数、当前页码、每页数量以及具体的消息记录数组
     *         每条消息通常包含 ID、内容、发送者、时间戳等信息
     *         如果请求失败或无数据则返回 null
     */
    @Override
    public JSONObject history(String conversationId, String user, Integer limit, String firstId) {
        String result = SimpleHttpClient.get(config).requestJson(AppRoutes.MESSAGES, new HashMap<String, Object>() {{
            put("conversation_id", conversationId);
            put("user", user);
            put("limit", limit);
            put("first_id", firstId);
        }}, null);

        if (result == null) {
            return null;
        }

        return JSON.parseObject(result);
    }

    /**
     * 查询用户的会话列表
     * 
     * <p>该方法用于获取 Dify 聊天应用中某个用户的所有会话列表，支持排序和分页查询。
     * 适用于展示用户的对话列表、管理会话或进行会话分析的场景。</p>
     *
     * @param user 用户标识符，用于查询该用户下的所有会话
     *               可以是用户 ID、用户名或其他唯一标识
     * @param limit 每页记录数，限制单次查询返回的最大会话条数
     *              用于控制响应数据量，避免一次性加载过多数据
     * @param sortBy 排序字段，指定会话列表的排序方式
     *               可选值包括 updated_at（最后更新时间）、created_at（创建时间）等
     * @param lastId 起始会话 ID，用作分页查询的游标
     *               指定从哪个会话开始返回列表，为空则从最新会话开始
     * @return JSONObject 会话列表响应对象
     *         包含会话列表总数、当前页码、每页数量以及具体的会话记录数组
     *         每个会话通常包含 ID、名称、最后消息、更新时间等信息
     *         如果请求失败或无数据则返回 null
     */
    @Override
    public JSONObject conversations(String user, Integer limit, String sortBy, String lastId) {
        String result = SimpleHttpClient.get(config).requestJson(AppRoutes.CHAT_CONVERSATIONS, new HashMap<String, Object>() {{
            put("user", user);
            put("last_id", lastId);
            put("sort_by", sortBy);
            put("limit", limit);
        }}, null);

        if (result == null) {
            return null;
        }

        return JSON.parseObject(result);
    }

    /**
     * 删除指定的会话
     * 
     * <p>该方法通过向 Dify 服务端发送删除请求，永久删除指定的会话及其所有历史消息。
     * 适用于用户清理无用对话、释放存储空间或遵守数据保留政策的场景。</p>
     *
     * @param conversationId 会话唯一标识符，用于定位需要删除的对话
     *                       该 ID 通常在创建会话时由服务端返回
     * @param user 用户标识符，用于记录操作者身份和权限验证
     *               可以是用户 ID、用户名或其他唯一标识
     * @return Boolean 表示删除操作是否成功
     *         - true: 服务端成功处理删除请求，会话已被永久删除
     *         - false: 删除操作失败，可能原因包括会话不存在、无权限或网络异常
     */
    @Override
    public Boolean deleteConversation(String conversationId, String user) {
        String result = SimpleHttpClient.get(config).requestJson(
                AppRoutes.CHAT_CONVERSATIONS.getUrl() + "/" + conversationId,
                HttpMethod.DELETE,
                null,
                new HashMap<String, Object>() {{
                    put("user", user);
                }}
        );

        JSONObject json = JSON.parseObject(result);
        return "success".equals(json.getString("result"));
    }

    /**
     * 重命名会话（使用默认设置）
     * 
     * <p>该方法用于修改指定会话的名称，自动启用智能命名功能。
     * 适用于用户自定义会话标签、整理对话分类或优化会话管理的场景。</p>
     *
     * @param conversationId 会话唯一标识符，用于定位需要重命名的对话
     *                       该 ID 通常在创建会话时由服务端返回
     * @param user 用户标识符，用于记录操作者身份和权限验证
     *               可以是用户 ID、用户名或其他唯一标识
     * @param name 新的会话名称，用于替换原有名称
     *             长度和内容需符合服务端的验证规则
     * @return JSONObject 重命名操作的响应结果
     *         包含操作状态、更新后的会话信息等
     */
    @Override
    public JSONObject renameConversation(String conversationId, String user, String name) {
        return renameConversation(conversationId, user, name, false);
    }

    /**
     * 重命名会话（可配置智能命名）
     * 
     * <p>该方法用于修改指定会话的名称，支持手动命名和 AI 智能命名两种模式。
     * 当启用智能命名时，Dify 会根据对话内容自动生成合适的会话标题。</p>
     *
     * @param conversationId 会话唯一标识符，用于定位需要重命名的对话
     *                       该 ID 通常在创建会话时由服务端返回
     * @param user 用户标识符，用于记录操作者身份和权限验证
     *               可以是用户 ID、用户名或其他唯一标识
     * @param name 新的会话名称，用于替换原有名称
     *             长度和内容需符合服务端的验证规则
     * @param autoGenerate 是否启用 AI 智能命名
     *                     - true: 忽略 name 参数，由 AI 根据对话内容自动生成标题
     *                     - false: 使用提供的 name 参数作为会话名称
     * @return JSONObject 重命名操作的响应结果
     *         包含操作状态、更新后的会话信息、智能生成的名称（如启用）等
     */
    @Override
    public JSONObject renameConversation(String conversationId, String user, String name, boolean autoGenerate) {
        String result = SimpleHttpClient.get(config).requestJson(
                String.format("%s/%s/name", AppRoutes.CHAT_CONVERSATIONS.getUrl(), conversationId),
                HttpMethod.POST,
                null,
                new HashMap<String, Object>() {{
                    put("user", user);
                    put("name", name);
                    put("auto_generate", autoGenerate);
                }}
        );

        return JSON.parseObject(result);
    }

}
