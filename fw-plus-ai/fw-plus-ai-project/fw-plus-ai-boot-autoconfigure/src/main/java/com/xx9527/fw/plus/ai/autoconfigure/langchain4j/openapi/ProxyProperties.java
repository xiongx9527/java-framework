package com.xx9527.fw.plus.ai.autoconfigure.langchain4j.openapi;

import lombok.Data;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Data
class ProxyProperties {
    Proxy.Type type;
    String host;
    Integer port;

    static Proxy convert(ProxyProperties proxy) {
        return proxy == null ? null : new Proxy(proxy.type, new InetSocketAddress(proxy.host, proxy.port));
    }
}
