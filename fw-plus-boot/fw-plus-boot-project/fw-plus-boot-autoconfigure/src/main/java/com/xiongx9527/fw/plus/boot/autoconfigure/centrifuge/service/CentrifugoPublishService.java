package com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.config.CentrifugoProperties;
import io.github.centrifugal.centrifuge.SubscriptionEventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Centrifugo 最小发布服务。
 */
@Service
public class CentrifugoPublishService {

    private final CentrifugoProperties properties;
    private final CentrifugoConnectionService connectionService;

    public CentrifugoPublishService(CentrifugoProperties properties, CentrifugoConnectionService connectionService) {
        this.properties = properties;
        this.connectionService = connectionService;
    }

    public void publish(String instanceName, String channel, byte[] data) {
        connectionService.publish(instanceName, channel, data);
    }

    public void publishToAll(String channel, byte[] data) {
        connectionService.publishToAll(channel, data);
    }

    public void publish(String channel, byte[] data) {
        List<com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.config.CentrifugoInstanceProperties> instances = properties.getInstances();
        if (instances == null || instances.isEmpty()) {
            connectionService.publish(properties.getSingle().getName(), channel, data);
            return;
        }

        for (com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.config.CentrifugoInstanceProperties instance : instances) {
            if (instance.isEnabled()) {
                connectionService.publish(instance.getName(), channel, data);
                return;
            }
        }
    }

    public void subscribe(String instanceName, String channel) {
        connectionService.subscribe(instanceName, channel);
    }

    public void subscribe(String instanceName, String channel, SubscriptionEventListener listener) {
        connectionService.subscribe(instanceName, channel, listener);
    }

    public void unsubscribe(String instanceName, String channel) {
        connectionService.unsubscribe(instanceName, channel);
    }
}
