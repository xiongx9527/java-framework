package com.xx9527.fw.plus.dify.app.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xx9527.fw.plus.dify.SimpleHttpClient;
import com.xx9527.fw.plus.dify.app.DifyEventSourceBaseListener;
import com.xx9527.fw.plus.dify.app.IAppFlowClient;
import com.xx9527.fw.plus.dify.app.params.ParamMessage;
import com.xx9527.fw.plus.dify.app.types.WorkflowStatus;
import com.xx9527.fw.plus.dify.routes.AppRoutes;
import com.xx9527.fw.plus.dify.types.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.HashMap;

@Slf4j
public class AppFlowClientImpl extends AppBaseClientImpl implements IAppFlowClient {
    /**
     * constructor
     *
     * @param server Dify Server URL
     * @param apiKey The App Api Key
     */
    public AppFlowClientImpl(String server, String apiKey) {
        super(server, apiKey);
    }

    /**
     * 以流式模式异步执行 Dify 工作流并监听响应事件
     * 
     * <p>该方法用于启动 Dify 工作流的流式执行，通过事件源监听器实时接收工作流执行过程中的各个节点输出和状态更新。
     * 适用于需要实时获取工作流执行进度和中间结果的场景。</p>
     *
     * @param message 参数消息对象，包含工作流执行所需的输入参数和配置信息
     *                该对象会被转换为 Map 结构发送到 Dify 服务端点
     * @param listener 事件源监听器，用于接收和处理工作流执行过程中的流式响应事件
     *                 包括节点开始、节点结束、文本片段等不同类型的回调
     * @return EventSource 事件源对象，用于管理流式连接的生命周期
     *         可通过该对象控制连接的开启、关闭等操作
     */
    @Override
    public EventSource runStreaming(ParamMessage message, EventSourceListener listener) {
        return requestStreaming(AppRoutes.WORKFLOW_RUN, null, message.toMap(), listener);
    }

    /**
     * 以流式模式异步执行 Dify 工作流并监听响应事件（使用默认监听器）
     * 
     * <p>该方法用于启动 Dify 工作流的流式执行，使用默认的 DifyEventSourceListener 
     * 接收工作流执行过程中的各个节点输出和状态更新。适用于不需要自定义事件处理逻辑
     * 的常规流式工作流执行场景。</p>
     *
     * @param message 参数消息对象，包含工作流执行所需的输入参数和配置信息
     *                该对象会被转换为 Map 结构发送到 Dify 服务端点
     * @return EventSource 事件源对象，用于管理流式连接的生命周期
     *         可通过该对象控制连接的开启、关闭等操作
     */
    @Override
    public EventSource runStreaming(ParamMessage message) {
        return runStreaming(message, new DifyEventSourceBaseListener());
    }

    /**
     * 同步阻塞执行 Dify 工作流并返回完整结果
     * 
     * <p>该方法用于启动 Dify 工作流的执行，通过阻塞式请求等待工作流完全执行完毕后返回最终结果。
     * 适用于不需要实时反馈、只需获取工作流最终输出结果的场景。</p>
     *
     * @param message 参数消息对象，包含工作流执行所需的输入参数和配置信息
     *                该对象会被转换为 Map 结构发送到 Dify 服务端点
     * @return JSONObject 工作流执行的完整响应结果
     *         包含工作流的输出数据、执行状态、耗时等信息，具体结构由工作流定义决定
     */
    @Override
    public JSONObject runBlocking(ParamMessage message) {
        String result = requestBlocking(AppRoutes.WORKFLOW_RUN, null, message.toMap());
        return JSON.parseObject(result);
    }

    /**
     * 获取 Dify 工作流的运行状态信息
     * 
     * <p>该方法通过向 Dify 服务端发送查询请求，获取指定工作流 ID 的当前执行状态和详细信息。
     * 适用于需要监控工作流执行进度、查询执行结果或诊断执行异常的场景。</p>
     *
     * @param workFlowId 工作流唯一标识符，用于定位需要查询的工作流实例
     *                   该 ID 通常在启动工作流时由服务端返回
     * @return JSONObject 工作流状态信息对象
     *         包含工作流的执行状态（running/succeeded/failed 等）、开始时间、结束时间、
     *         输出数据、错误信息等详细执行信息
     */
    @Override
    public JSONObject getWorkFlowStatus(String workFlowId) {
        String result = SimpleHttpClient.get(config).requestJson(
                AppRoutes.WORKFLOW_RUN.getUrl() + "/" + workFlowId,
                HttpMethod.GET,
                null,
                null
        );

        return JSON.parseObject(result);
    }

    /**
     * 停止 Dify 工作流的执行
     * 
     * <p>该方法通过向 Dify 服务端发送停止请求，中断指定任务 ID 的工作流执行过程。
     * 适用于用户主动取消长时间运行的工作流或需要紧急释放计算资源的场景。</p>
     *
     * @param taskId 任务唯一标识符，用于定位需要停止的工作流实例
     *               该 ID 通常在启动工作流时由服务端返回
     * @param user 用户标识符，用于记录操作者身份和权限验证
     *               可以是用户 ID、用户名或其他唯一标识
     * @return Boolean 表示停止操作是否成功
     *         - true: 服务端成功处理停止请求，工作流已被中断
     *         - false: 停止操作失败，可能原因包括任务不存在、已完成或网络异常
     */
    @Override
    public Boolean stopWorkFlow(String taskId, String user) {
        String result = SimpleHttpClient.get(config).requestJson(
                String.format("%s/%s/stop", AppRoutes.WORKFLOW_TASK.getUrl(), taskId),
                HttpMethod.POST,
                null,
                new HashMap<String, Object>() {{
                    put("user", user);
                }}
        );

        JSONObject json = JSON.parseObject(result);
        return "success".equals(json.getString("result"));
    }

    /**
     * 查询 Dify 工作流的执行日志列表
     * 
     * <p>该方法用于获取 Dify 工作流的历史执行记录，支持按关键词、状态筛选和分页查询。
     * 适用于审计工作流执行情况、分析执行效率或排查执行问题的场景。</p>
     *
     * @param keyword 搜索关键词，用于模糊匹配工作流名称、描述或其他相关信息
     *                可为 null，表示不进行关键词过滤
     * @param status 工作流状态过滤条件，用于筛选特定状态的执行记录
     *               可为 null，表示不过滤状态，返回所有状态的记录
     * @param page 页码，从 1 开始计数，用于分页查询
     *              指定要返回的日志页码，配合 limit 参数实现分页功能
     * @param limit 每页记录数，限制单次查询返回的最大日志条数
     *              用于控制响应数据量，避免一次性加载过多数据
     * @return JSONObject 工作流日志列表响应对象
     *         包含日志列表总数、当前页码、每页数量以及具体的日志记录数组
     *         每条日志通常包含工作流 ID、状态、开始时间、结束时间、耗时等信息
     *         如果请求失败或无数据则返回 null
     */
    @Override
    public JSONObject getWorkFlowLog(String keyword, WorkflowStatus status, Integer page, Integer limit) {
        String result = SimpleHttpClient.get(config).requestJson(AppRoutes.WORKFLOW_LOGS, new HashMap<String, Object>() {{
            put("keyword", keyword);
            put("status", status.name());
            put("page", page);
            put("limit", limit);
        }}, null);

        if (result == null) {
            return null;
        }

        return JSON.parseObject(result);
    }

}
