package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.BaseDefinition;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.ssssssss.magicapi.core.config.Constants.VAR_NAME_REQUEST_BODY;

@Slf4j
public abstract class BaseDynamicSseHandler implements BaseService {

    /**
     * 客户端ID与会话列表映射的缓存实例
     *
     * <p>该缓存用于存储客户端ID与其对应会话列表的映射关系，具有以下特性：</p>
     * <ul>
     *   <li>基于LRU策略进行缓存淘汰</li>
     *   <li>支持配置最大缓存大小</li>
     *   <li>支持配置缓存过期时间</li>
     *   <li>支持配置并发访问级别</li>
     *   <li>提供缓存移除监听机制</li>
     * </ul>
     *
     * <p>当缓存项被移除时，会自动关闭对应的会话连接，确保资源得到及时释放。</p>
     */
    private final com.google.common.cache.Cache<String, List<SessionInfo>> clientIdSessionListMap =
        com.google.common.cache.CacheBuilder.newBuilder()
                .maximumSize(getMaxClientSessions())
                .expireAfterWrite(getSessionCacheExpireMinutes(), java.util.concurrent.TimeUnit.MINUTES)
                .concurrencyLevel(getCacheConcurrencyLevel())
                .removalListener(new RemovalListener<String, List<SessionInfo>>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, List<SessionInfo>> notification) {
                        log.warn("Client session list removed from cache: {} - {}",
                                notification.getKey(), notification.getCause());
                        List<SessionInfo> sessions = notification.getValue();
                        if (sessions != null) {
                            // 遍历并关闭所有被移除的会话
                            for (SessionInfo session : sessions) {
                                try {
                                    delayedClose(session, "Service overload", 1011, getDelayMillis());
                                } catch (Exception e) {
                                    log.error("Failed to close session for client: {}", notification.getKey(), e);
                                }
                            }
                        }
                    }
                })
                .build();

    /**
     * 魔法数据源信息存储资源
     * 用于存储和管理SSE数据源的相关信息
     */
    private final SseDataSourceInfoMagicResourceStorage magicDataSourceStorage;

    /**
     * 魔法订阅信息存储资源
     * 用于存储和管理SSE订阅相关的信息
     */
    private final SseSubscribeInfoMagicResourceStorage magicSubscribeStorage ;

    /**
     * SSE服务器唯一标识符
     * 通过generateSseServerId()方法生成的服务器ID
     */
    private final String sseServerId = generateSseServerId();

    /**
     * 缓存的 MagicAPIProperties web base path，避免每次 MCP 连接时查找 Spring Bean。
     */
    private volatile String cachedWebBasePath;
    private volatile boolean webBasePathResolved;

    /**
     * 共享的调度线程池，用于延迟关闭等定时任务，避免每次创建新线程池
     */
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "sse-delayed-close");
                t.setDaemon(true);
                return t;
            });

    /**
     * MCP 消息异步处理专用线程池，避免占用 ForkJoinPool.commonPool()。
     */
    private final ExecutorService mcpExecutor = new ThreadPoolExecutor(
            2, Runtime.getRuntime().availableProcessors(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            r -> {
                Thread t = new Thread(r, "sse-mcp-worker");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());

     /**
     * 构造函数，初始化BaseDynamicSseHandler实例
     *
     * @param magicDataSourceStorage 数据源信息存储器，用于管理SSE数据源的相关信息
     * @param magicSubscribeStorage 订阅信息存储器，用于管理SSE订阅相关的信息
     */
    public BaseDynamicSseHandler(SseDataSourceInfoMagicResourceStorage magicDataSourceStorage, SseSubscribeInfoMagicResourceStorage magicSubscribeStorage) {
        this.magicDataSourceStorage = magicDataSourceStorage;
        this.magicSubscribeStorage = magicSubscribeStorage;
        addShutdownHook();
    }

    /**
     * 获取SSE服务器ID
     *
     * @return 返回当前实例的SSE服务器ID字符串
     */
    public String getSseServerId() {
        return this.sseServerId;
    }

    /**
     * 添加JVM关闭钩子，确保在应用关闭时能清理所有资源
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupAllResources));
    }

    /**
     * 清理所有资源，在应用关闭时调用
     */
    private void cleanupAllResources() {
        try {
            clientIdSessionListMap.invalidateAll();
            scheduledExecutor.shutdownNow();
            mcpExecutor.shutdownNow();
            log.info("All Sse resources have been cleaned up");
        } catch (Exception e) {
            log.error("Error during cleanup of all resources", e);
        }
    }


    /**
     * 根据应用ID清理相关会话信息
     *
     * @param appId 应用ID，用于匹配需要清理的会话
     */
    public void cleanupByAppId(String appId) {
        Map<String, List<SessionInfo>> snapshot = new HashMap<>(clientIdSessionListMap.asMap());
        // 收集每个 clientId 下需要清理的所有 sessionId
        Map<String, Set<String>> clientSessionIdsMap = new HashMap<>();
        for (Map.Entry<String, List<SessionInfo>> e : snapshot.entrySet()) {
            for (SessionInfo session : e.getValue()) {
                if (appId.equals(session.getAppId())) {
                    clientSessionIdsMap.computeIfAbsent(session.getClientId(), k -> new HashSet<>())
                            .add(session.getSessionId());
                }
            }
        }
        for (Map.Entry<String, Set<String>> entry : clientSessionIdsMap.entrySet()) {
            String clientId = entry.getKey();
            for (String sessionId : entry.getValue()) {
                cleanSession(clientId, sessionId, null);
            }
        }
    }


    /**
     * 生成SSE服务器唯一标识符
     * <p>
     * 该方法首先尝试通过主机名和进程ID组合生成唯一ID，
     * 如果获取主机信息失败，则回退使用UUID生成唯一标识符。
     * </p>
     *
     * @return 服务器唯一标识符字符串
     */
    private String generateSseServerId() {
        try {
            // 尝试获取主机名和进程ID来生成唯一ID
            String hostName = java.net.InetAddress.getLocalHost().getHostName();
            String processId = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            return hostName + "-" + processId;
        } catch (Exception e) {
            // 如果无法获取主机信息，则使用UUID生成唯一ID
            log.warn("Failed to generate server ID based on host and process, using UUID instead", e);
            return "server-" + java.util.UUID.randomUUID().toString();
        }
    }

    /**
     * 执行生命周期订阅脚本（init/close），根据业务类型查找并执行对应的订阅脚本。
     */
    private void executeLifecycleScript(String appId, String sessionId, String clientId, String businessType) {
        List<SseSubscribeInfo> subscribeInfoList = ListUtils.emptyIfNull(magicSubscribeStorage.findAll()).stream()
                .filter(s -> Boolean.TRUE.equals(s.isEnabled()) && s.getKey().equals(appId)
                        && businessType.equals(s.getBusinessId()))
                .collect(Collectors.toList());
        for (SseSubscribeInfo subscribeInfo : subscribeInfoList) {
            MagicScriptContext context = new MagicScriptContext();
            try {
                context.setScriptName(subscribeInfo.getName());
                context.set("clientId", clientId);
                context.set("appId", appId);
                context.set("type", subscribeInfo.getBusinessId());
                context.set("sessionId", sessionId);
                Object result = ScriptManager.executeScript(subscribeInfo.getScript(), context);
                log.info("Lifecycle script [{}] result - sessionId:{}, appId:{}: {}", businessType, sessionId, appId, result);
            } catch (Exception e) {
                log.error("Error executing lifecycle script [{}]: {}", businessType, e.getMessage(), e);
            }
        }
    }

    /**
     * 建立 SSE 连接。
     * 校验 appId、secret 和 clientId 参数，验证数据源配置，创建 SSE Emitter 实例，
     * 注册连接生命周期回调，并在 MCP 模式下发送端点事件。
     *
     * @param appId 应用唯一标识符
     * @param secret 应用密钥，用于身份验证
     * @param clientId 客户端唯一标识符
     * @return 创建的 SseEmitter 实例
     * @throws IllegalArgumentException 当 appId、secret 或 clientId 为空，或 appId 不存在时抛出
     * @throws RuntimeException 当建立连接过程中发生异常时抛出
     */
    public SseEmitter connect(String appId, String secret, String clientId) {
        final String[] connectionIdHolder = new String[1];
        final boolean[] sessionAdded = {false};
        try {
            if (StringUtils.isBlank(appId) || StringUtils.isBlank(secret) || StringUtils.isBlank(clientId)) {
                log.error("Missing required parameters - appId: {}, Client ID: {}", appId, clientId);
                throw new IllegalArgumentException("Missing required parameters: appId and clientId");
            }

            List<SseDataSourceInfo> dataSourceList = ListUtils.emptyIfNull(magicDataSourceStorage.findAll()).stream()
                    .filter(e -> appId.equals(e.getKey()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(dataSourceList)) {
                log.error("AppId not exist: {}", appId);
                throw new IllegalArgumentException("AppId not exist");
            }
            if (dataSourceList.size() != 1) {
                log.error("AppId is not unique: {}", appId);
                throw new IllegalArgumentException("AppId is not unique");
            }
            SseDataSourceInfo dataSourceInfo = dataSourceList.get(0);

            String configSecret = MagicAPIConfigUtils.getSseConfig(dataSourceInfo.getKey(), "secret", dataSourceInfo.getSecret());
            if (!configSecret.equals(secret)) {
                log.error("Invalid secret for appId: {}", appId);
                throw new IllegalArgumentException("Invalid secret");
            }

            SseEmitter emitter = new SseEmitter(getTimeOut());
            String connectionId = clientId + "@" + UUID.randomUUID().toString();
            connectionIdHolder[0] = connectionId;
            SessionInfo clientSseInfo = new SessionInfo(emitter, connectionId, appId, clientId);

            addSession(appId, clientSseInfo, clientId);
            sessionAdded[0] = true;
            executeLifecycleScript(appId, connectionId, clientId, getInitBussinessType());

            emitter.onCompletion(() -> {
                log.info("SSE connection completed - Connection ID: {}, Client ID: {}", connectionId, clientId);
                executeLifecycleScript(appId, connectionId, clientId, getCloseBussinessType());
                cleanSession(clientId, connectionId, null);
            });

            emitter.onError(throwable -> {
                log.error("SSE connection error - Connection ID: {}, Client ID: {}", connectionId, clientId, throwable);
                executeLifecycleScript(appId, connectionId, clientId, getCloseBussinessType());
                handleTransportError(clientSseInfo, throwable);
            });

            emitter.onTimeout(() -> {
                log.info("SSE connection timed out - Connection ID: {}, Client ID: {}", connectionId, clientId);
                executeLifecycleScript(appId, connectionId, clientId, getCloseBussinessType());
                cleanSession(clientId, connectionId, null);
            });

            log.info("SSE connection established - Connection ID: {}, Client ID: {}, App ID: {}",
                    connectionId, clientId, appId);

            if (dataSourceInfo.isMcp()) {
                sendMcpEndpointEvent(emitter, dataSourceInfo, connectionId, clientId);
            }
            return emitter;
        } catch (Exception e) {
            log.error("Error during SSE connection establishment - Client ID: {}", clientId, e);
            if (sessionAdded[0] && connectionIdHolder[0] != null) {
                cleanSession(clientId, connectionIdHolder[0], null);
            }
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new RuntimeException("Error during SSE connection establishment", e);
        }
    }

    /**
     * 从数据源 other 配置中解析 MCP 配置。
     */
    private JSONObject parseMcpConfig(SseDataSourceInfo dataSourceInfo) {
        String otherStr = MagicAPIConfigUtils.getSseConfig(dataSourceInfo.getKey(), "other", dataSourceInfo.getOther());
        if (StringUtils.isNotBlank(otherStr)) {
            JSONObject other = JSON.parseObject(otherStr);
            if (other.containsKey("mcp") && other.get("mcp") instanceof JSONObject) {
                return other.getJSONObject("mcp");
            }
        }
        return new JSONObject();
    }

    private String getCachedWebBasePath() {
        if (!webBasePathResolved) {
            synchronized (this) {
                if (!webBasePathResolved) {
                    MagicAPIProperties magicAPIProperties = SpringUtils.getBean(MagicAPIProperties.class);
                    String path = magicAPIProperties != null && magicAPIProperties.getWeb() != null
                            ? magicAPIProperties.getWeb() : "";
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    if (path.endsWith("/") && path.length() > 1) {
                        path = path.substring(0, path.length() - 1);
                    }
                    cachedWebBasePath = path;
                    webBasePathResolved = true;
                }
            }
        }
        return cachedWebBasePath;
    }

    private void sendMcpEndpointEvent(SseEmitter emitter, SseDataSourceInfo dataSourceInfo,
                                      String connectionId, String clientId) {
        try {
            JSONObject mcp = parseMcpConfig(dataSourceInfo);
            String endpointEventName = mcp.getString("endpointEventName");
            if (StringUtils.isBlank(endpointEventName)) {
                endpointEventName = "endpoint";
            }
            String endpointPath = mcp.getString("endpointPath");
            if (StringUtils.isBlank(endpointPath)) {
                endpointPath = "/mcp/message";
            }
            if (!endpointPath.startsWith("/")) {
                endpointPath = "/" + endpointPath;
            }
            String webBasePath = getCachedWebBasePath();
            endpointPath = webBasePath + endpointPath;
            SseEmitter.SseEventBuilder event = SseEmitter.event().name(endpointEventName)
                    .data(String.format("%s?sessionId=%s", endpointPath, connectionId));
            emitter.send(event);
        } catch (Exception e) {
            log.error("Error scheduling SSE event - Client ID: {}", clientId, e);
            emitter.completeWithError(e);
        }
    }


    /**
     * 将 magicapi 的数据类型转换为 JSON Schema 类型
     *
     * @param dataType magicapi 数据类型枚举
     * @return JSON Schema 类型字符串
     */
    private String convertDataType(org.ssssssss.magicapi.core.model.DataType dataType) {
        if (dataType == null) {
            return "string";
        }
        switch (dataType) {
            case Integer:
            case Long:
            case Short:
            case Byte:
                return "integer";
            case Double:
            case Float:
                return "number";
            case Boolean:
                return "boolean";
            case Array:
                return "array";
            case Date:
                return "string";
            case MultipartFile:
            case MultipartFiles:
                return "string";
            case Any:
            case Object:
                return "object";
            default:
                return "string";
        }
    }

    /**
     * 根据分组 ID 获取该分组下的所有 API 信息
     * <p>
     * 该方法通过 MagicResourceService 查询指定分组下的所有接口文件，
     * 返回包含该分组下所有 API 元数据的列表。
     * </p>
     *
     * @param groupId 分组 ID，用于标识要查询的 API 分组
     * @return List<ApiInfo> 包含指定分组下所有 API 信息的列表，如果分组不存在或为空则返回空列表
     */
    public List<ApiInfo> getApiInfosByGroup(String groupId) {
        return MagicConfiguration.getMagicResourceService().listFiles(groupId);
    }


    /**
     * 构建属性映射表，将参数定义列表转换为以参数名为键的 Schema 映射
     *
     * @param parameters 基础定义对象列表，包含多个参数的完整信息
     * @return 属性映射 Map，键为参数名称，值为对应的参数 Schema 对象
     */
    private Map<String,Object> buildProperties(List<BaseDefinition> parameters, List<String> requiredNames) {
        if (CollectionUtils.isEmpty(parameters)) {
            return new HashMap<>();
        }
        Map<String, Object> properties = new HashMap<>(parameters.size());
        parameters.forEach(parameter -> {
            String paramName = parameter.getName();
            if (paramName == null) {
                return;
            }
            properties.put(paramName, buildParamSchema(parameter, requiredNames));
        });
        return properties;
    }

    /**
     * 构建 JSON Schema 兼容的参数定义（仅输出标准字段）。
     */
    private Map<String, Object> buildParamSchema(BaseDefinition parameter, List<String> requiredNames) {
        Map<String, Object> paramSchema = new HashMap<>();
        paramSchema.put("type", convertDataType(parameter.getDataType()));
        paramSchema.put("description", StringUtils.defaultString(parameter.getDescription(), ""));
        if (parameter.isRequired() && requiredNames != null) {
            requiredNames.add(parameter.getName());
        }
        if (CollectionUtils.isNotEmpty(parameter.getChildren())) {
            List<String> childRequired = new ArrayList<>();
            Map<String, Object> childProperties = buildProperties(parameter.getChildren(), childRequired);
            paramSchema.put("properties", childProperties);
            if (!childRequired.isEmpty()) {
                paramSchema.put("required", childRequired);
            }
        }
        return paramSchema;
    }

    /**
     * 构建工具列表，将 API 信息转换为包含名称、路径、描述和输入 Schema 的工具映射
     *
     * @param apiInfos API 信息列表，包含多个 API 的完整定义
     * @return 工具映射列表，每个工具包含 name、path、description 和 inputSchema 字段
     */
    private List<Map<String, Object>> buildTools(List<ApiInfo> apiInfos) {
        // 如果 API 信息列表为空，直接返回空列表
        if (CollectionUtils.isEmpty(apiInfos)) {
            return new ArrayList<>();
        }
        
        // 初始化指定容量的 ArrayList，避免扩容开销
        List<Map<String, Object>> tools = new ArrayList<>(apiInfos.size());

        // 遍历所有 API 信息，逐个构建工具映射
        for (ApiInfo apiInfo : apiInfos) {
            // 获取 API 的 ID 和路径
            String apiId = apiInfo.getId();
            String apiPath = apiInfo.getPath();
            
            // 跳过 ID 或路径为空的 API
            if (apiId == null || apiPath == null) {
                continue;
            }
            
            // 初始化指定容量的 HashMap，避免扩容开销
            Map<String, Object> tool = new HashMap<>(4);
            // 设置工具名称为 API ID
            tool.put("name", apiId);
            // 设置工具路径为 API 路径
            tool.put("path", apiPath);
            // 设置工具描述，如果描述为空则使用默认描述
            tool.put("description", StringUtils.defaultString(apiInfo.getDescription(), "API endpoint: " + apiPath));

            // 构建 JSON Schema 兼容的 inputSchema
            List<String> requiredNames = new ArrayList<>();
            Map<String, Object> inputProperties = buildProperties(
                    ListUtils.emptyIfNull(apiInfo.getParameters()).stream()
                    .filter(e -> e instanceof BaseDefinition)
                    .map(e -> (BaseDefinition) e)
                    .collect(Collectors.toList()),
                    requiredNames);

            Map<String, Object> inputSchema = new LinkedHashMap<>(3);
            inputSchema.put("type", "object");
            inputSchema.put("properties", inputProperties);
            if (!requiredNames.isEmpty()) {
                inputSchema.put("required", requiredNames);
            }
            tool.put("inputSchema", inputSchema);

            tools.add(tool);
        }
        return tools;
    }


    /**
     * 构建 MCP 规范兼容的 tools/call 结果。
     * 格式: { content: [{ type: "text", text: "..." }], isError: false }
     */
    private Map<String, Object> buildToolCallResult(Callable<Object> executor) {
        List<Map<String, Object>> content = new ArrayList<>(1);
        Map<String, Object> textContent = new HashMap<>(2);
        textContent.put("type", "text");
        boolean isError = false;
        try {
            Object rawResult = executor.call();
            textContent.put("text", rawResult == null ? "" : rawResult.toString());
        } catch (Exception e) {
            log.error("Tool execution error: {}", e.getMessage(), e);
            textContent.put("text", e.getMessage() != null ? e.getMessage() : "Unknown error");
            isError = true;
        }
        content.add(textContent);
        Map<String, Object> result = new LinkedHashMap<>(2);
        result.put("content", content);
        result.put("isError", isError);
        return result;
    }

    /**
     * 处理 JSON-RPC 请求，根据方法类型和数据源配置返回对应的响应
     *
     * @param dataSource SSE 数据源信息，包含 MCP 配置和其他扩展信息
     * @param request JSON-RPC 请求消息，包含方法名、参数和 ID
     * @return JSON-RPC 响应消息，包含对应方法的执行结果或错误信息
     */
    private JsonRpcMessage processRequest(SseDataSourceInfo dataSource, JsonRpcMessage request) {
        JsonRpcMessage response = new JsonRpcMessage();
        JSONObject mcp = parseMcpConfig(dataSource);
        // 根据请求方法名分发到不同的处理逻辑
        switch (request.getMethod()) {
            case "initialize":
                // MCP 规范：必须返回 protocolVersion、capabilities、serverInfo
                Map<String, Object> initializeResult = new LinkedHashMap<>();
                initializeResult.put("protocolVersion", "2024-11-05");
                Map<String, Object> capabilities = new LinkedHashMap<>();
                Map<String, Object> toolsCap = new HashMap<>();
                toolsCap.put("listChanged", true);
                capabilities.put("tools", toolsCap);
                initializeResult.put("capabilities", capabilities);
                Map<String, Object> serverInfo = new LinkedHashMap<>();
                serverInfo.put("name", "fw-plus-magicapi-mcp");
                serverInfo.put("version", "0.1.1");
                initializeResult.put("serverInfo", serverInfo);
                // 用户可通过 other.mcp.initialize 覆盖或扩展默认值
                if (mcp.containsKey("initialize") && mcp.get("initialize") instanceof JSONObject) {
                    JSONObject userInit = mcp.getJSONObject("initialize");
                    if (userInit.containsKey("protocolVersion")) {
                        initializeResult.put("protocolVersion", userInit.getString("protocolVersion"));
                    }
                    if (userInit.containsKey("capabilities")) {
                        initializeResult.put("capabilities", userInit.get("capabilities"));
                    }
                    if (userInit.containsKey("serverInfo")) {
                        initializeResult.put("serverInfo", userInit.get("serverInfo"));
                    }
                    for (Map.Entry<String, Object> entry : userInit.entrySet()) {
                        String key = entry.getKey();
                        if (!"protocolVersion".equals(key) && !"capabilities".equals(key) && !"serverInfo".equals(key)) {
                            initializeResult.put(key, entry.getValue());
                        }
                    }
                }
                response.setResult(initializeResult);
                break;

            case "tools/list":
                // 根据分组获取 API 列表并构建工具列表
                String group = mcp.getString("tools/list/group");
                List<ApiInfo> apiInfos = new ArrayList<>();
                if (StringUtils.isNotBlank(group)) {
                    apiInfos = getApiInfosByGroup(group);
                }
                // 根据所有 API 信息动态构建工具列表
                List<Map<String, Object>> tools = buildTools(apiInfos);

                Map<String, Object> toolsResult = new HashMap<>();
                toolsResult.put("tools", tools);

                response.setResult(toolsResult);
                break;

            case "tools/call":
                if (request.getParams() == null) {
                    throw new McpException("Missing params in tools/call request", -32602);
                }
                String toolName = request.getParams().getString("name");
                if (StringUtils.isBlank(toolName)) {
                    throw new McpException("Missing tool name in tools/call request", -32602);
                }
                JSONObject arguments = request.getParams().getJSONObject("arguments");
                if (arguments == null) {
                    arguments = new JSONObject();
                }
                ApiInfo apiInfo = MagicConfiguration.getMagicResourceService().file(toolName);
                if (apiInfo == null) {
                    throw new McpException("Unknown tool: " + toolName, -32602);
                }
                String script = String.format("import '@%s:%s' as func ; return func();",apiInfo.getMethod().toLowerCase(),apiInfo.getPath());
                String scriptName = MagicConfiguration.getMagicResourceService().getScriptName(apiInfo);
                MagicScriptContext context = new MagicScriptContext();
                context.setScriptName(scriptName);
                String queryStr = apiInfo.getOptionValue(Options.WRAP_REQUEST_PARAMETERS);
                if (StringUtils.isNotBlank(queryStr)) {
                    context.set(queryStr, arguments);
                } else {
                    for (Map.Entry<String, Object> e : arguments.entrySet())    {
                        context.set(e.getKey(), e.getValue());
                    }
                }
                if ("post".equalsIgnoreCase(apiInfo.getMethod())) {
                    context.set(VAR_NAME_REQUEST_BODY, arguments);
                }
                // MCP 规范：tools/call 结果必须为 { content: [...], isError: false }
                Map<String, Object> toolCallResult = buildToolCallResult(() ->
                        ScriptManager.executeScript(script, context));
                response.setResult(toolCallResult);
                break;

            case "notifications/initialized":
                // MCP 规范：客户端在 initialize 成功后必须发送此通知，服务器无需返回具体结果
                response.setResult(Collections.emptyMap());
                break;

            default:
                throw new McpException("Method not found: " + request.getMethod(), -32601);
        }
        return response;
    }


    /**
     * 处理 MCP (Model Context Protocol) SSE 请求，通过异步方式处理并返回响应
     *
     * @param sessionId 会话 ID，格式为 clientId@appId，用于标识 SSE 连接
     * @param message JSON-RPC 消息对象，包含请求的方法、参数和 ID
     * @return ResponseEntity<Void> 响应实体，202 Accepted 表示请求已接受处理
     */
    public ResponseEntity<Void> message(String sessionId, BaseDynamicSseHandler.JsonRpcMessage message) {
        int atIndex = sessionId.lastIndexOf('@');
        String clientId = atIndex > 0 ? sessionId.substring(0, atIndex) : sessionId;
        List<SessionInfo> sessionList = clientIdSessionListMap.asMap().get(clientId);
        if (CollectionUtils.isEmpty(sessionList)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        sessionList = sessionList.stream()
                .filter(e -> sessionId.equals(e.getSessionId()) && Objects.nonNull(e.getSseEmitter()))
                .collect(Collectors.toList());
        if (sessionList.size() != 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String appid = sessionList.get(0).getAppId();
        SseEmitter emitter = sessionList.get(0).getSseEmitter();
        List<SseDataSourceInfo> dataSourceList = ListUtils.emptyIfNull(magicDataSourceStorage.findAll()).stream()
                .filter(e -> e.getKey().equals(appid))
                .collect(Collectors.toList());
        if (dataSourceList.size() != 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        SseDataSourceInfo dataSource = dataSourceList.get(0);
        if (!Boolean.TRUE.equals(dataSource.isMcp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        CompletableFuture.runAsync(() -> {
            JsonRpcMessage responseMessage = null;
            boolean isNotification = message.getId() == null;
            try {
                responseMessage = processRequest(dataSource, message);
                if (!isNotification) {
                    responseMessage.setId(message.getId());
                }
            } catch (Exception e) {
                log.error("Error processing MCP request: {}", e.getMessage(), e);
                if (isNotification) {
                    return; // JSON-RPC 通知不需要响应
                }
                if (null == responseMessage) {
                    responseMessage = new JsonRpcMessage();
                    responseMessage.setId(message.getId());
                }
                int errorCode = e instanceof McpException ? ((McpException) e).getErrorCode() : -32603;
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("code", errorCode);
                errorData.put("message", e.getMessage());
                responseMessage.setError(new JSONObject(errorData));
            }
            if (!isNotification) {
                try {
                    emitter.send(SseEmitter.event().name("message").data(responseMessage));
                } catch (IOException e) {
                    log.error("Failed to send SSE message: {}", e.getMessage(), e);
                }
            }
        }, mcpExecutor);
        // 立即返回 202 Accepted，表示请求已接受处理，结果将通过 SSE 推送
        return ResponseEntity.accepted().build();
    }

    /**
     * 处理传输错误的回调方法
     * 当发生传输错误时，该方法会被调用以执行相应的错误处理逻辑
     *
     * @param sessionInfo 会话信息对象，包含当前会话的相关信息
     * @param exception   抛出的异常对象，表示发生的错误类型和详情
     */
    public void handleTransportError(SessionInfo sessionInfo, Throwable exception) {
        log.error("Transport error occurred - Client ID: {}, Session ID: {}", sessionInfo.getClientId(), sessionInfo.getSessionId(), exception);
        cleanSession(sessionInfo.getClientId(), sessionInfo.getSessionId(), exception);
    }


    /**
     * 清理指定客户端的会话信息
     *
     * @param clientId 客户端ID，用于标识需要清理会话的客户端
     * @param sessionId 会话ID，如果为空则清理该客户端的所有会话，否则只清理指定会话
     * @param exception 异常信息，如果不为空则使用该异常完成SSE连接，否则正常完成连接
     */
    private void cleanSession(String clientId, String sessionId, Throwable exception) {
        List<SessionInfo> sessionList = clientIdSessionListMap.asMap().get(clientId);
        if (sessionList == null || sessionList.isEmpty()) {
            clientIdSessionListMap.invalidate(clientId);
            return;
        }
        boolean removeAll = StringUtils.isBlank(sessionId);
        List<SessionInfo> toRemove = new ArrayList<>();
        // COWArrayList 迭代器是快照且不支持 remove()，需要先收集再删除
        for (SessionInfo session : sessionList) {
            if (removeAll || (session.getSessionId() != null && session.getSessionId().equals(sessionId))) {
                try {
                    if (exception != null) {
                        session.getSseEmitter().completeWithError(exception);
                    } else {
                        session.getSseEmitter().complete();
                    }
                } catch (Exception ignored) {
                }
                toRemove.add(session);
            }
        }
        sessionList.removeAll(toRemove);
        if (sessionList.isEmpty()) {
            clientIdSessionListMap.invalidate(clientId);
        }
    }

    /**
     * 添加会话信息到客户端会话列表映射中
     *
     * @param appId 应用程序ID
     * @param session 会话信息对象
     * @param clientId 客户端ID
     */
    private void addSession(String appId, SessionInfo session, String clientId) {
        try {
            List<SessionInfo> sessions = clientIdSessionListMap.get(clientId,
                    () -> new java.util.concurrent.CopyOnWriteArrayList<>());
            sessions.add(session);
        } catch (java.util.concurrent.ExecutionException e) {
            // Guava Cache loader (CopyOnWriteArrayList constructor) 不会抛检查异常，此分支不可达
            log.error("Unexpected error adding session for client: {}", clientId, e);
        }
        log.debug("Added session - Session ID: {}, Client ID: {}, App ID: {}",
                session.getSessionId(), clientId, appId);
    }


    /**
     * 向指定的会话发送消息
     *
     * @param session   会话信息对象，包含目标会话的详细信息
     * @param eventName 事件名称，用于标识消息类型；如果为空或空白则发送普通消息
     * @param message   要发送的消息内容
     * @throws Exception 当发送消息失败时抛出异常
     */
    private void sendMessage(SessionInfo session, String eventName,String message) throws Exception {
        if (StringUtils.isNotBlank(eventName)) {
            session.getSseEmitter().send(SseEmitter.event().name(eventName).data(message));
        } else{
            session.getSseEmitter().send(message);
        }
        session.setLastPingTime(System.currentTimeMillis());
    }

    /**
     * 验证会话是否有效
     *
     * @param session 待验证的会话信息对象
     * @return 如果会话有效返回true，否则返回false
     */
    private boolean isSessionValid(SessionInfo session) {
        if (session == null) {
            return false;
        }
        long timeoutMillis = getSessionCacheExpireMinutes() * 60L * 1000L;
        return (System.currentTimeMillis() - session.getLastPingTime()) <= timeoutMillis;
    }


    /**
     * 向指定客户端的所有会话发送消息，仅发送给与指定appId匹配的会话
     * 
     * @param appId 应用ID，用于过滤会话
     * @param clientId 客户端ID
     * @param message 要发送的消息内容
     */
    public void broadcastMessageToClient(String appId, String clientId, String eventName, String message) {
        if (appId == null) {
            log.warn("broadcastMessageToClient called with null appId, skipping");
            return;
        }
        if (!deliverToLocalClient(appId, clientId, eventName, message)) {
            broadcastMessageToClient(this.sseServerId, appId, clientId, eventName, message);
        }
    }


    /**
     * 延迟关闭会话连接
     *
     * @param session 会话信息对象，包含SSE发射器等会话相关数据
     * @param message 要发送给客户端的关闭消息
     * @param code 关闭码
     * @param delayMillis 延迟关闭的时间间隔，单位为毫秒
     */
    private void delayedClose(SessionInfo session, String message, int code, int delayMillis) {
        try {
            scheduledExecutor.schedule(() -> {
                try {
                    session.getSseEmitter().completeWithError(new CloseException(code, message));
                } catch (Exception e) {
                    log.error("Error closing session: {}", session.getSessionId(), e);
                }
            }, delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Error during delayed close operation for session: {}", session.getSessionId(), e);
            try {
                session.getSseEmitter().complete();
            } catch (Exception innerEx) {
                log.warn("Failed to complete SSE emitter after error in delayedClose: {}", session.getSessionId(), innerEx);
            }
        }
    }

    abstract void broadcastMessageToClient(String excludedServerId , String appId, String clientId, String eventName,String message) ;


    protected void broadcastMessageToAllClient(String appId, String eventName, String message) {
        if (appId == null) {
            log.warn("broadcastMessageToAllClient called with null appId, skipping");
            return;
        }
        deliverToLocalClients(appId, eventName, message);
        broadcastMessageToAllClient(sseServerId, appId, eventName, message);
    }


    abstract void broadcastMessageToAllClient(String excludedServerId , String appId, String eventName,String message) ;


    public void broadcastDistributedMessageToAllClient(String appId, String eventName,String message) {
        // 先向本地客户端广播消息
        broadcastMessageToAllClient(appId, eventName,message);
    }

    /**
     * 仅向本地匹配 appId 的客户端投递消息，不触发 Redis 广播。
     * 供 Redis 消息监听器使用，避免无限循环。
     */
    void deliverToLocalClients(String appId, String eventName, String message) {
        if (appId == null) {
            return;
        }
        for (Map.Entry<String, List<SessionInfo>> entry : clientIdSessionListMap.asMap().entrySet()) {
            for (SessionInfo session : entry.getValue()) {
                if (session == null || !appId.equals(session.getAppId()) || !isSessionValid(session)) {
                    continue;
                }
                try {
                    sendMessage(session, eventName, message);
                } catch (Exception e) {
                    log.error("Failed to send message to session: {}", session.getSessionId(), e);
                }
            }
        }
    }

    /**
     * 仅向本地指定 clientId 的客户端投递消息，不触发 Redis 广播。
     * 供 Redis 消息监听器使用，避免无限循环。
     *
     * @return true 表示消息已投递到至少一个会话，false 表示未找到匹配的客户端
     */
    boolean deliverToLocalClient(String appId, String clientId, String eventName, String message) {
        if (appId == null) {
            return false;
        }
        List<SessionInfo> sessionList = this.clientIdSessionListMap.getIfPresent(clientId);
        if (sessionList == null || sessionList.isEmpty()) {
            return false;
        }
        boolean delivered = false;
        List<SessionInfo> sessionsToRemove = new ArrayList<>(2);
        for (SessionInfo session : sessionList) {
            if (session == null || !appId.equals(session.getAppId())) {
                continue;
            }
            if (!isSessionValid(session)) {
                sessionsToRemove.add(session);
                continue;
            }
            try {
                sendMessage(session, eventName, message);
                delivered = true;
            } catch (Exception e) {
                log.error("Failed to send message to session: {}", session.getSessionId(), e);
                sessionsToRemove.add(session);
            }
        }
        for (SessionInfo session : sessionsToRemove) {
            sessionList.remove(session);
        }
        if (sessionList.isEmpty()) {
            this.clientIdSessionListMap.invalidate(clientId);
        }
        return delivered;
    }


    /**
     * 关闭异常类，用于表示关闭操作时发生的异常情况
     * 该异常类包含了错误码和错误信息，便于更精确地描述异常原因
     */
    @Data
    private class CloseException extends Exception implements Serializable  {
        private static final long serialVersionUID = 1L;

        // 异常错误码
        private final int code ;

        // 异常错误信息
        private final String message;

        /**
         * 构造一个CloseException实例
         * @param code 错误码，用于标识异常的具体类型
         * @param message 错误信息，描述异常的详细内容
         */
        private CloseException(int code, String message) {
            super(message);
            this.code = code;
            this.message = message;
        }
    }


    /**
     * MCP 协议异常，携带 JSON-RPC 错误码。
     */
    private static class McpException extends RuntimeException {
        private final int errorCode;

        McpException(String message, int errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        int getErrorCode() {
            return errorCode;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JsonRpcMessage {
        private String jsonrpc = "2.0";
        private String id; // 请求ID，通知类型可为null
        private String method;
        private JSONObject params;
        private Object result;
        private JSONObject error;
    }

    /**
     * 会话信息类，包含与会话相关的所有元数据
     */
    @Data
    private static class SessionInfo {
        /**
         * SSE发射器实例，用于向客户端推送服务器发送事件
         */
        private SseEmitter sseEmitter;

        /**
         * 会话ID，用于标识当前连接的唯一会话
         */
        private String sessionId;

        /**
         * 客户端ID
         */
        private String clientId;
        
        /**
         * 应用ID
         */
        private String appId;
        
        /**
         * 最后一次ping时间
         */
        private Long lastPingTime;
        
        /**
         * 会话创建时间
         */
        private Long createTime;
        
        public SessionInfo(SseEmitter sseEmitter,String sessionId,String appId,String clientId) {
            this.sseEmitter = sseEmitter;
            this.sessionId = sessionId;
            this.appId = appId;
            this.clientId = clientId;
            this.createTime = System.currentTimeMillis();
            this.lastPingTime = this.createTime;
        }
    }
}
