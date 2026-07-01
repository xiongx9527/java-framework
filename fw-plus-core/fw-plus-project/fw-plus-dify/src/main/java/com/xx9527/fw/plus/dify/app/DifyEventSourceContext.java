package com.xx9527.fw.plus.dify.app;

import lombok.Data;
import okhttp3.Response;
import okhttp3.sse.EventSource;

/**
 * SSE 事件上下文信息封装类
 * 
 * <p>该类用于封装所有 SSE 事件回调的参数，包括事件名称、事件源、响应对象、
 * 异常信息以及事件数据等，便于统一处理和传递事件上下文信息。</p>
 */
@Data
public class DifyEventSourceContext {
    /**
     * 监听器方法名称，如 onOpen、onEvent、onClosed、onFailure
     */
    private String methodName;
    
    /**
     * SSE 事件源对象
     */
    private EventSource eventSource;
    
    /**
     * HTTP 响应对象
     */
    private Response response;
    
    /**
     * 异常对象，仅在 onFailure 事件中有效
     */
    private Throwable exception;
    
    /**
     * 事件 ID，仅在 onEvent 事件中有效
     */
    private String eventId;
    
    /**
     * 事件类型，仅在 onEvent 事件中有效
     */
    private String eventType;
    
    /**
     * 事件数据内容，仅在 onEvent 事件中有效
     */
    private String eventData;
}