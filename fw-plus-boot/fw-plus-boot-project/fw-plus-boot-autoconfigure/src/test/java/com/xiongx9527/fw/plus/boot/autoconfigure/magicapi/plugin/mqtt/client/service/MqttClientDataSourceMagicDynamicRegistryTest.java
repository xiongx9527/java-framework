package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MagicDynamicMqttClientDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientSubscribeScriptInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqttClientDataSourceMagicDynamicRegistryTest {

    @Test
    void unregisterDataSourceDeletesProducerAndConsumer() {
        MagicDynamicMqttClientDataSource dataSource = mock(MagicDynamicMqttClientDataSource.class);
        MqttClientDataSourceMagicDynamicRegistry registry = new MqttClientDataSourceMagicDynamicRegistry(
                mock(MagicResourceStorage.class), dataSource);
        MqttClientDataSourceInfo info = new MqttClientDataSourceInfo();
        info.setKey("iot");

        registry.unregisterByEntity(info);

        verify(dataSource).deleteProducer("iot");
        verify(dataSource).deleteConsumer("iot");
    }

    @Test
    void mappingRegisterRefreshOnlyDeletesProducer() {
        MagicDynamicMqttClientDataSource dataSource = mock(MagicDynamicMqttClientDataSource.class);
        TestableMqttClientDataSourceMagicDynamicRegistry registry = new TestableMqttClientDataSourceMagicDynamicRegistry(
                mock(MagicResourceStorage.class), dataSource, null);
        MqttClientDataSourceInfo info = new MqttClientDataSourceInfo();
        info.setKey("iot");
        when(dataSource.putProducer(info)).thenReturn(true);

        registry.registerMapping(info);

        verify(dataSource).deleteProducer("iot");
        verify(dataSource).putProducer(info);
        verify(dataSource, never()).deleteConsumer("iot");
    }

    @Test
    void mappingRegisterRefreshesProducerThenRebuildsEnabledConsumersForSameKey() {
        MagicDynamicMqttClientDataSource dataSource = mock(MagicDynamicMqttClientDataSource.class);
        MqttClientSubscribeInfoMagicResourceStorage subscribeStorage = mock(MqttClientSubscribeInfoMagicResourceStorage.class);
        TestableMqttClientDataSourceMagicDynamicRegistry registry = new TestableMqttClientDataSourceMagicDynamicRegistry(
                mock(MagicResourceStorage.class), dataSource, subscribeStorage);
        MqttClientDataSourceInfo info = new MqttClientDataSourceInfo();
        info.setKey("iot");
        MqttClientSubscribeInfo enabledSameKey = subscribe("iot", "device/+/status", true);
        MqttClientSubscribeInfo disabledSameKey = subscribe("iot", "device/+/shadow", false);
        MqttClientSubscribeInfo enabledOtherKey = subscribe("other", "device/+/status", true);
        when(dataSource.putProducer(info)).thenReturn(true);
        when(subscribeStorage.findAll()).thenReturn(Arrays.asList(enabledSameKey, disabledSameKey, enabledOtherKey));

        registry.registerMapping(info);

        verify(dataSource).deleteProducer("iot");
        verify(dataSource, never()).deleteConsumer("iot");
        verify(dataSource).putProducer(info);
        ArgumentCaptor<Supplier<List<MqttClientSubscribeScriptInfo>>> snapshotCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(dataSource).replaceConsumerFromSnapshot(same(info), snapshotCaptor.capture());
        List<MqttClientSubscribeScriptInfo> scripts = snapshotCaptor.getValue().get();
        assertThat(scripts).hasSize(2);
        assertThat(scripts).extracting(MqttClientSubscribeScriptInfo::getTopic)
                .containsExactlyInAnyOrder("device/+/status", "device/+/shadow");
    }

    @Test
    void mappingRegisterDoesNotRebuildConsumersWhenProducerRefreshFails() {
        MagicDynamicMqttClientDataSource dataSource = mock(MagicDynamicMqttClientDataSource.class);
        MqttClientSubscribeInfoMagicResourceStorage subscribeStorage = mock(MqttClientSubscribeInfoMagicResourceStorage.class);
        TestableMqttClientDataSourceMagicDynamicRegistry registry = new TestableMqttClientDataSourceMagicDynamicRegistry(
                mock(MagicResourceStorage.class), dataSource, subscribeStorage);
        MqttClientDataSourceInfo info = new MqttClientDataSourceInfo();
        info.setKey("iot");
        when(dataSource.putProducer(info)).thenReturn(false);

        registry.registerMapping(info);

        verify(subscribeStorage, never()).findAll();
        verify(dataSource, never()).replaceConsumerFromSnapshot(eq(info), org.mockito.ArgumentMatchers.<Supplier<List<MqttClientSubscribeScriptInfo>>>any());
    }

    @Test
    void mappingRegisterAppliesEmptySubscriptionSnapshotToConsumer() {
        MagicDynamicMqttClientDataSource dataSource = mock(MagicDynamicMqttClientDataSource.class);
        MqttClientSubscribeInfoMagicResourceStorage subscribeStorage = mock(MqttClientSubscribeInfoMagicResourceStorage.class);
        TestableMqttClientDataSourceMagicDynamicRegistry registry = new TestableMqttClientDataSourceMagicDynamicRegistry(
                mock(MagicResourceStorage.class), dataSource, subscribeStorage);
        MqttClientDataSourceInfo info = new MqttClientDataSourceInfo();
        info.setKey("iot");
        when(dataSource.putProducer(info)).thenReturn(true);
        when(subscribeStorage.findAll()).thenReturn(Collections.<MqttClientSubscribeInfo>emptyList());

        registry.registerMapping(info);

        ArgumentCaptor<Supplier<List<MqttClientSubscribeScriptInfo>>> snapshotCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(dataSource).replaceConsumerFromSnapshot(same(info), snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().get()).isEmpty();
    }

    private static MqttClientSubscribeInfo subscribe(String key, String topic, boolean enabled) {
        MqttClientSubscribeInfo info = new MqttClientSubscribeInfo();
        info.setKey(key);
        info.setTopic(topic);
        info.setEnabled(enabled);
        return info;
    }

    private static class TestableMqttClientDataSourceMagicDynamicRegistry extends MqttClientDataSourceMagicDynamicRegistry {
        TestableMqttClientDataSourceMagicDynamicRegistry(MagicResourceStorage<MqttClientDataSourceInfo> magicResourceStorage,
                                                         MagicDynamicMqttClientDataSource dataSource,
                                                         MqttClientSubscribeInfoMagicResourceStorage subscribeStorage) {
            super(magicResourceStorage, dataSource, subscribeStorage);
        }

        boolean registerMapping(MqttClientDataSourceInfo entity) {
            return super.register(new MappingNode<MqttClientDataSourceInfo>(entity));
        }
    }
}
