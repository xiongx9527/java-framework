package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.consumer.MqttClientConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MagicDynamicMqttClientDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MqttClientModuleTest {

    @Test
    void publishWithMissingDynamicDataSourceIncludesKeyInErrorMessage() {
        MagicDynamicMqttClientDataSource dataSource = new MagicDynamicMqttClientDataSource(mock(MqttClientConsumerService.class));
        MqttClientModule module = new MqttClientModule(dataSource).getDynamicAttribute("missing");

        assertThatThrownBy(() -> module.publish("topic/a", "payload"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");
    }
}
