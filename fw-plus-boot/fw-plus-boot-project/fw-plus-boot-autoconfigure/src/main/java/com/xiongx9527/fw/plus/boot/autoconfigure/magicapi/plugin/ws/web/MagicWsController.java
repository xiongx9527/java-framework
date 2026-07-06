package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.web;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.BaseDynamicWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.Map;
import java.util.Objects;


/**
 * @author zzy
 */
@Slf4j
public class MagicWsController extends MagicController implements MagicExceptionHandler , BaseService {

    private final WsSubscribeInfoMagicResourceStorage wsSubscribeInfoMagicResourceStorage;

    private final WsDataSourceInfoMagicResourceStorage wsDataSourceInfoMagicResourceStorage;

    private final BaseDynamicWebSocketHandler baseDynamicWebSocketHandler;

    public MagicWsController(MagicConfiguration configuration) {
        this(configuration, null, null, null);
    }

    public MagicWsController(MagicConfiguration configuration,
                             WsSubscribeInfoMagicResourceStorage wsSubscribeInfoMagicResourceStorage,
                             WsDataSourceInfoMagicResourceStorage wsDataSourceInfoMagicResourceStorage,
                             BaseDynamicWebSocketHandler baseDynamicWebSocketHandler) {
        super(configuration);
        this.wsSubscribeInfoMagicResourceStorage = wsSubscribeInfoMagicResourceStorage;
        this.wsDataSourceInfoMagicResourceStorage = wsDataSourceInfoMagicResourceStorage;
        this.baseDynamicWebSocketHandler = baseDynamicWebSocketHandler;
    }

    /**
     * 执行Web Service操作
     * @param id 服务ID
     * @param request HTTP请求对象
     * @return 执行结果的JsonBean对象
     */
    @PostMapping("/ws/execute")
    @ResponseBody
    public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
        try {
            validateRequiredField(id, "id");
            WsSubscribeInfo subscribeInfo = findSubscribeInfo(id);
            if (subscribeInfo == null) {
                return failure("ws-subscribe脚本不存在: " + id);
            }
            MagicScriptContext context = buildTestContext(subscribeInfo, null);
            Object result = ScriptManager.executeScript(subscribeInfo.getScript(), context);
            return new JsonBean<>(result);
        } catch (Exception e) {
            log.error("Failed to execute websocket subscribe script: {}", id, e);
            return failure(e.getMessage());
        }
    }

    /**
     * 测试数据源连接
     * @param properties 数据源配置信息
     * @return 测试结果的JsonBean对象
     */
    @RequestMapping("/ws/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody WsDataSourceInfo properties) {
        try {
            validateDataSourceInfo(properties);
            getSecretKeyAndSignatureAlgorithm();
            BaseDynamicWebSocketHandler handler = getBaseDynamicWebSocketHandler();
            if (handler != null) {
                new WsModule(handler, properties.getKey()).generateToken(properties.getSecret());
            }
            return new JsonBean<>("ok");
        } catch (Exception e) {
            log.error("Failed to test websocket datasource", e);
            return failure(e.getMessage());
        }
    }

    MagicScriptContext buildTestContext(WsSubscribeInfo subscribeInfo, Map<String, Object> params) {
        MagicScriptContext magicScriptContext = new MagicScriptContext();
        magicScriptContext.setScriptName(subscribeInfo.getName());
        magicScriptContext.set("appId", subscribeInfo.getKey());
        magicScriptContext.set("clientId", "test-client");
        magicScriptContext.set("sessionId", "test-session");
        magicScriptContext.set("type", subscribeInfo.getBusinessId());
        magicScriptContext.set("businessType", subscribeInfo.getBusinessId());
        magicScriptContext.set("params", params == null ? new JSONObject() : params);
        return magicScriptContext;
    }

    private WsSubscribeInfo findSubscribeInfo(String id) {
        WsSubscribeInfoMagicResourceStorage storage = getWsSubscribeInfoMagicResourceStorage();
        if (storage == null) {
            return null;
        }
        return ListUtils.emptyIfNull(storage.findAll()).stream()
                .filter(item -> Objects.equals(item.getId(), id))
                .findFirst()
                .orElse(null);
    }

    private void validateDataSourceInfo(WsDataSourceInfo properties) {
        if (properties == null) {
            throw new IllegalArgumentException("数据源不能为空");
        }
        WsDataSourceInfoMagicResourceStorage storage = getWsDataSourceInfoMagicResourceStorage();
        if (storage != null) {
            storage.validate(properties);
            return;
        }
        validateRequiredField(properties.getKey(), "key");
        validateRequiredField(properties.getName(), "name");
        validateRequiredField(properties.getSecret(), "secret");
        validateJSON(properties.getOther(), "other");
    }

    private JsonBean<Object> failure(String message) {
        return new JsonBean<>(-1, StringUtils.defaultIfBlank(message, "操作失败"), null);
    }

    private WsSubscribeInfoMagicResourceStorage getWsSubscribeInfoMagicResourceStorage() {
        if (wsSubscribeInfoMagicResourceStorage != null) {
            return wsSubscribeInfoMagicResourceStorage;
        }
        try {
            return SpringUtils.getBean(WsSubscribeInfoMagicResourceStorage.class);
        } catch (Exception e) {
            return null;
        }
    }

    private WsDataSourceInfoMagicResourceStorage getWsDataSourceInfoMagicResourceStorage() {
        if (wsDataSourceInfoMagicResourceStorage != null) {
            return wsDataSourceInfoMagicResourceStorage;
        }
        try {
            return SpringUtils.getBean(WsDataSourceInfoMagicResourceStorage.class);
        } catch (Exception e) {
            return null;
        }
    }

    private BaseDynamicWebSocketHandler getBaseDynamicWebSocketHandler() {
        if (baseDynamicWebSocketHandler != null) {
            return baseDynamicWebSocketHandler;
        }
        try {
            return SpringUtils.getBean(BaseDynamicWebSocketHandler.class);
        } catch (Exception e) {
            return null;
        }
    }

}
