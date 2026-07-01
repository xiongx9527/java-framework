package com.xiongx9527.fw.plus.dify.app;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;

import java.util.function.Function;

/**
 * Dify SSE 事件默认监听器
 * 
 * <p>该类继承自 DifyEventSourceBaseListener，通过函数式接口的方式统一处理 SSE 事件回调。
 * 所有事件回调都会将参数封装为 SseEventContext 对象并调用传入的 Function 进行处理，
 * 适用于需要集中处理或动态配置事件处理逻辑的场景。</p>
 */
@Slf4j
public class DifyEventSourceDefaultListener extends DifyEventSourceBaseListener{
    
    /**
     * 事件处理函数，接收 SseEventContext 参数并返回 Void
     */
    private final Function<DifyEventSourceContext, Void> function;

    /**
     * 构造函数
     * 
     * <p>创建一个默认的事件监听器实例，传入自定义的事件处理函数。</p>
     *
     * @param function 事件处理函数，用于统一处理所有 SSE 事件回调
     */
    public DifyEventSourceDefaultListener(Function<DifyEventSourceContext, Void> function) {
        this.function = function;
    }

    /**
     * SSE 连接建立成功时的回调
     * 
     * <p>当服务端响应成功并建立 SSE 连接时触发此方法，将事件参数封装后调用处理函数。</p>
     *
     * @param eventSource SSE 事件源对象
     * @param response HTTP 响应对象，包含状态码、响应头等信息
     */
    public final void onOpen(EventSource eventSource, Response response) {
        super.onOpen(eventSource, response);
        DifyEventSourceContext context = new DifyEventSourceContext();
        context.setMethodName("onOpen");
        context.setEventSource(eventSource);
        context.setResponse(response);
        this.function.apply(context);
    }
    
    /**
     * 接收到 SSE 事件时的回调
     * 
     * <p>当服务端推送新的事件数据时触发此方法，将事件参数封装后调用处理函数。</p>
     *
     * @param eventSource SSE 事件源对象
     * @param id 事件 ID，用于标识和追踪特定事件
     * @param type 事件类型，表示事件的分类或用途
     * @param data 事件数据内容，通常为 JSON 格式的字符串
     */
    public final void onEvent(EventSource eventSource, String id, String type, String data) {
        super.onEvent(eventSource, id, type, data);
        DifyEventSourceContext context = new DifyEventSourceContext();
        context.setMethodName("onEvent");
        context.setEventSource(eventSource);
        context.setEventId(id);
        context.setEventType(type);
        context.setEventData(data);
        this.function.apply(context);
    }
    
    /**
     * SSE 连接关闭时的回调
     * 
     * <p>当 SSE 连接正常关闭或服务端主动断开连接时触发此方法，将事件参数封装后调用处理函数。</p>
     *
     * @param eventSource SSE 事件源对象
     */
    public final void onClosed(EventSource eventSource) {
        super.onClosed(eventSource);
        DifyEventSourceContext context = new DifyEventSourceContext();
        context.setMethodName("onClosed");
        context.setEventSource(eventSource);
        this.function.apply(context);
    }
    
    /**
     * SSE 连接发生错误时的回调
     * 
     * <p>当 SSE 连接过程中出现网络异常、服务端错误或解析失败时触发此方法，
     * 将事件参数封装后调用处理函数，用于问题排查和错误处理。</p>
     *
     * @param eventSource SSE 事件源对象
     * @param t 异常对象，可能为 null（表示非异常导致的连接关闭）
     * @param response HTTP 响应对象，可能为 null（表示请求未到达服务端）
     */
    public final void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (null == t) {
            t = new Exception(response.message());
        }
        super.onFailure(eventSource, t, response);
        DifyEventSourceContext context = new DifyEventSourceContext();
        context.setMethodName("onFailure");
        context.setEventSource(eventSource);
        context.setException(t);
        context.setResponse(response);
        this.function.apply(context);
    }

}
