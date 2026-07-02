package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import io.jsonwebtoken.lang.Collections;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.CloseStatus;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.context.SessionContext;
import org.ssssssss.magicapi.core.model.*;
import org.ssssssss.magicapi.core.service.impl.RequestMagicDynamicRegistry;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.magicapi.modules.servlet.RequestModule;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.reflection.JavaReflection;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.ssssssss.magicapi.core.config.Constants.*;
import static org.ssssssss.magicapi.core.config.Constants.VAR_NAME_SESSION;

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
                //.weakKeys()
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
                                    delayedClose(session, "SERVICE OVERLOAD", CloseStatus.SERVICE_OVERLOAD, getDelayMillis());
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
            // 清理所有会话相关映射
            clientIdSessionListMap.invalidateAll();
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
        // 创建副本避免并发修改异常
        Map<String, List<SessionInfo>> snapshot = clientIdSessionListMap.asMap();
        Map<String,String> clientSessionIdMap = new HashMap<>();
        // 遍历所有客户端的会话列表，查找并清理匹配应用ID的会话
        for (Map.Entry<String, List<SessionInfo>> e : snapshot.entrySet()) {
            for (SessionInfo session : e.getValue()) {
                if(appId.equals(session.getAppId())) {
                    clientSessionIdMap.put(session.getClientId(), session.getSessionId());
                }
            }
        }
        // 执行会话清理操作
        if (clientSessionIdMap.size() > 0) {
            for (Map.Entry<String, String> entry : clientSessionIdMap.entrySet()) {
                String clientId = entry.getKey();
                String sessionId = entry.getValue();
                cleanSession(clientId, sessionId,null);
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
     * 初始化方法，根据应用ID、会话ID和客户端ID执行相关的订阅脚本
     * @param appId 应用ID，用于匹配订阅信息
     * @param sessionId 会话ID，作为脚本执行的上下文参数
     * @param clientId 客户端ID，作为脚本执行的上下文参数
     */
    private void init(String appId, String sessionId, String clientId) {
        // 获取初始化的业务类型
        String businessType = getInitBussinessType();

        // 查找符合条件的订阅信息列表（启用状态、匹配的应用ID和业务ID）
        List<SseSubscribeInfo> subscribeInfoList = ListUtils.emptyIfNull(magicSubscribeStorage.findAll()).stream()
                .filter(subscribeInfo -> Boolean.TRUE.equals(subscribeInfo.isEnabled()) && subscribeInfo.getKey().equals(appId)
                        && businessType.equals(subscribeInfo.getBusinessId())).collect(Collectors.toList());

        // 遍历订阅信息列表，执行相应的脚本
        for (SseSubscribeInfo subscribeInfo : subscribeInfoList) {
            // 创建脚本执行上下文
            MagicScriptContext magicScriptContext = new MagicScriptContext();
            try {
                magicScriptContext.setScriptName(subscribeInfo.getName());
                // 设置脚本变量
                magicScriptContext.set("clientId", clientId);
                magicScriptContext.set("appId", appId);
                magicScriptContext.set("type", subscribeInfo.getBusinessId());
                magicScriptContext.set("sessionId", sessionId);
                // 执行脚本并记录结果
                Object result = ScriptManager.executeScript(subscribeInfo.getScript(), magicScriptContext);
                log.info("Result of init sessionId:{} ,appId:{} script: {}", sessionId, appId, result);
            } catch (Exception e) {
                // 记录脚本执行错误
                log.error("Error processing subscribe message: {}", e.getMessage(), e);
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
    public SseEmitter connect(String appId,String secret, String clientId) {
        try {
            // 校验必要参数是否存在
            if (StringUtils.isBlank(appId) || StringUtils.isBlank(secret) || StringUtils.isBlank(clientId)) {
                log.error("Missing required parameters - appId: {}, secret: {}, Client ID: {}", appId, secret, clientId);
                throw new IllegalArgumentException("Missing required parameters: appId and clientId");
            }

            // 判断提取出的 appId 是否在数据源中存在
            List<SseDataSourceInfo> dataSourceList = ListUtils.emptyIfNull(magicDataSourceStorage.findAll()).stream().filter(e->appId.equals(e.getKey()))
                    .collect(Collectors.toList());
            //boolean isAppIdExist = ListUtils.emptyIfNull(magicDataSourceStorage.findAll()).stream()
            //        .anyMatch(e -> e.getKey().equals(extractedAppId));
            if (CollectionUtils.isEmpty(dataSourceList)) {
                log.error("AppId not exist: {}", appId);
                throw new IllegalArgumentException("AppId not exist");
            }
            if (dataSourceList.size() != 1) {
                log.error("AppId is not unique: {}", appId);
                throw new IllegalArgumentException("AppId is not unique");
            }
            SseDataSourceInfo dataSourceInfo = dataSourceList.get(0);

            String configSecret = SpringUtils.getAppConfig(String.format("magic.api.sse.%s.secret", dataSourceInfo.getSecret()));
            if (!configSecret.equals(secret)) {
                log.error("Invalid secret for appId: {}", appId);
                throw new IllegalArgumentException("Invalid secret");
            }

            // 创建 SSE Emitter 实例，并设置超时时间
            SseEmitter emitter = new SseEmitter(getTimeOut());

            // 生成唯一连接 ID
            String connectionId = clientId + "@" + UUID.randomUUID().toString();

            // 构造客户端 SSE 会话信息
            SessionInfo clientSseInfo = new SessionInfo(emitter, connectionId, appId, clientId);

            // 添加会话信息并执行初始化脚本
            addSession(appId, clientSseInfo, clientId);
            init(appId, connectionId, clientId);

            // 注册连接完成回调：记录日志
            emitter.onCompletion(() -> {
                log.info("SSE connection completed - Connection ID: {}, Client ID: {}", connectionId, clientId);
                end(appId, connectionId, clientId);
                // 再清理会话资源
                cleanSession(clientId,connectionId,null);
            });

            // 注册连接错误回调：记录异常并处理传输错误
            emitter.onError(throwable -> {
                log.error("SSE connection error - Connection ID: {}, Client ID: {}", connectionId, clientId, throwable);
                handleTransportError(clientSseInfo, throwable);
            });

            // 注册连接超时回调：延迟关闭连接并记录日志
            emitter.onTimeout(() -> {
                log.info("SSE connection timed out - Connection ID: {}, Client ID: {}", connectionId, clientId);
                delayedClose(clientSseInfo, "Connection timed out", CloseStatus.SERVER_ERROR,getDelayMillis());
            });

            log.info("SSE connection established - Connection ID: {}, Client ID: {}, App ID: {}",
                    connectionId, clientId, appId);

            if (dataSourceInfo.isMcp()) {
                sendMcpEndpointEvent(emitter, dataSourceInfo, connectionId, clientId);
            }
            return emitter;
        } catch (Exception e) {
            log.error("Error during SSE connection establishment - Client ID: {}", clientId, e);
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new RuntimeException("Error during SSE connection establishment", e);
        }
    }

    /**
     * 发送 MCP 端点事件到客户端。
     * 解析数据源中的 MCP 配置，构造端点路径信息，并通过 SSE 发送给客户端。
     *
     * @param emitter SSE 发射器
     * @param dataSourceInfo SSE 数据源配置信息
     * @param connectionId 连接唯一标识符
     * @param clientId 客户端唯一标识符
     */
    private void sendMcpEndpointEvent(SseEmitter emitter, SseDataSourceInfo dataSourceInfo,
                                      String connectionId, String clientId) {
        try {
            String otherStr = SpringUtils.getAppConfig(String.format("magic.api.sse.%s.other", dataSourceInfo.getOther()));
            // 解析数据源中的 MCP 配置信息
            JSONObject mcp = new JSONObject();
            if (StringUtils.isNotBlank(otherStr)) {
                JSONObject other = JSON.parseObject(otherStr);
                if (other.containsKey("mcp") && other.get("mcp") instanceof JSONObject) {
                    mcp = other.getJSONObject("mcp");
                }
            }
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
            MagicAPIProperties magicAPIProperties = SpringUtils.getBean(MagicAPIProperties.class);
            String webBasePath = magicAPIProperties != null && magicAPIProperties.getWeb() != null ? magicAPIProperties.getWeb() : "";
            if (!webBasePath.startsWith("/")) {
                webBasePath = "/" + webBasePath;
            }
            if (webBasePath.endsWith("/") && webBasePath.length() > 0) {
                webBasePath = webBasePath.substring(0, webBasePath.length() - 1);
            }
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
                return "float";
            case Boolean:
                return "boolean";
            case Array:
                return "array";
            case Date:
                return "date";
            case MultipartFile:
            case MultipartFiles:
                return "file";
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
    private Map<String,Object> buildProperties(List<BaseDefinition> parameters) {
        // 如果参数列表为空，直接返回空映射
        if (CollectionUtils.isEmpty(parameters)) {
            return new HashMap<>();
        }
        
        // 初始化指定容量的 HashMap，避免扩容开销
        Map<String, Object> properties = new HashMap<>(parameters.size());
        // 遍历所有参数，构建属性映射
        parameters.forEach(parameter -> {
            // 获取参数名称，如果为空则跳过该参数
            String paramName = parameter.getName();
            if (paramName == null) {
                return;
            }
            // 将参数名和对应的 Schema 放入结果映射
            properties.put(paramName, buildParamSchema(parameter));
        });
        return properties;
    }

    /**
     * 构建参数 Schema 映射
     *
     * @param parameter 基础定义对象，包含参数的完整信息
     * @return 参数 Schema 的 Map 结构，键为参数名，值为包含类型、名称、值、描述、是否必填、默认值及子属性等信息的 Map
     */
    private Map<String, Object> buildParamSchema(BaseDefinition parameter) {
        Map<String, Object> paramSchema = new HashMap<>();
        // 转换并设置数据类型
        paramSchema.put("type", convertDataType(parameter.getDataType()));
        // 设置参数名称
        paramSchema.put("name", parameter.getName());
        // 设置参数值
        paramSchema.put("value", parameter.getValue());
        // 设置参数描述，为空时返回空字符串
        paramSchema.put("description", StringUtils.defaultString(parameter.getDescription(), ""));
        // 设置是否必填标识
        paramSchema.put("required", parameter.isRequired());
        // 设置默认值
        paramSchema.put("defaultValue", parameter.getDefaultValue());
        // 如果存在子参数，递归构建子属性映射
        if (CollectionUtils.isNotEmpty(parameter.getChildren())) {
            paramSchema.put("properties", buildProperties(parameter.getChildren()));
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

            // 构建输入参数的对象 Schema
            Map<String, Object> inputSchema = new HashMap<>(2);
            inputSchema.put("type", "object");

            // 将参数转换为 BaseDefinition 类型并构建属性映射，过滤非 BaseDefinition 类型的元素
            Map<String, Object> inputProperties = buildProperties(
                    ListUtils.emptyIfNull(apiInfo.getParameters()).stream()
                    .filter(e -> e instanceof BaseDefinition)
                    .map(e -> (BaseDefinition) e)
                    .collect(Collectors.toList()));

            inputSchema.put("properties", inputProperties);
            tool.put("inputSchema", inputSchema);


            //apiInfo.getResponseBodyDefinition().
            tools.add(tool);
        }
        return tools;
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
        // 解析数据源中的 MCP 配置信息
        JSONObject mcp = new JSONObject();
        if (StringUtils.isNotBlank(dataSource.getOther())) {
            JSONObject other = JSON.parseObject(dataSource.getOther());
            if (other.containsKey("mcp") && other.get("mcp") instanceof JSONObject) {
                mcp = other.getJSONObject("mcp");
            }
        }
        // 根据请求方法名分发到不同的处理逻辑
        switch (request.getMethod()) {
            case "initialize":
                // 处理初始化请求，使用配置的初始化结果
                Map<String, Object> initializeResult = new HashMap<>();
                if (mcp.containsKey("initialize") && mcp.get("initialize") instanceof JSONObject) {
                    initializeResult.putAll(mcp.getJSONObject("initialize"));
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
                // 执行工具调用，解析参数并通过脚本引擎执行
                String toolName = request.getParams().getString("name");
                JSONObject arguments = request.getParams().getJSONObject("arguments");
                // 根据工具名称获取 API 信息
                ApiInfo apiInfo = MagicConfiguration.getMagicResourceService().file(toolName);
                if (apiInfo == null) {
                    // 抛出未知工具异常
                    throw new RuntimeException("Unknown tool: " + toolName);
                }
                // 构建脚本并设置执行上下文
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
                response.setResult(ScriptManager.executeScript(script, context));
                break;

            default:
                // 抛出方法未找到异常
                throw new RuntimeException("Method not found: " + request.getMethod());
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
    public ResponseEntity<Void> message(String sessionId ,BaseDynamicSseHandler.JsonRpcMessage message) {
        // 解析 sessionId 提取 clientId
        String[] params = sessionId.split("@");
        String clientId = params[0];
        // 获取客户端的会话列表
        List<SessionInfo> sessionList = clientIdSessionListMap.asMap().get(clientId);
        // 如果未找到会话，返回错误请求
        if (CollectionUtils.isEmpty(sessionList)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // 过滤出与指定 sessionId 匹配且有效的 SseEmitter
        sessionList = sessionList.stream()
                .filter(e -> sessionId.equals(e.getSessionId()) && Objects.nonNull(e.getSseEmitter())).collect(Collectors.toList());
        // 如果过滤后会话为空或存在多个匹配，返回错误请求
        if (CollectionUtils.isEmpty(sessionList) || sessionList.size() > 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String appid = sessionList.get(0).getAppId();
        SseEmitter emitter = sessionList.get(0).getSseEmitter();
        // 根据 appId 过滤数据源列表
        List<SseDataSourceInfo> dataSourceList = ListUtils.emptyIfNull(magicDataSourceStorage.findAll()).stream().filter(e->e.getKey().equals(appid))
                .collect(Collectors.toList());
        // 重复检查会话列表有效性
        if (CollectionUtils.isEmpty(sessionList) || sessionList.size() > 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        SseDataSourceInfo dataSource = dataSourceList.get(0);
        // 检查数据源是否启用 MCP 功能
        if(!Boolean.TRUE.equals(dataSource.isMcp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // 异步处理业务逻辑，避免阻塞 Tomcat 线程
        CompletableFuture.runAsync(() -> {
            JsonRpcMessage responseMessage = null;
            try {
                // 执行请求处理逻辑
                responseMessage = processRequest(dataSource,message);
                responseMessage.setId(message.getId()); // 必须回传相同的 ID
            } catch (Exception e) {
                log.error("Error processing request: {}", e.getMessage(), e);
                if (null == responseMessage) {
                    responseMessage = new JsonRpcMessage();
                    responseMessage.setId(message.getId());
                }
                // 捕获业务异常，构造错误响应并通过 SSE 发送
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("code", -32000);
                errorData.put("message", e.getMessage());
                errorData.put("data", e.getClass().getSimpleName());
                responseMessage.setError(new JSONObject(errorData));
            } finally{
                // 通过 SSE 发送响应消息
                try {
                    emitter.send(SseEmitter.event().name("message").data(responseMessage));
                } catch (IOException e) {
                    log.error("Failed to send SSE message: {}", e.getMessage(), e);
                }
            }
        });
        // 立即返回 202 Accepted，表示请求已接受处理，结果将通过 SSE 推送
        return ResponseEntity.accepted().build();
    }

    /**
     * 结束WebSocket会话，执行订阅信息中的关闭脚本
     *
     * @param appId 应用ID
     * @param sessionId 会话ID
     * @param clientId 客户端ID
     */
    private void end(String appId,String sessionId,String clientId) {
        String businessType = getCloseBussinessType();
        // 查找启用的且与当前应用ID和业务类型匹配的订阅信息
        List<SseSubscribeInfo> subscribeInfoList = ListUtils.emptyIfNull(magicSubscribeStorage.findAll()).stream()
                .filter(subscribeInfo ->Boolean.TRUE.equals(subscribeInfo.isEnabled()) && subscribeInfo.getKey().equals(appId)
                        && businessType.equals(subscribeInfo.getBusinessId())).collect(Collectors.toList());
        // 为每个匹配的订阅信息执行相应的关闭脚本
        for (SseSubscribeInfo subscribeInfo : subscribeInfoList) {
            MagicScriptContext magicScriptContext = new MagicScriptContext();
            try {
                magicScriptContext.setScriptName(subscribeInfo.getName());
                magicScriptContext.set("clientId", clientId);
                magicScriptContext.set("appId", appId);
                magicScriptContext.set("type", subscribeInfo.getBusinessId());
                magicScriptContext.set("sessionId", sessionId);
                Object result = ScriptManager.executeScript(subscribeInfo.getScript(), magicScriptContext);
                log.info("Result of init sessionId:{} ,appId:{} script: {}", sessionId,appId,result);
            } catch (Exception e) {
                log.error("Error processing subscribe message: {}", e.getMessage(), e);
            }
        }
    }


    /**
     * 处理传输错误的回调方法
     * 当发生传输错误时，该方法会被调用以执行相应的错误处理逻辑
     *
     * @param sessionInfo 会话信息对象，包含当前会话的相关信息
     * @param exception   抛出的异常对象，表示发生的错误类型和详情
     */
    public void handleTransportError(SessionInfo sessionInfo, Throwable exception)  {
        log.error("Transport error occurred - Client ID: {}, Session ID: {}", sessionInfo.getClientId(), sessionInfo.getSessionId(), exception);
        // 延迟关闭连接，指定错误原因、关闭状态和延迟时间
        delayedClose(sessionInfo, "Error occurred", CloseStatus.PROTOCOL_ERROR, getDelayMillis());
    }


    /**
     * 清理指定客户端的会话信息
     *
     * @param clientId 客户端ID，用于标识需要清理会话的客户端
     * @param sessionId 会话ID，如果为空则清理该客户端的所有会话，否则只清理指定会话
     * @param exception 异常信息，如果不为空则使用该异常完成SSE连接，否则正常完成连接
     */
    private void cleanSession(String clientId, String sessionId,Throwable exception) {
        List<SessionInfo> sessionList = clientIdSessionListMap.asMap().get(clientId);
        // 检查客户端会话列表是否存在，不存在则直接清理缓存
        if (sessionList == null || sessionList.isEmpty()) {
            clientIdSessionListMap.invalidate(clientId);
            return;
        }
        // 判断是否需要删除所有会话
        boolean removeAll = StringUtils.isBlank(sessionId);
        Iterator<SessionInfo> iterator = sessionList.iterator();

        // 遍历并清理符合条件的会话
        while (iterator.hasNext()) {
            SessionInfo session = iterator.next();
            if (removeAll || (session.getSessionId() != null && session.getSessionId().equals(sessionId))) {
                try {
                    if (exception!= null) {
                        session.getSseEmitter().completeWithError(exception);
                    } else {
                        session.getSseEmitter().complete();
                    }
                } catch (Exception ignored) {
                    // 忽略SSE关闭过程中的异常，确保会话能被正常移除
                }
                iterator.remove();
            }
        }

        // 更新缓存
        if (sessionList.isEmpty()) {
            clientIdSessionListMap.invalidate(clientId);
        } else if (!removeAll) {
            // 如果只是删除部分会话，需要重新 put 回去保持引用一致性
            clientIdSessionListMap.put(clientId, sessionList);
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
        String sessionId = session.getSessionId();
        // 添加客户端ID与会话列表的映射
        try {
            List<SessionInfo> sessions = clientIdSessionListMap.get(clientId,
                () -> new java.util.concurrent.CopyOnWriteArrayList<>());
            sessions.add(session);
        } catch (java.util.concurrent.ExecutionException e) {
            log.error("Error adding session for client: {}", clientId, e);
            // Fallback: 直接操作
            List<SessionInfo> sessions = clientIdSessionListMap.getIfPresent(clientId);
            if (sessions == null) {
                sessions = new java.util.concurrent.CopyOnWriteArrayList<>();
                clientIdSessionListMap.put(clientId, sessions);
            }
            sessions.add(session);
        }

        log.debug("Added session - Session ID: {}, Client ID: {}, App ID: {}",
                 sessionId, clientId, appId);
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
        // 检查会话是否仍然有效
        if (!isSessionValid(session)) {
            log.warn("Attempted to send message to invalid session: {}", session.getSessionId());
            return;
        }
        // 根据事件名称是否存在决定发送带事件名的消息或普通消息
        if (StringUtils.isNotBlank(eventName)) {
            session.getSseEmitter().send(SseEmitter.event().name(eventName).data(message));
        } else{
            session.getSseEmitter().send(message);
        }
        
    }

    /**
     * 验证会话是否有效
     *
     * @param session 待验证的会话信息对象
     * @return 如果会话有效返回true，否则返回false
     */
    private boolean isSessionValid(SessionInfo session) {
        // 检查会话是否打开以及是否仍在缓存中（未过期）
        if (session == null ) {
            return false;
        }
        // 检查会话信息是否存在
        SessionInfo sessionInfo = session;
        // 检查会话是否超时（即使在缓存中）
        long currentTime = System.currentTimeMillis();
        long lastPingTime = sessionInfo.getLastPingTime();
        long timeoutMillis = getSessionCacheExpireMinutes() * 60 * 1000;
        return (currentTime - lastPingTime) <= timeoutMillis;
    }


    /**
     * 向指定客户端的所有会话发送消息，仅发送给与指定appId匹配的会话
     * 
     * @param appId 应用ID，用于过滤会话
     * @param clientId 客户端ID
     * @param message 要发送的消息内容
     */
    public void broadcastMessageToClient(String appId, String clientId, String eventName,String message) {
        List<SessionInfo> sessionList = this.clientIdSessionListMap.getIfPresent(clientId);
        if (!Collections.isEmpty(sessionList)) {
            // 创建需要移除的会话列表
            List<SessionInfo> sessionsToRemove = new java.util.ArrayList<>();
            // 创建需要发送消息的会话列表
            List<SessionInfo> sessionsToSend = new java.util.ArrayList<>();
            // 筛选出与指定appId匹配的会话
            for (SessionInfo session : sessionList) {
                SessionInfo sessionInfo = session ;
                if (sessionInfo != null && appId.equals(sessionInfo.getAppId())) {
                    if (isSessionValid(session)) {
                        sessionsToSend.add(session);
                    } else {
                        // 如果会话无效，记录需要移除的会话
                        sessionsToRemove.add(session);
                    }
                }
            }
            // 向筛选出的会话发送消息
            for (SessionInfo session : sessionsToSend) {
                try {
                    sendMessage(session, eventName,message);
                } catch (Exception e) {
                    log.error("Failed to send message to session: {}", session.getSessionId(), e);
                    // 如果发送失败，记录需要移除的会话
                    sessionsToRemove.add(session);
                }
            }

            // 移除无效或发送失败的会话
            for (SessionInfo session : sessionsToRemove) {
                sessionList.remove(session);
            }

            // 如果该clientId没有有效会话了，则从map中移除
            if (sessionList.isEmpty()) {
                this.clientIdSessionListMap.invalidate(clientId);
            }
        } else {
            log.warn("No active sessions found for client: {}", clientId);
            broadcastMessageToClient(this.sseServerId,clientId, appId, message);
        }
    }


    /**
     * 延迟关闭会话连接
     *
     * @param session 会话信息对象，包含SSE发射器等会话相关数据
     * @param message 要发送给客户端的关闭消息
     * @param closeStatus 关闭状态信息，包含关闭码和原因
     * @param delayMillis 延迟关闭的时间间隔，单位为毫秒
     */
    private void delayedClose(SessionInfo session, String message, CloseStatus closeStatus, int delayMillis) {
        try {
            // 创建延迟任务来关闭连接
            java.util.concurrent.ScheduledExecutorService delayedCloser =
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
            delayedCloser.schedule(() -> {
                try {
                    session.getSseEmitter().completeWithError(new CloseException(closeStatus.getCode(),message));
                } catch (Exception e) {
                    log.error("Error closing session: {}", session.getSessionId(), e);
                } finally {
                    // 清理资源
                    delayedCloser.shutdown();
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
    

    protected void broadcastMessageToAllClient(String appId, String eventName,String message) {
        // 遍历所有客户端
        for (Map.Entry<String, List<SessionInfo>> entry : clientIdSessionListMap.asMap().entrySet()) {
            for (SessionInfo session : entry.getValue()) {
                try {
                    SessionInfo sessionInfo = session ;
                    if (!isSessionValid(session) || sessionInfo == null || !appId.equals(sessionInfo.getAppId())) {
                        log.warn("Attempted to send message to invalid session: {}", session.getSessionId());
                        continue;
                    }
                    sendMessage(session, eventName,message);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        broadcastMessageToAllClient(sseServerId, appId, message);
    }


    abstract void broadcastMessageToAllClient(String excludedServerId , String appId, String eventName,String message) ;
    

    public void broadcastDistributedMessageToAllClient(String appId, String eventName,String message) {
        // 先向本地客户端广播消息
        broadcastMessageToAllClient(appId, eventName,message);
    }


    /**
     * 业务JSON Bean类，用于封装业务相关的JSON数据
     * 继承自JsonBean，添加了业务类型字段
     */
    @Data
    public static class BusinessJsonBean extends JsonBean<Object> {
        /**
         * 业务类型
         */
        private String type;
        
        /**
         * BusinessJsonBean构造函数
         */
        public BusinessJsonBean() {
            super();
        }
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
