package com.xiongx9527.fw.plus.dify.app;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.dify.app.params.ParamMessage;
import com.xiongx9527.fw.plus.dify.app.types.DifyWorkFlowResult;
import com.xiongx9527.fw.plus.dify.app.types.WorkflowStatus;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 适用于工作流应用的客户端
 * workflow
 */
public interface IAppFlowClient extends IAppBaseClient {

    /**
     * 执行工作流（流式模式）
     * @param message
     * @param listener
     * @return
     */
    EventSource runStreaming(ParamMessage message, EventSourceListener listener);

    /**
     * 执行工作流（流式模式）
     * @param message
     * @return
     */
    EventSource runStreaming(ParamMessage message);

    /**
     * 执行工作流（阻塞模式）
     *
     * @param message ParamMessage
     */
    JSONObject runBlocking(ParamMessage message);

    /**
     * 获取工作流执行情况
     *
     * @param workFlowId 工作流ID
     */
    JSONObject getWorkFlowStatus(String workFlowId);

    /**
     * 停止工作流（仅限流式模式）
     * <p>对应 POST /workflows/tasks/{task_id}/stop（复数 tasks）；body 中 user 必填，
     * 但服务端不检查 user 与发起者是否一致（文档语义：停止操作对任务生效、与 user 无关）。</p>
     *
     * @param taskId 任务ID（流式返回chunk中获取）
     * @param user   用户标识，必填；无需与发起运行的 user 一致
     */
    Boolean stopWorkFlow(String taskId, String user);

    /**
     * 获取工作流日志
     *
     * @param keyword 关键字
     * @param status  执行状态（running/succeeded/failed/stopped/partial-succeeded/paused）
     * @param page    页码
     * @param limit   每页数量
     */
    JSONObject getWorkFlowLog(String keyword, WorkflowStatus status, Integer page, Integer limit);

    /**
     * 按 ID 执行工作流（阻塞模式）
     * <p>执行指定的已发布工作流版本；草稿版本 ID 会返回 400 bad_request。</p>
     *
     * @param workflowId 已发布工作流版本 ID（来自执行工作流响应或运行详情的 workflow_id）
     * @param message    ParamMessage，包含 inputs/user/files 等执行参数
     * @return JSONObject 运行结果（task_id、workflow_run_id、data）
     */
    JSONObject runByIdBlocking(String workflowId, ParamMessage message);

    /**
     * 按 ID 执行工作流（流式模式）
     *
     * @param workflowId 已发布工作流版本 ID
     * @param message    ParamMessage
     * @param listener   SSE 事件监听器
     * @return EventSource
     */
    EventSource runByIdStreaming(String workflowId, ParamMessage message, EventSourceListener listener);

    /**
     * 按 ID 执行工作流（流式模式，使用默认监听器）
     *
     * @param workflowId 已发布工作流版本 ID
     * @param message    ParamMessage
     * @return EventSource
     */
    EventSource runByIdStreaming(String workflowId, ParamMessage message);

    /**
     * 流式获取工作流事件（连接中断或运行暂停后按 workflow_run_id 恢复）
     * <p>query 中的 user 必须与发起该运行的 user 一致，否则服务端返回 404。</p>
     *
     * @param workflowRunId       运行 ID
     * @param user                终端用户标识（必须与发起者一致）
     * @param includeStateSnapshot 是否先重放已执行节点的状态快照
     * @param continueOnPause      true 时流在多次 workflow_paused 之间保持打开
     * @param listener             SSE 事件监听器
     * @return EventSource
     */
    EventSource streamWorkflowEvents(String workflowRunId, String user, boolean includeStateSnapshot,
                                     boolean continueOnPause, EventSourceListener listener);

    /**
     * 流式获取工作流事件（默认参数：不重放快照、暂停即关流）
     *
     * @param workflowRunId 运行 ID
     * @param user          终端用户标识
     * @param listener      SSE 事件监听器
     * @return EventSource
     */
    EventSource streamWorkflowEvents(String workflowRunId, String user, EventSourceListener listener);
}
