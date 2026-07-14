package com.xiongx9527.fw.plus.dify;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.dify.types.*;
import com.xiongx9527.fw.plus.dify.utils.DifyUrlUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.*;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class SimpleHttpClient {

    private static final Pattern JSON_UNICODE_ESCAPE_PATTERN = Pattern.compile("\\\\{1,2}u([0-9a-fA-F]{4})");

    /**
     * 缓存池，用于存储已创建的 SimpleHttpClient 实例
     * <p>
     * 使用 ConcurrentHashMap 保证线程安全，键为 server-apiKey 组合，值为对应的客户端实例。
     * </p>
     */
    private final static Map<String, SimpleHttpClient> CACHE = new ConcurrentHashMap<>();
    
    /**
     * 默认配置的 OkHttpClient 实例
     * <p>
     * 该实例在类加载时创建，全局共享使用。
     * </p>
     */
    private static final OkHttpClient DEFAULT_CLIENT = createDefaultClient();
    
    /**
     * JSON 格式的媒体类型常量
     * <p>
     * 用于构建 application/json 类型的请求体。
     * </p>
     */
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * 二进制流格式的媒体类型常量
     * <p>
     * 用于构建 application/octet-stream 类型的请求体，通常用于文件上传。
     * </p>
     */
    private static final MediaType OCTET_STREAM_MEDIA_TYPE = MediaType.parse("application/octet-stream");

    /**
     * Dify 服务器地址
     */
    private final String server;

    /**
     * API 认证密钥
     */
    private final String apiKey;

    /**
     * OkHttpClient 实例，用于执行 HTTP 请求
     */
    private final OkHttpClient client;

    /**
     * 获取 OkHttpClient 实例
     * <p>
     * 如果当前实例配置了 client，则返回该 client；
     * 否则返回全局默认的 DEFAULT_CLIENT。
     * </p>
     *
     * @return OkHttpClient 实例
     */
    private OkHttpClient getHttpClient() {
        return this.client != null ? this.client : DEFAULT_CLIENT;
    }

    /**
     * 构造 SimpleHttpClient 实例
     * <p>
     * 初始化客户端对象的基本配置，设置服务器地址和 API 密钥。
     * HTTP 客户端实例将在首次请求时懒加载创建。
     * </p>
     *
     * @param server Dify 服务器地址
     * @param apiKey API 认证密钥
     */
    private SimpleHttpClient(String server, String apiKey) {
        this.apiKey = apiKey;
        this.server = server;
        this.client = null;
    }

    /**
     * 构造 SimpleHttpClient 实例
     * <p>
     * 初始化客户端对象，设置服务器地址、API 密钥和 HTTP 客户端实例。
     * </p>
     *
     * @param server Dify 服务器地址
     * @param apiKey API 认证密钥
     * @param client OkHttpClient 实例，用于执行 HTTP 请求
     */
    private SimpleHttpClient(String server, String apiKey, OkHttpClient client) {
        this.apiKey = apiKey;
        this.server = server;
        this.client = client;
    }

    /**
     * 创建默认配置的 OkHttpClient 实例
     * <p>
     * 该方法创建一个全局共享的 OkHttpClient 实例，配置了以下参数：
     * 连接超时、读取超时、写入超时均为 30 秒；
     * 连接池大小为 5 个连接，空闲超时 5 分钟；
     * 启用连接失败自动重试机制。
     * </p>
     *
     * @return 配置好的 OkHttpClient 实例
     */
    private static OkHttpClient createDefaultClient() {
        return buildOkHttpClient(30L, 30L, 30L, 5, 5, true);
    }

    /**
     * 根据配置参数创建 OkHttpClient 实例
     * <p>
     * 该方法从 ApiConfig 配置对象中读取各项参数，
     * 构建并返回一个自定义配置的 OkHttpClient 实例。
     * </p>
     *
     * @param config ApiConfig 配置对象，包含连接超时、读写超时、连接池大小等参数
     * @return 按配置创建的 OkHttpClient 实例
     */
    private static OkHttpClient createClientByConfig(ApiConfig config) {
        return buildOkHttpClient(
                config.getConnectTimeoutSeconds(),
                config.getReadTimeoutSeconds(),
                config.getWriteTimeoutSeconds(),
                config.getMaxIdleConnections(),
                config.getKeepAliveDurationMinutes(),
                config.isRetryOnConnectionFailure()
        );
    }

    /**
     * 构建 OkHttpClient 实例的通用方法
     * 
     * <p>该方法封装了 OkHttpClient 的创建逻辑，通过统一的构建器模式配置各项参数。</p>
     *
     * @param connectTimeout 连接超时时间（秒）
     *                       指定客户端与服务器建立连接的最大等待时间
     * @param readTimeout 读取超时时间（秒）
     *                    指定从服务器读取数据的最大等待时间
     * @param writeTimeout 写入超时时间（秒）
     *                     指定向服务器写入数据的最大等待时间
     * @param maxIdleConnections 连接池最大空闲连接数
     *                           控制连接池中保持的空闲连接数量，超出部分会被回收
     * @param keepAliveDuration 连接空闲存活时间（分钟）
     *                          连接在连接池中保持空闲状态的最长时间
     * @param retryOnConnectionFailure 是否启用连接失败重试
     *                                 - true: 连接失败时自动重试
     *                                 - false: 连接失败时直接抛出异常
     * @return 配置好的 OkHttpClient 实例
     */
    private static OkHttpClient buildOkHttpClient(long connectTimeout, long readTimeout, long writeTimeout,
                                                   int maxIdleConnections, int keepAliveDuration, boolean retryOnConnectionFailure) {
        if (keepAliveDuration <= 0) {
            keepAliveDuration = 5;
        }
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.MINUTES))
                .retryOnConnectionFailure(retryOnConnectionFailure)
                .build();
    }

    /**
     * 从缓存池中获取或创建 SimpleHttpClient 实例
     * <p>
     * 该方法采用缓存池模式管理 HttpClient 实例，避免重复创建。
     * 如果缓存池中存在相同配置的实例则直接返回，否则创建新实例并加入缓存池。
     * </p>
     *
     * @param config DifyConfig 配置对象，包含服务器地址和 API 密钥等信息
     * @return SimpleHttpClient 客户端实例
     */
    public static SimpleHttpClient get(ApiConfig config) {
        //return get(config.getServer(), config.getApiKey());
        return CACHE.computeIfAbsent(String.format("%s-%s", config.getServer(), config.getApiKey()),
                id -> new SimpleHttpClient(config.getServer(), config.getApiKey(), createClientByConfig(config)));
    }

    /**
     * 从缓存池中获取一个 client 对象，如果没有则创建
     *
     * @param server Dify Server Address
     * @param apiKey Api Key
     */
    public static SimpleHttpClient get(String server, String apiKey) {
        return CACHE.computeIfAbsent(String.format("%s-%s", server, apiKey), id -> new SimpleHttpClient(server, apiKey));
    }

    /**
     * 实例化一个新对象
     *
     * @param server Dify Server Address
     */
    public static SimpleHttpClient newHttpClient(String server) {
        return new SimpleHttpClient(server, null);
    }

    /**
     * 实例化一个新对象
     *
     * @param server Dify Server Address
     * @param apiKey Api Key
     */
    public static SimpleHttpClient newHttpClient(String server, String apiKey) {
        return new SimpleHttpClient(server, apiKey);
    }

    /**
     * request by application/json content-type
     *
     * @param route DifyRoute
     * @return String
     */
    public String requestJson(DifyRoute route) {
        return requestJson(route.getUrl(), route.getMethod(), null, null);
    }

    /**
     * request by application/json content-type
     *
     * @param route DifyRoute
     * @param query Query 查询参数
     * @return String
     */
    public String requestJson(DifyRoute route, Map<String, Object> query) {
        return requestJson(route.getUrl(), route.getMethod(), query, null);
    }

    /**
     * request by application/json content-type
     *
     * @param route  DifyRoute
     * @param query  Query 查询参数
     * @param params Body 参数
     * @return String
     */
    public String requestJson(DifyRoute route, Map<String, Object> query, Object params) {
        return requestJson(route.getUrl(), route.getMethod(), query, params);
    }

    /**
     * 发送 JSON 格式的 HTTP 请求并返回响应字符串
     * <p>
     * 该方法支持多种 HTTP 方法（GET、POST、PUT、DELETE、PATCH 等），
     * 自动将参数对象序列化为 JSON 格式作为请求体（GET 请求除外），
     * 并在请求头中添加 Bearer Token 认证信息。
     * </p>
     *
     * @param url    API 相对路径或完整 URL
     * @param method HTTP 请求方法（GET、POST、PUT、DELETE、PATCH 等）
     * @param query  查询参数 Map，将附加到 URL 后面
     * @param params 请求体参数对象，将被序列化为 JSON 格式（GET 请求时忽略）
     * @return 响应内容的字符串表示
     */
    public String requestJson(String url, HttpMethod method, Map<String, Object> query, Object params) {
        try {
            String fullUrl = buildUrl(url, query);
            
            Request.Builder requestBuilder = new Request.Builder()
                    .url(fullUrl);
            
            if (this.apiKey != null) {
                requestBuilder.header("Authorization", "Bearer " + this.apiKey);
            }
            
            // 非 GET 请求需要构建 JSON 请求体
            RequestBody body = null;
            if (method != HttpMethod.GET) {
                String jsonContent = params != null ? JSON.toJSONString(params) : null;
                body = jsonContent != null ? RequestBody.create(jsonContent, JSON_MEDIA_TYPE) : RequestBody.create(new byte[0], JSON_MEDIA_TYPE);
            }
            
            // 根据 HTTP 方法设置对应的请求方式
            if (method == HttpMethod.GET) {
                requestBuilder.get();
            } else if (method == HttpMethod.POST) {
                requestBuilder.post(body);
            } else if (method == HttpMethod.PUT) {
                requestBuilder.put(body);
            } else if (method == HttpMethod.DELETE) {
                requestBuilder.delete(body);
            } else if (method == HttpMethod.PATCH) {
                requestBuilder.patch(body);
            } else {
                requestBuilder.method(method.name(), body);
            }
            
            Request request = requestBuilder.build();
            
            try (Response response = getHttpClient().newCall(request).execute()) {
                int statusCode = response.code();
                String responseBody = readResponseBody(response);
                if (statusCode >= 400) {
                    throw new DifyException(responseBody, statusCode);
                }
                
                return decodeUnicodeEscapes(responseBody);
            }
        } catch (DifyException e) {
            throw e;
        } catch (Exception e) {
            throw new DifyClientException("Dify request error: " + method + " " + DifyUrlUtils.removeQueryAndFragment(url), e);
        }
    }

    private String readResponseBody(Response response) throws java.io.IOException {
        return response.body() != null ? response.body().string() : null;
    }

    private String decodeUnicodeEscapes(String value) {
        if (value == null || value.indexOf("\\u") < 0) {
            return value;
        }
        Matcher matcher = JSON_UNICODE_ESCAPE_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            char ch = (char) Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(ch)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * request by application/json content-type. return File bytes
     *
     * @param route  DifyRoute
     * @param query  query params
     * @param params body params
     */
    public AudioFile requestFile(DifyRoute route, Map<String, Object> query, Object params) {
        return requestFile(route.getUrl(), route.getMethod(), query, params);
    }

    /**
     * 发送 JSON 请求并返回文件字节数据
     * <p>
     * 该方法支持多种 HTTP 方法（GET、POST、PUT、DELETE、PATCH 等），
     * 根据响应头中的 Content-Type 自动识别文件类型和后缀，
     * 将响应内容以字节数组形式返回。
     * </p>
     *
     * @param url    API 相对路径或完整 URL
     * @param method HTTP 请求方法（GET、POST、PUT、DELETE、PATCH 等）
     * @param query  查询参数 Map，将附加到 URL 后面
     * @param params 请求体参数对象，将被序列化为 JSON 格式（GET 请求时忽略）
     * @return AudioFile 包含文件类型、后缀和字节数据的对象
     */
    public AudioFile requestFile(String url, HttpMethod method, Map<String, Object> query, Object params) {
        try {
            String fullUrl = buildUrl(url, query);
            
            Request.Builder requestBuilder = new Request.Builder()
                    .url(fullUrl);
            
            if (this.apiKey != null) {
                requestBuilder.header("Authorization", "Bearer " + this.apiKey);
            }
            
            // 非 GET 请求需要构建 JSON 请求体
            RequestBody body = null;
            if (method != HttpMethod.GET) {
                String jsonContent = params != null ? JSON.toJSONString(params) : null;
                body = jsonContent != null ? RequestBody.create(jsonContent, JSON_MEDIA_TYPE) : RequestBody.create(new byte[0], JSON_MEDIA_TYPE);
            }
            
            // 根据 HTTP 方法设置对应的请求方式
            if (method == HttpMethod.GET) {
                requestBuilder.get();
            } else if (method == HttpMethod.POST) {
                requestBuilder.post(body);
            } else if (method == HttpMethod.PUT) {
                requestBuilder.put(body);
            } else if (method == HttpMethod.DELETE) {
                requestBuilder.delete(body);
            } else if (method == HttpMethod.PATCH) {
                requestBuilder.patch(body);
            } else {
                requestBuilder.method(method.name(), body);
            }
            
            Request request = requestBuilder.build();
            
            try (Response response = getHttpClient().newCall(request).execute()) {
                int statusCode = response.code();
                if (statusCode >= 400) {
                    throw new DifyException(readResponseBody(response), statusCode);
                }
                
                // 从响应头获取文件类型信息并构建返回对象
                String contentType = response.header("Content-Type");
                String suffix = contentType != null ? contentType.replaceAll(".*/", "") : null;
                byte[] data = response.body() != null ? response.body().bytes() : new byte[0];

                return AudioFile.builder()
                        .type(contentType)
                        .suffix(suffix)
                        .data(data)
                        .build();
            }
        } catch (DifyException e) {
            throw e;
        } catch (Exception e) {
            throw new DifyClientException("Dify file request error: " + method + " " + DifyUrlUtils.removeQueryAndFragment(url), e);
        }
    }


    /**
     * 使用 SSE (Server-Sent Events) 方式发起请求
     * 
     * 该方法用于建立与服务器的 SSE 长连接，实时接收服务器推送的事件流数据。
     * 适用于需要持续接收服务端推送数据的场景，如流式响应、实时通知等。
     * 
     * @param route Dify 路由配置，包含 API URL 和 HTTP 请求方法
     * @param query 查询参数 Map，用于构建 URL 查询字符串，为 null 时不添加查询参数
     * @param params 请求体参数 Map，当请求方法不是 GET 时，会被序列化为 JSON 作为请求体
     * @param listener EventSourceListener 事件监听器，用于接收 SSE 连接的生命周期事件和数据
     *                 包括 onOpen(连接打开)、onEvent(接收事件)、onClosed(连接关闭)、onFailure(连接失败)
     * @return EventSource 返回创建的 EventSource 实例，可用于主动关闭连接或管理连接状态
     */
    public EventSource requestSse(
            DifyRoute route,
            Map<String, Object> query,
            Map<String, Object> params,
            EventSourceListener listener
    ) {
        // 构建完整的请求 URL，包含基础 URL 和查询参数
        String fullUrl = buildUrl(route.getUrl(), query);
        
        // 创建 HTTP 请求构建器，设置基础 URL 和 SSE 必需的请求头
        Request.Builder requestBuilder = new Request.Builder()
                .url(fullUrl)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache");
        
        // 如果配置了 API Key，添加 Authorization 认证头
        if (this.apiKey != null) {
            requestBuilder.header("Authorization", "Bearer " + this.apiKey);
        }
        
        // 根据 HTTP 方法构建请求体，非 GET 请求需要设置 JSON 格式的请求体
        RequestBody body = null;
        if (route.getMethod() != HttpMethod.GET) {
            String jsonContent = params != null ? JSON.toJSONString(params) : null;
            body = jsonContent != null ? RequestBody.create(jsonContent, JSON_MEDIA_TYPE) : RequestBody.create(new byte[0], JSON_MEDIA_TYPE);
        }
        
        // 根据不同的 HTTP 方法设置对应的请求方式
        if (route.getMethod() == HttpMethod.GET) {
            requestBuilder.get();
        } else if (route.getMethod() == HttpMethod.POST) {
            requestBuilder.post(body);
        } else if (route.getMethod() == HttpMethod.PUT) {
            requestBuilder.put(body);
        } else if (route.getMethod() == HttpMethod.DELETE) {
            requestBuilder.delete(body);
        } else if (route.getMethod() == HttpMethod.PATCH) {
            requestBuilder.patch(body);
        } else {
            requestBuilder.method(route.getMethod().name(), body);
        }
        
        // 构建最终的 Request 对象
        Request request = requestBuilder.build();
        
        // 创建 EventSource 并返回，开始 SSE 连接
        return EventSources.createFactory(getHttpClient()).newEventSource(request, listener);
    }

    /**
     * request by multipart/form-data content-type
     *
     * @param route  DifyRoute
     * @param query  Query 查询参数
     * @param params Body 参数，文件流需自行在 params 中传入
     * @return String
     */
    public String requestMultipart(DifyRoute route, Map<String, Object> query, Map<String, Object> params) {
        return requestMultipart(route.getUrl(), query, params, null);
    }

    /**
     * request by multipart/form-data content-type
     *
     * @param route  DifyRoute
     * @param query  Query 查询参数
     * @param params Body 参数，文件流需自行在 params 中传入
     * @param fileType 如果该参数不为空，那么 params 中所有 File 或者 Path 类型的对象的 ContentType 将设置为该值
     * @return String
     */
    public String requestMultipart(DifyRoute route, Map<String, Object> query, Map<String, Object> params, String fileType) {
        return requestMultipart(route.getUrl(), query, params, fileType);
    }

    /**
     * 发送 multipart 请求
     *
     * @param url   api url
     * @param query Query params
     * @param params Form params
     * @param fileType 文件类型
     * @return String
     */
    public String requestMultipart(String url, Map<String, Object> query, Map<String, Object> params, String fileType) {
        try {
            String fullUrl = buildUrl(url, query);
            
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);
            
            // 添加表单字段和文件
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof File || value instanceof Path) {
                    Path filePath = (value instanceof Path) ? (Path) value : ((File) value).toPath();
                    File file = filePath.toFile();
                    
                    MediaType mediaType = fileType != null ? MediaType.parse(fileType) : null;
                    RequestBody fileBody = RequestBody.create(file, mediaType != null ? mediaType : OCTET_STREAM_MEDIA_TYPE);
                    
                    multipartBuilder.addFormDataPart(key, file.getName(), fileBody);
                } else if (value instanceof String) {
                    multipartBuilder.addFormDataPart(key, (String) value);
                } else {
                    multipartBuilder.addFormDataPart(key, JSON.toJSONString(value));
                }
            }
            
            Request.Builder requestBuilder = new Request.Builder()
                    .url(fullUrl)
                    .post(multipartBuilder.build());
            
            if (this.apiKey != null) {
                requestBuilder.header("Authorization", "Bearer " + this.apiKey);
            }
            
            Request request = requestBuilder.build();
            
            try (Response response = getHttpClient().newCall(request).execute()) {
                int statusCode = response.code();
                if (statusCode >= 400) {
                    throw new DifyException(readResponseBody(response), statusCode);
                }
                
                return readResponseBody(response);
            }
        } catch (DifyException e) {
            throw e;
        } catch (Exception e) {
            throw new DifyClientException(e);
        }
    }

    /**
     * 构建完整的请求 URL
     * <p>
     * 该方法负责将基础服务器地址与 API 路径拼接，并附加查询参数。
     * 自动处理 server 和 url 连接处的斜杠，确保不会出现重复或缺失的斜杠。
     * </p>
     *
     * @param url   API 相对路径
     * @param query 查询参数 Map，键值对将转换为 URL 查询字符串
     * @return 构建完成的完整 URL 字符串
     */
    private String buildUrl(String url, Map<String, Object> query) {
        return DifyUrlUtils.buildUrl(this.server, url, query);
    }
}
