
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MagicDynamicMqttClientDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.service.MqttClientDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.*;
import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import org.apache.commons.collections4.ListUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.logging.MagicLoggerContext;
import org.ssssssss.magicapi.core.model.DebugRequest;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptDebugContext;

import java.util.List;

@Slf4j
public class MagicMqttClientController extends MagicController implements MagicExceptionHandler {

    private static final String MQTT_CLIENT_TEST_TOPIC = "magic-api-test-topic";

    public MagicMqttClientController(MagicConfiguration configuration) {
        super(configuration);
    }

    @RequestMapping("/mqttclient/availableStorages")
    @ResponseBody
    public JsonBean<Object> availableStorages() {
        MqttClientDataSourceInfoMagicResourceStorage storage = SpringUtils.getBean(MqttClientDataSourceInfoMagicResourceStorage.class);
        List<MqttClientDataSourceInfo> all = ListUtils.emptyIfNull(storage.findAll());
        return new JsonBean<>(all);
    }

    @PostMapping("/mqttclient/execute")
    @ResponseBody
    public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
        MqttClientSubscribeInfo entity = MagicConfiguration.getMagicResourceService().file(id);
        notNull(entity, FILE_NOT_FOUND);
        String script = entity.getScript();
        if (script == null || script.isEmpty()) {
            throw new IllegalArgumentException("Script content is empty or null.");
        }
        DebugRequest debugRequest = DebugRequest.create(request);
        String clientId = debugRequest.getRequestedClientId();
        String scriptId = debugRequest.getRequestedScriptId();

        if (clientId == null || scriptId == null) {
            throw new IllegalArgumentException("Client ID or Script ID is missing in the debug request.");
        }
        String sessionAndScriptId = clientId + scriptId;
        MagicLoggerContext.SESSION.set(clientId);
        MagicScriptDebugContext magicScriptContext = null;
        try {
            magicScriptContext = debugRequest.createMagicScriptContext(configuration.getDebugTimeout());
            org.ssssssss.magicapi.core.config.WebSocketSessionManager.addMagicScriptContext(sessionAndScriptId, magicScriptContext);
            magicScriptContext.setScriptName(MagicConfiguration.getMagicResourceService().getScriptName(entity));
            String qosStr = MagicAPIConfigUtils.getMqttClientSubscribeConfig(entity.getKey(), entity.getTopic(), "qos", entity.getQos());
            MqttQoS qos = MqttQoS.QOS0;
            try {
                int qosInt = Integer.parseInt(qosStr);
                if (qosInt == 1) qos = MqttQoS.QOS1;
                else if (qosInt == 2) qos = MqttQoS.QOS2;
            } catch (Exception ignored) {}
            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, qos, false, 0);
            MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(entity.getTopic(), 0);
            byte[] payload = entity.getParams() != null ? entity.getParams().getBytes() : new byte[0];
            MqttPublishMessage msg = new MqttPublishMessage(fixedHeader, variableHeader, payload);
            magicScriptContext.set("msg", msg);
            Object result = ScriptManager.executeScript(script, magicScriptContext);
            return new JsonBean<>(result);
        } finally {
            if (magicScriptContext != null) {
                org.ssssssss.magicapi.core.config.WebSocketSessionManager.removeMagicScriptContext(sessionAndScriptId);
            }
            MagicLoggerContext.SESSION.remove();
        }
    }

    @RequestMapping("/mqttclient/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody MqttClientDataSourceInfo properties) {
        MqttClientTemplate mqttClientTemplate = null;
        try {
            mqttClientTemplate = SpringUtils.getBean(MagicDynamicMqttClientDataSource.class).createMqttClientTemplate(properties);
            mqttClientTemplate.afterSingletonsInstantiated();
            if (mqttClientTemplate.isConnected()) {
                return new JsonBean<>("ok");
            } else {
                return new JsonBean<>("连接失败: 无法建立MQTT连接");
            }
        } catch (Exception e) {
            return new JsonBean<>("连接失败: " + e.getMessage());
        } finally {
            if (mqttClientTemplate != null) {
                try {
                    mqttClientTemplate.destroy();
                } catch (Exception e) {
                    log.warn("关闭MQTT Client测试连接时发生异常", e);
                }
            }
        }
    }
}