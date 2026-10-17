package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.consumer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientSubscribeScriptInfo;
import org.dromara.mica.mqtt.spring.client.MqttClientTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MqttClientConsumerTest {

    @Test
    void disabledSubscriptionClosesExistingConsumerTemplateDuringRefresh() {
        MqttClientDataSourceInfo dataSourceInfo = new MqttClientDataSourceInfo();
        dataSourceInfo.setKey("iot");
        dataSourceInfo.setName("IoT");
        MqttClientConsumer consumer = new MqttClientConsumer(mock(MqttClientConsumerService.class), dataSourceInfo, null);
        MqttClientTemplate oldTemplate = mock(MqttClientTemplate.class);
        Map<String, MqttClientTemplate> consumerMap = (Map<String, MqttClientTemplate>) ReflectionTestUtils.getField(consumer, "consumerMap");
        consumerMap.put("device/+/status", oldTemplate);
        MqttClientSubscribeInfo disabledSubscribe = new MqttClientSubscribeInfo();
        disabledSubscribe.setKey("iot");
        disabledSubscribe.setTopic("device/+/status");
        disabledSubscribe.setEnabled(false);

        consumer.createConsumer(Arrays.asList(new MqttClientSubscribeScriptInfo(disabledSubscribe, "device/+/status")));

        verify(oldTemplate).unSubscribe("device/+/status");
        verify(oldTemplate).destroy();
        assertThat(consumerMap).doesNotContainKey("device/+/status");
    }

    @Test
    void unsubscribeDestroysConsumerTemplateWhenBrokerUnsubscribeFails() {
        MqttClientConsumer consumer = consumer();
        MqttClientTemplate template = mock(MqttClientTemplate.class);
        Map<String, MqttClientTemplate> consumerMap = consumerMap(consumer);
        consumerMap.put("device/+/status", template);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(template).unSubscribe("device/+/status");

        consumer.unsubscribe("device/+/status");

        verify(template).destroy();
        assertThat(consumerMap).doesNotContainKey("device/+/status");
    }

    @Test
    void closeDestroysConsumerTemplateWhenBrokerUnsubscribeFails() {
        MqttClientConsumer consumer = consumer();
        MqttClientTemplate template = mock(MqttClientTemplate.class);
        Map<String, MqttClientTemplate> consumerMap = consumerMap(consumer);
        consumerMap.put("device/+/status", template);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(template).unSubscribe("device/+/status");

        consumer.close();

        verify(template).destroy();
        assertThat(consumerMap).isEmpty();
    }

    private static MqttClientConsumer consumer() {
        MqttClientDataSourceInfo dataSourceInfo = new MqttClientDataSourceInfo();
        dataSourceInfo.setKey("iot");
        dataSourceInfo.setName("IoT");
        return new MqttClientConsumer(mock(MqttClientConsumerService.class), dataSourceInfo, null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, MqttClientTemplate> consumerMap(MqttClientConsumer consumer) {
        return (Map<String, MqttClientTemplate>) ReflectionTestUtils.getField(consumer, "consumerMap");
    }
}
