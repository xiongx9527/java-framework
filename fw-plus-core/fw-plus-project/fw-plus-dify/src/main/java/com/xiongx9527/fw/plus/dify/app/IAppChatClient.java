package com.xiongx9527.fw.plus.dify.app;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.dify.app.params.ParamMessage;
import com.xiongx9527.fw.plus.dify.app.types.DifyChatResult;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 适用于对话应用的客户端
 * ChatBot、Agent、ChatFlow
 */
public interface IAppChatClient extends IAppBaseClient {
    /**
     * 发送对话消息（同步接收）
     *
     * @param message ParamMessage
     */
    DifyChatResult sendMessages(ParamMessage message);

    /**
     * 发送对话消息（异步接收）
     * @param message ParamMessage
     * @param listener
     * @return
     */
    EventSource sendMessagesAsync(ParamMessage message, EventSourceListener listener);

    /**
     * 发送对话消息（异步接收）
     *
     * @param message
     * @return
     */
    EventSource sendMessagesAsync(ParamMessage message);

    /**
     * 停止响应(仅支持流式模式)
     * <p>对应 POST /chat-messages/{task_id}/stop；user 与发起者不一致时服务端静默成功
     * （不报错、也不停止实际生成），SDK 原样透传该语义。</p>
     *
     * @param taskId 任务 ID，可在流式返回 Chunk 中获取
     * @param user   用户标识，用于定义终端用户的身份，必须和发送消息接口传入 user 保持一致。
     */
    Boolean stopResponse(String taskId, String user);

    /**
     * 获取下一轮建议问题列表
     *
     * @param messageId 消息ID
     * @param user      用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     */
    List<String> suggestedList(String messageId, String user);

    /**
     * 获取会话历史消息
     *
     * @param conversationId 会话ID
     * @param user           用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     * @param limit          一次请求返回多少条聊天记录，默认 20 条。
     * @param firstId        当前页第一条聊天记录的 ID，默认为空，表示获取第一页数据
     */
    JSONObject history(String conversationId, String user, Integer limit, String firstId);

    /**
     * 获取会话列表
     *
     * @param user   用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     * @param lastId （选填）当前页最后面一条记录的 ID，默认 null
     * @param sortBy （选填）排序字段，默认 -updated_at(按更新时间倒序排列)
     *               可选值：created_at, -created_at, updated_at, -updated_at
     *               字段前面的符号代表顺序或倒序，-代表倒序
     * @param limit  （选填）一次请求返回多少条记录，默认 20 条，最大 100 条，最小 1 条。
     */
    JSONObject conversations(String user, Integer limit, String sortBy, String lastId);

    /**
     * 删除会话
     *
     * @param conversationId 会话 ID
     * @param user           用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     */
    Boolean deleteConversation(String conversationId, String user);

    /**
     * 会话重命名
     *
     * @param conversationId 会话ID
     * @param user           用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     * @param name           会话名称
     */
    JSONObject renameConversation(String conversationId, String user, String name);

    /**
     * 会话重命名
     *
     * @param conversationId 会话ID
     * @param user           用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     * @param name           会话名称
     * @param autoGenerate   是否自动生成标题
     */
    JSONObject renameConversation(String conversationId, String user, String name, boolean autoGenerate);

    /**
     * 获取会话变量
     * <p>对应 GET /conversations/{conversation_id}/variables，
     * 400 not_chat_app/invalid_param、404 not_found 原样抛出。</p>
     *
     * @param conversationId 会话 ID
     * @param user           用户标识（可选）
     * @param limit          返回条数（可选，默认 20，1–100）
     * @param lastId         分页游标：当前页最后一条变量的 id（可选）
     * @param variableName   按名称筛选变量（可选，1–255 字符）
     * @return JSONObject 会话变量分页对象（data[]/has_more/limit）
     */
    JSONObject listConversationVariables(String conversationId, String user, Integer limit, String lastId, String variableName);

    /**
     * 更新会话变量
     * <p>对应 PUT /conversations/{conversation_id}/variables/{variable_id}；
     * 新值必须匹配变量现有类型，类型不匹配返回 400 bad_request、变量或会话不存在返回 404 not_found。</p>
     *
     * @param conversationId 会话 ID
     * @param variableId     变量 ID（来自 {@link #listConversationVariables}）
     * @param value          变量新值（必填，须匹配预期类型）
     * @param user           用户标识（可选）
     * @return JSONObject 更新后的变量对象
     */
    JSONObject updateConversationVariable(String conversationId, String variableId, Object value, String user);
}
