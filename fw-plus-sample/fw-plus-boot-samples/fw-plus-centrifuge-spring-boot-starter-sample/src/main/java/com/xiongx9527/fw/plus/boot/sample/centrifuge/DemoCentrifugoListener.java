package com.xiongx9527.fw.plus.boot.sample.centrifuge;

import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.listener.CentrifugoMessageListener;
import org.springframework.stereotype.Component;

@Component
public class DemoCentrifugoListener implements CentrifugoMessageListener {

    @Override
    public void onConnected(String clientId) {
        System.out.println("[centrifuge] connected: " + clientId);
    }

    @Override
    public void onDisconnected(int code, String reason) {
        System.out.println("[centrifuge] disconnected: " + code + ", " + reason);
    }

    @Override
    public void onError(Throwable error) {
        System.err.println("[centrifuge] error");
        error.printStackTrace();
    }

    @Override
    public void onMessage(Object subscription, byte[] data) {
        System.out.println("[centrifuge] message: " + new String(data));
    }
}
