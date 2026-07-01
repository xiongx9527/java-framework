package com.xx9527.fw.plus.dify.app;

import com.alibaba.fastjson2.JSON;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import okio.Buffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class DifyEventSourceBaseListener extends EventSourceListener {

    /**
     * 获取请求体内容
     * 
     * <p>该方法通过 Okio Buffer 读取请求体的字节流内容并转换为 UTF-8 字符串。
     * 适用于日志记录、调试或审计请求数据的场景。</p>
     *
     * @param requestBody 请求体对象，可能为 null
     * @return 请求体的字符串内容，读取失败或为空时返回 "unknown"
     */
    private String getRequestBody(RequestBody requestBody) {
        try {
            if (requestBody != null) {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                return buffer.readUtf8();
            }
        } catch (Exception e) {
            log.warn("Failed to read request body", e);
        }
        return "unknown";
    }

    /**
     * 将 EventSource 对象转换为 JSON 字符串
     * 
     * <p>该方法提取 EventSource 中的请求信息（URL、请求头、请求方法、请求体），
     * 并将其序列化为 JSON 格式字符串，用于日志记录和调试。</p>
     *
     * @param eventSource SSE 事件源对象
     * @return JSON 格式的请求信息字符串，包含 url、headers、method、body 字段
     */
    private String eventSource2Str(EventSource eventSource) {
        Map<String, Object> requestMap = new HashMap<>();
        Request request = eventSource.request();
        if (null != request) {
            requestMap.put("url", request.url().url().toString());
            Iterator<Pair<String, String>> iterator = request.headers().iterator();
            Map<String, Object> headersMap = new HashMap<>();
            while (iterator.hasNext()) {
                Pair<String, String> pair = iterator.next();
                headersMap.put(pair.getFirst(), pair.getSecond());
            }
            requestMap.put("headers", headersMap);
            requestMap.put("method", request.method());
            requestMap.put("body", getRequestBody(request.body()));
        }
        return JSON.toJSONString(requestMap);
    }

    /**
     * SSE 连接建立成功时的回调
     * 
     * <p>当服务端响应成功并建立 SSE 连接时触发此方法，记录连接信息和响应状态。</p>
     *
     * @param eventSource SSE 事件源对象
     * @param response HTTP 响应对象，包含状态码、响应头等信息
     */
    public void onOpen(EventSource eventSource, Response response) {
        log.info("onOpen: eventSource={}, response={}", eventSource2Str(eventSource), response);
    }

    /**
     * 接收到 SSE 事件时的回调
     * 
     * <p>当服务端推送新的事件数据时触发此方法，记录事件的 ID、类型和数据内容。</p>
     *
     * @param eventSource SSE 事件源对象
     * @param id 事件 ID，用于标识和追踪特定事件
     * @param type 事件类型，表示事件的分类或用途
     * @param data 事件数据内容，通常为 JSON 格式的字符串
     */
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        log.info("onEvent:  id={}, type={}, data={}",  id, type, data);
    }

    /**
     * SSE 连接关闭时的回调
     * 
     * <p>当 SSE 连接正常关闭或服务端主动断开连接时触发此方法。</p>
     *
     * @param eventSource SSE 事件源对象
     */
    public void onClosed(EventSource eventSource) {
        log.info("onClosed");
    }

    /**
     * SSE 连接发生错误时的回调
     * 
     * <p>当 SSE 连接过程中出现网络异常、服务端错误或解析失败时触发此方法，
     * 记录详细的错误信息和请求上下文，用于问题排查和监控告警。</p>
     *
     * @param eventSource SSE 事件源对象
     * @param t 异常对象，可能为 null（表示非异常导致的连接关闭）
     * @param response HTTP 响应对象，可能为 null（表示请求未到达服务端）
     */
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        String eventSource2Str = eventSource2Str(eventSource);
        if (null == t) {
            log.error("onFailure: eventSource={}, response={}", eventSource2Str, response != null ? response.toString() : "");
        } else {
            log.error("onFailure: eventSource={}, message={}, response={}", eventSource2Str, t.getMessage() != null ? t.getMessage() : t.getClass().getName(), response != null ? response.toString() : "");
        }
    }

}
