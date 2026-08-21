//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.openai.spring;

import java.net.InetSocketAddress;
import java.net.Proxy;

class ProxyProperties {
    Proxy.Type type;
    String host;
    Integer port;

    static Proxy convert(ProxyProperties proxy) {
        return proxy == null ? null : new Proxy(proxy.type, new InetSocketAddress(proxy.host, proxy.port));
    }

    public Proxy.Type getType() {
        return this.type;
    }

    public String getHost() {
        return this.host;
    }

    public Integer getPort() {
        return this.port;
    }

    public void setType(Proxy.Type type) {
        this.type = type;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
