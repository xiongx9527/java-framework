
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MagicDynamicMqttClientDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.producer.MqttClientProducer;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.beans.Transient;
import java.util.Objects;

@Slf4j
@MagicModule("mqtt")
public class MqttClientModule implements DynamicAttribute<MqttClientModule, MqttClientModule>, DynamicModule<MqttClientModule> {

    private final MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource;
    private MqttClientProducer mqttClientProducer;
    private String key;

    public MqttClientModule(MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource) {
        this.magicDynamicMqttClientDataSource = magicDynamicMqttClientDataSource;
    }

    public MqttClientModule(MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource, MqttClientTemplate mqttClientTemplate) {
        this.magicDynamicMqttClientDataSource = magicDynamicMqttClientDataSource;
        if (mqttClientTemplate != null) {
            this.mqttClientProducer = new MqttClientProducer(mqttClientTemplate);
        }
    }

    private MqttClientModule(MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource, MqttClientProducer mqttClientProducer) {
        this.magicDynamicMqttClientDataSource = magicDynamicMqttClientDataSource;
        this.mqttClientProducer = mqttClientProducer;
    }

    public MqttClientProducer getMqttClientProducer() {
        return this.mqttClientProducer;
    }

    @Override
    @Transient
    public MqttClientModule getDynamicAttribute(String key) {
        if (Objects.isNull(key) || Options.DEFAULT_DATA_SOURCE.getValue().equalsIgnoreCase(key)
                || "default".equalsIgnoreCase(key)) {
            return this;
        }
        MqttClientModule module = new MqttClientModule(this.magicDynamicMqttClientDataSource, this.magicDynamicMqttClientDataSource.getProducer(key));
        module.key = key;
        return module;
    }

    @Override
    public MqttClientModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        MqttClientModule module = new MqttClientModule(this.magicDynamicMqttClientDataSource, this.magicDynamicMqttClientDataSource.getProducer(dataSourceKey));
        module.key = dataSourceKey;
        return module;
    }

    private MqttClientProducer requireProducer() {
        if (this.mqttClientProducer != null) {
            return this.mqttClientProducer;
        }
        String dataSourceKey = StringUtils.defaultIfBlank(this.key, "default");
        throw new IllegalStateException(I18nMessageResolver.resolveKey("fw.boot.mqtt.client.datasourceNotFound", dataSourceKey));
    }

    public boolean publish(String topic, Object payload) {
        return requireProducer().publish(topic, payload);
    }

    public boolean publish(String topic, Object payload, MqttQoS qos) {
        return requireProducer().publish(topic, payload, qos);
    }

    public boolean publish(String topic, Object payload, int qos) {
        MqttQoS mqttQoS;
        switch (qos) {
            case 1:
                mqttQoS = MqttQoS.QOS1;
                break;
            case 2:
                mqttQoS = MqttQoS.QOS2;
                break;
            default:
                mqttQoS = MqttQoS.QOS0;
                break;
        }
        return publish(topic, payload, mqttQoS);
    }

    @Transient
    public String getKey() {
        return key;
    }
}
