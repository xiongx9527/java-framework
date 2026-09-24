package com.xiongx9527.fw.plus.boot.sample.centrifuge;

import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.service.CentrifugoConnectionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.service.CentrifugoPublishService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class DemoCentrifugoSender {

    @Resource
    private CentrifugoPublishService publishService;

    @Resource
    private CentrifugoConnectionService connectionService;

    @PostConstruct
    public void sendInitMessage() {
        publishService.publish("node-1", "chat", "hello from sample".getBytes());
    }

    public void sendToDefault(String channel, String message) {
        publishService.publish(channel, message.getBytes());
    }

    public void sendToInstance(String instanceName, String channel, String message) {
        publishService.publish(instanceName, channel, message.getBytes());
    }

    public void sendToAll(String channel, String message) {
        publishService.publishToAll(channel, message.getBytes());
    }

    public void directSend(String instanceName, String channel, String message) {
        connectionService.send(instanceName, channel, message.getBytes());
    }
}
