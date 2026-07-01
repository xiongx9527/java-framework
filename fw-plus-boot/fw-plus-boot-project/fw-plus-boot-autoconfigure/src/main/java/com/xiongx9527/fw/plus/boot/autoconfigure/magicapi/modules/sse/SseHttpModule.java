package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.sse;

import com.alibaba.fastjson.JSON;
import com.xiongx9527.fw.plus.dify.app.DifyEventSourceBaseListener;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import okio.ByteString;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@MagicModule("ssehttp")
public class SseHttpModule {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * HTTP请求头映射
     */
    private Map<String, String> headers = new HashMap<>();
    
    /**
     * HTTP请求体
     */
    private RequestBody body;
    
    /**
     * OkHttp客户端实例
     */
    private final OkHttpClient okHttpClient ;
    
    /**
     * 请求参数映射，支持同一参数名对应多个值
     */
    private final MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    
    /**
     * HTTP请求方法，默认为POST
     */
    private HttpMethod method = HttpMethod.POST;
    
    /**
     * 请求URL地址
     */
    private String url;

    /**
     * 构造SseHttpModule实例，并根据配置属性初始化OkHttpClient
     *
     * @param sseHttpProperties SSE HTTP配置属性，包含连接超时、读取超时和写入超时等配置
     */
    public SseHttpModule(SseHttpProperties sseHttpProperties) {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(sseHttpProperties.getConnectTimeoutSec(), TimeUnit.SECONDS)
                .readTimeout(sseHttpProperties.getReadTimeoutSec(), TimeUnit.SECONDS)
                .writeTimeout(sseHttpProperties.getWriteTimeoutSec(), TimeUnit.SECONDS)
                .build();
    }

    /**
     * 构造SseHttpModule实例
     *
     * @param okHttpClient OkHttp客户端实例
     * @param url 请求URL地址
     */
    private SseHttpModule(OkHttpClient okHttpClient,String url) {
        this.okHttpClient = okHttpClient;
        this.url = url;
    }

    /**
     * 创建一个新的SseHttpModule实例并连接到指定URL
     *
     * @param url 请求URL地址
     * @return 新的SseHttpModule实例
     */
    public SseHttpModule connect(String url){
        return new SseHttpModule(okHttpClient, url);
    }

    /**
     * 添加请求参数，支持同一参数名对应多个值
     *
     * @param key 参数名
     * @param values 参数值列表
     * @return 当前SseHttpModule实例，支持链式调用
     */
    public SseHttpModule param(String key,Object... values) {
        if (values != null) {
            for (Object value : values) {
                this.params.add(key, value);
            }
        }
        return this;
    }

    /**
     * 批量添加请求参数
     *
     * @param values 参数映射表
     * @return 当前SseHttpModule实例，支持链式调用
     */
    public SseHttpModule param(Map<String, Object> values) {
        values.forEach((key, value) -> param(key, Objects.toString(value, "")));
        return this;
    }

    /**
     * 设置单个请求头
     *
     * @param key 请求头名称
     * @param value 请求头值
     * @return 当前SseHttpModule实例，支持链式调用
     */
    public SseHttpModule header(String key,String value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * 批量设置请求头，过滤掉值为null的项
     *
     * @param values 请求头映射表
     * @return 当前SseHttpModule实例，支持链式调用
     */
    public SseHttpModule header(Map<String, Object> values) {
        values.entrySet()
                .stream()
                .filter(it -> it.getValue() != null)
                .forEach(entry -> header(entry.getKey(), entry.getValue().toString()));
        return this;
    }

    /**
     * 设置HTTP请求方法
     *
     * @param method HTTP请求方法
     * @return 当前SseHttpModule实例，支持链式调用
     */
    public SseHttpModule method(HttpMethod method) {
        this.method = method;
        return this;
    }

    /**
     * 设置请求体，支持String、byte数组和File类型
     *
     * @param requestBody 请求体对象，支持String、byte[]、File类型
     * @return 当前SseHttpModule实例，支持链式调用
     */
    public SseHttpModule body(Object requestBody) {
        if (requestBody instanceof String) {
            this.body = RequestBody.create((String)requestBody,JSON_MEDIA_TYPE);
        } else if (requestBody instanceof byte[]) {
            this.body = RequestBody.create((byte[])requestBody,JSON_MEDIA_TYPE);
        } else if (requestBody instanceof File) {
            this.body = RequestBody.create((File)requestBody,JSON_MEDIA_TYPE);//ByteString
        } else if (requestBody instanceof ByteString) {
            this.body = RequestBody.create((ByteString)requestBody,JSON_MEDIA_TYPE);
        } else {
            this.body = RequestBody.create(JSON.toJSONString(requestBody),JSON_MEDIA_TYPE);
        }
        return this;
    }

    /**
     * 执行POST请求
     *
     * @param listener SSE事件监听器
     * @return EventSource实例，用于管理SSE连接
     */
    public EventSource post(EventSourceListener listener) {
        this.method(HttpMethod.POST);
        return this.execute(listener);
    }

    /**
     * 执行 POST 请求（使用默认事件监听器）
     * 
     * <p>该方法使用默认的 DifyEventSourceBaseListener 作为事件监听器来执行 POST 请求，
     * 适用于不需要自定义事件处理逻辑的常规 SSE 场景。</p>
     *
     * @return EventSource 事件源对象，用于管理 SSE 连接的生命周期
     */
    public EventSource post() {
        return this.post(new DifyEventSourceBaseListener());
    }

    /**
     * 执行GET请求
     *
     * @param listener SSE事件监听器
     * @return EventSource实例，用于管理SSE连接
     */
    public EventSource get(EventSourceListener listener) {
        this.method(HttpMethod.GET);
        return this.execute(listener);
    }

    /**
     * 执行 GET 请求（使用默认事件监听器）
     * 
     * <p>该方法使用默认的 DifyEventSourceBaseListener 作为事件监听器来执行 GET 请求，
     * 适用于不需要自定义事件处理逻辑的常规 SSE 场景。</p>
     *
     * @return EventSource 事件源对象，用于管理 SSE 连接的生命周期
     */
    public EventSource get() {
        return this.get(new DifyEventSourceBaseListener());
    }

    /**
     * 执行PUT请求
     *
     * @param listener SSE事件监听器
     * @return EventSource实例，用于管理SSE连接
     */
    public EventSource put(EventSourceListener listener) {
        this.method(HttpMethod.PUT);
        return this.execute(listener);
    }

    /**
     * 执行 PUT 请求（使用默认事件监听器）
     * 
     * <p>该方法使用默认的 DifyEventSourceBaseListener 作为事件监听器来执行 PUT 请求，
     * 适用于不需要自定义事件处理逻辑的常规 SSE 场景。</p>
     *
     * @return EventSource 事件源对象，用于管理 SSE 连接的生命周期
     */
    public EventSource put() {
        return this.put(new DifyEventSourceBaseListener());
    }

    /**
     * 执行DELETE请求
     *
     * @param listener SSE事件监听器
     * @return EventSource实例，用于管理SSE连接
     */
    public EventSource delete(EventSourceListener listener) {
        this.method(HttpMethod.DELETE);
        return this.execute(listener);
    }

    /**
     * 执行 DELETE 请求（使用默认事件监听器）
     * 
     * <p>该方法使用默认的 DifyEventSourceBaseListener 作为事件监听器来执行 DELETE 请求，
     * 适用于不需要自定义事件处理逻辑的常规 SSE 场景。</p>
     *
     * @return EventSource 事件源对象，用于管理 SSE 连接的生命周期
     */
    public EventSource delete() {
        return this.delete(new DifyEventSourceBaseListener());
    }

    /**
     * 执行HEAD请求
     *
     * @param listener SSE事件监听器
     * @return EventSource实例，用于管理SSE连接
     */
    public EventSource head(EventSourceListener listener) {
        this.method(HttpMethod.HEAD);
        return this.execute(listener);
    }

    /**
     * 执行 HEAD 请求（使用默认事件监听器）
     * 
     * <p>该方法使用默认的 DifyEventSourceBaseListener 作为事件监听器来执行 HEAD 请求，
     * 适用于不需要自定义事件处理逻辑的常规 SSE 场景。</p>
     *
     * @return EventSource 事件源对象，用于管理 SSE 连接的生命周期
     */
    public EventSource head() {
        return this.head(new DifyEventSourceBaseListener());
    }

    /**
     * 执行OPTIONS请求
     *
     * @param listener SSE事件监听器
     * @return EventSource实例，用于管理SSE连接
     */
    public EventSource options(EventSourceListener listener) {
        this.method(HttpMethod.OPTIONS);
        return this.execute(listener);
    }

    /**
     * 执行 OPTIONS 请求（使用默认事件监听器）
     * 
     * <p>该方法使用默认的 DifyEventSourceBaseListener 作为事件监听器来执行 OPTIONS 请求，
     * 适用于不需要自定义事件处理逻辑的常规 SSE 场景。</p>
     *
     * @return EventSource 事件源对象，用于管理 SSE 连接的生命周期
     */
    public EventSource options() {
        return this.options(new DifyEventSourceBaseListener());
    }

    /**
     * 执行TRACE请求
     *
     * @param listener SSE事件监听器
     * @return EventSource实例，用于管理SSE连接
     */
    public EventSource trace(EventSourceListener listener) {
        this.method(HttpMethod.TRACE);
        return this.execute(listener);
    }

    /**
     * 执行 TRACE 请求（使用默认事件监听器）
     * 
     * <p>该方法使用默认的 DifyEventSourceBaseListener 作为事件监听器来执行 TRACE 请求，
     * 适用于不需要自定义事件处理逻辑的常规 SSE 场景。</p>
     *
     * @return EventSource 事件源对象，用于管理 SSE 连接的生命周期
     */
    public EventSource trace() {
        return this.trace(new DifyEventSourceBaseListener());
    }

    /**
     * 执行PATCH请求
     *
     * @param listener SSE事件监听器
     * @return EventSource实例，用于管理SSE连接
     */
    public EventSource patch(EventSourceListener listener) {
        this.method(HttpMethod.PATCH);
        return this.execute(listener);
    }

    /**
     * 执行 PATCH 请求（使用默认事件监听器）
     * 
     * <p>该方法使用默认的 DifyEventSourceBaseListener 作为事件监听器来执行 PATCH 请求，
     * 适用于不需要自定义事件处理逻辑的常规 SSE 场景。</p>
     *
     * @return EventSource 事件源对象，用于管理 SSE 连接的生命周期
     */
    public EventSource patch() {
        return this.patch(new DifyEventSourceBaseListener());
    }

    /**
     * 执行HTTP请求并建立SSE连接
     * 该方法会构建完整的HTTP请求，包括URL参数、请求头和请求体，
     * 并自动设置SSE所需的Accept和Cache-Control头
     *
     * @param listener SSE事件监听器，用于接收服务端推送的事件
     * @return EventSource实例，用于管理SSE连接
     */
    public EventSource execute(EventSourceListener listener) {
        Request.Builder requestBuilder = new Request.Builder();
        
        // 构建URL查询参数字符串
        if (!this.params.isEmpty()) {
            StringBuilder queryString = new StringBuilder();
            for (Map.Entry<String, List<Object>> entry : this.params.entrySet()) {
                for (Object value : entry.getValue()) {
                    if (queryString.length() > 0) {
                        queryString.append('&');
                    }
                    queryString.append(entry.getKey()).append('=').append(value);
                }
            }
            if (queryString.length() > 0) {
                this.url += (this.url.contains("?") ? "&" : "?") + queryString;
            }
        }
        
        // 设置SSE必需的请求头（使用局部副本，避免污染成员变量）
        Map<String, String> requestHeaders = new HashMap<>(this.headers);
        requestHeaders.put("Accept", "text/event-stream");
        requestHeaders.put("Cache-Control", "no-cache");
        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            requestBuilder = requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        // GET和DELETE方法不允许包含请求体
        if (HttpMethod.GET.equals(this.method) || HttpMethod.DELETE.equals(this.method)) {
            this.body = null;
        }

        requestBuilder = requestBuilder.url(this.url).method(this.method.name(), this.body);
        EventSource.Factory factory = EventSources.createFactory(okHttpClient);
        return factory.newEventSource(requestBuilder.build(), listener);
    }

    /**
     * 执行 SSE 请求（使用默认事件监听器）
     * 
     * <p>该方法使用默认的 DifyEventSourceBaseListener 作为事件监听器来执行 SSE 请求，
     * 适用于不需要自定义事件处理逻辑的常规 SSE 场景。</p>
     *
     * @return EventSource 事件源对象，用于管理 SSE 连接的生命周期
     */
    public EventSource execute() {
        return execute(new DifyEventSourceBaseListener());
    }


}
