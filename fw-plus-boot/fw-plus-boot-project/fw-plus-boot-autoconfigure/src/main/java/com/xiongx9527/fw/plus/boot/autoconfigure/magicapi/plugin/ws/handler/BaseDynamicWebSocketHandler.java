package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsInvocationContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSubscribeScript;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * magic-api 侧的 WebSocket 业务调度实现。
 *
 * <p>连接、鉴权、会话、心跳与广播等通用职责已由本模块
 * {@code com.xiongx9527.fw.plus.boot.autoconfigure.web.ws} 包中的
 * {@code AbstractWebSocketHandler} 承担；本类只提供 magic 语义：</p>
 * <ul>
 *     <li>appId 是否已登记 → 取 magic 的 ws 数据源资源存储；</li>
 *     <li>某 appId + 业务类型下启用中的订阅 → 取 magic 的 ws 订阅资源存储；</li>
 *     <li>执行订阅 → 用 magic-script（{@link ScriptManager}）执行订阅脚本。</li>
 * </ul>
 *
 * <p>本类以 {@link WsBusinessDispatcher} Bean 的形式被地基的处理器自动注入，故不再创建
 * WebSocket handler 实例。</p>
 *
 * @author zzy
 */
@Slf4j
public class BaseDynamicWebSocketHandler implements WsBusinessDispatcher, BaseService {

    /**
     * WebSocket数据源信息存储
     */
    private final WsDataSourceInfoMagicResourceStorage magicDataSourceStorage;

    /**
     * WebSocket订阅信息存储
     */
    private final WsSubscribeInfoMagicResourceStorage magicSubscribeStorage;

    /**
     * WebSocket处理器构造函数
     *
     * @param magicDataSourceStorage WebSocket数据源信息存储
     * @param magicSubscribeStorage  WebSocket订阅信息存储
     */
    public BaseDynamicWebSocketHandler(WsDataSourceInfoMagicResourceStorage magicDataSourceStorage,
                                       WsSubscribeInfoMagicResourceStorage magicSubscribeStorage) {
        this.magicDataSourceStorage = magicDataSourceStorage;
        this.magicSubscribeStorage = magicSubscribeStorage;
    }

    /**
     * appId 是否已在 magic 的 ws 数据源资源存储中登记。
     *
     * @param appId 应用 ID
     * @return true 表示允许建立连接
     */
    @Override
    public boolean existsApp(String appId) {
        return ListUtils.emptyIfNull(magicDataSourceStorage.findAll()).stream()
                .anyMatch(e -> e.getKey().equals(appId));
    }

    /**
     * 查找启用且匹配指定 appId 和业务类型的订阅信息列表。
     *
     * @param appId        应用 ID
     * @param businessType 业务类型
     * @return 订阅列表
     */
    @Override
    public List<? extends WsSubscribeScript> findEnabledSubscribes(String appId, String businessType) {
        return ListUtils.emptyIfNull(magicSubscribeStorage.findAll()).stream()
                .filter(s -> Boolean.TRUE.equals(s.isEnabled())
                        && s.getKey().equals(appId)
                        && businessType.equals(s.getBusinessId()))
                .collect(Collectors.toList());
    }

    /**
     * 用 magic-script 执行订阅脚本。
     *
     * <p>脚本上下文的变量与拆分前保持一致：初始化/关闭阶段暴露订阅自身的业务类型；
     * 消息阶段额外暴露 `subscribeInfo` 会话属性与 `params` 消息体。</p>
     *
     * @param subscribe 订阅描述
     * @param context   调用上下文
     * @return 脚本执行结果
     */
    @Override
    public Object execute(WsSubscribeScript subscribe, WsInvocationContext context) {
        if (!(subscribe instanceof WsSubscribeInfo)) {
            throw new IllegalArgumentException("Unsupported subscribe type: " + subscribe);
        }
        WsSubscribeInfo subscribeInfo = (WsSubscribeInfo) subscribe;
        MagicScriptContext magicScriptContext = new MagicScriptContext();
        magicScriptContext.setScriptName(subscribeInfo.getName());
        magicScriptContext.set("appId", context.getAppId());
        magicScriptContext.set("clientId", context.getClientId());
        magicScriptContext.set("sessionId", context.getSessionId());
        if (context.getPhase() == WsInvocationContext.Phase.MESSAGE) {
            if (context.getSession() != null) {
                context.getSession().getAttributes().put("subscribeInfo", subscribeInfo);
            }
            putBusinessMessageContext(magicScriptContext, context.getBusinessType(),
                    resolveParams(context));
        } else {
            putBusinessType(magicScriptContext, subscribeInfo.getBusinessId());
        }
        Object result = ScriptManager.executeScript(subscribeInfo.getScript(), magicScriptContext);
        log.info("Result of {} sessionId:{} ,appId:{} script: {}",
                context.getBusinessType(), context.getSessionId(), context.getAppId(), result);
        return result;
    }

    /**
     * 暴露业务类型变量。
     *
     * @param magicScriptContext 脚本上下文
     * @param businessType       业务类型
     */
    void putBusinessType(MagicScriptContext magicScriptContext, String businessType) {
        magicScriptContext.set("type", businessType);
        magicScriptContext.set("businessType", businessType);
    }

    /**
     * 取得脚本可见的消息体：二进制帧没有 JSON 参数，这里把原始内容以 Base64 暴露为
     * {@code params.data}，并附带 {@code params.length}，使订阅脚本能取到同一份数据。
     *
     * @param context 调用上下文
     * @return 脚本消息体
     */
    private static JSONObject resolveParams(WsInvocationContext context) {
        JSONObject params = context.getParams();
        if (params != null || context.getMessageKind() != WsInvocationContext.MessageKind.BINARY) {
            return params;
        }
        byte[] body = context.getBinaryBody();
        JSONObject binaryParams = new JSONObject();
        binaryParams.put("data", body == null ? null : java.util.Base64.getEncoder().encodeToString(body));
        binaryParams.put("length", body == null ? 0 : body.length);
        return binaryParams;
    }

    /**
     * 暴露业务消息相关变量。
     *
     * @param magicScriptContext 脚本上下文
     * @param businessType       业务类型
     * @param params             消息体
     */
    void putBusinessMessageContext(MagicScriptContext magicScriptContext, String businessType, JSONObject params) {
        putBusinessType(magicScriptContext, businessType);
        magicScriptContext.set("params", params);
    }
}
