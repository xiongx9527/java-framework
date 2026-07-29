package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer.MqttClientConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer.MqttClientConsumerService;
import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicDynamicMqttClientDataSourceTest {

    @Test
    void refreshingConsumerReplacesThePreviousDatasourceConfiguration() {
        MagicDynamicMqttClientDataSource dataSource = new MagicDynamicMqttClientDataSource(
                mock(MqttClientConsumerService.class));
        MqttClientDataSourceInfo originalInfo = dataSourceInfo("iot", "192.168.1.10");
        MqttClientDataSourceInfo refreshedInfo = dataSourceInfo("iot", "192.168.1.20");
        dataSource.putConsumer(originalInfo, null);
        MqttClientConsumer originalConsumer = dataSource.getConsumer("iot");
        MqttClientTemplate originalTemplate = mock(MqttClientTemplate.class);
        consumerMap(originalConsumer).put("device/+/status", originalTemplate);

        boolean replaced = dataSource.replaceConsumer(refreshedInfo, null);

        assertThat(replaced).isTrue();
        assertThat(dataSource.getConsumer("iot")).isNotSameAs(originalConsumer);
        assertThat(dataSource.getConsumer("iot").getDataSourceInfo()).isSameAs(refreshedInfo);
        verify(originalTemplate).unSubscribe("device/+/status");
        verify(originalTemplate).destroy();
    }

    @Test
    void addingSubscriptionKeepsTheExistingConsumer() {
        MagicDynamicMqttClientDataSource dataSource = new MagicDynamicMqttClientDataSource(
                mock(MqttClientConsumerService.class));
        MqttClientDataSourceInfo info = dataSourceInfo("iot", "192.168.1.10");
        dataSource.putConsumer(info, null);
        MqttClientConsumer originalConsumer = dataSource.getConsumer("iot");

        dataSource.putConsumer(info, null);

        assertThat(dataSource.getConsumer("iot")).isSameAs(originalConsumer);
    }

    @Test
    void failedReplacementKeepsTheExistingConsumer() {
        MagicDynamicMqttClientDataSource dataSource = new MagicDynamicMqttClientDataSource(
                mock(MqttClientConsumerService.class));
        MqttClientDataSourceInfo originalInfo = dataSourceInfo("iot", "192.168.1.10");
        dataSource.putConsumer(originalInfo, null);
        MqttClientConsumer originalConsumer = dataSource.getConsumer("iot");
        MqttClientDataSourceInfo refreshedInfo = dataSourceInfo("iot", "192.168.1.20");
        MqttClientSubscribeScriptInfo failingSubscription = mock(MqttClientSubscribeScriptInfo.class);
        when(failingSubscription.getTopic()).thenReturn("device/+/status");
        when(failingSubscription.isEnabled()).thenThrow(new IllegalStateException("invalid subscription"));

        boolean replaced = dataSource.replaceConsumer(refreshedInfo,
                Collections.singletonList(failingSubscription));

        assertThat(replaced).isFalse();
        assertThat(dataSource.getConsumer("iot")).isSameAs(originalConsumer);
    }

    @Test
    void unsubscribeRemovesTopicFromTheCurrentConsumer() {
        MagicDynamicMqttClientDataSource dataSource = new MagicDynamicMqttClientDataSource(
                mock(MqttClientConsumerService.class));
        MqttClientDataSourceInfo info = dataSourceInfo("iot", "192.168.1.10");
        dataSource.putConsumer(info, null);
        MqttClientTemplate template = mock(MqttClientTemplate.class);
        consumerMap(dataSource.getConsumer("iot")).put("device/+/status", template);
        MqttClientSubscribeInfo subscription = new MqttClientSubscribeInfo();
        subscription.setKey("iot");
        subscription.setTopic("device/+/status");

        dataSource.unsubscribe(subscription);

        verify(template).unSubscribe("device/+/status");
        verify(template).destroy();
        assertThat(consumerMap(dataSource.getConsumer("iot"))).doesNotContainKey("device/+/status");
    }

    @Test
    void destroyedDatasourceRejectsNewConsumers() throws Exception {
        MagicDynamicMqttClientDataSource dataSource = new MagicDynamicMqttClientDataSource(
                mock(MqttClientConsumerService.class));
        MqttClientDataSourceInfo info = dataSourceInfo("iot", "192.168.1.10");
        dataSource.destroy();

        boolean added = dataSource.putConsumer(info, null);
        boolean replaced = dataSource.replaceConsumer(info, null);

        assertThat(added).isFalse();
        assertThat(replaced).isFalse();
        assertThat(dataSource.getConsumer("iot")).isNull();
    }

    @Test
    void snapshotReplacementLoadsAndAppliesSnapshotThroughDatasourceBoundary() {
        MagicDynamicMqttClientDataSource dataSource = new MagicDynamicMqttClientDataSource(
                mock(MqttClientConsumerService.class));
        MqttClientDataSourceInfo originalInfo = dataSourceInfo("iot", "192.168.1.10");
        MqttClientDataSourceInfo refreshedInfo = dataSourceInfo("iot", "192.168.1.20");
        dataSource.putConsumer(originalInfo, null);
        AtomicBoolean snapshotLoaded = new AtomicBoolean();

        boolean replaced = dataSource.replaceConsumerFromSnapshot(refreshedInfo, () -> {
            snapshotLoaded.set(true);
            return Collections.emptyList();
        });

        assertThat(replaced).isTrue();
        assertThat(snapshotLoaded).isTrue();
        assertThat(dataSource.getConsumer("iot").getDataSourceInfo()).isSameAs(refreshedInfo);
    }

    private static MqttClientDataSourceInfo dataSourceInfo(String key, String ip) {
        MqttClientDataSourceInfo info = new MqttClientDataSourceInfo();
        info.setKey(key);
        info.setName(key);
        info.setIp(ip);
        return info;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, MqttClientTemplate> consumerMap(MqttClientConsumer consumer) {
        return (Map<String, MqttClientTemplate>) ReflectionTestUtils.getField(consumer, "consumerMap");
    }
}
