package com.xx9527.fw.plus.boot.common.web.flux.http;

import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;

public abstract class MyWebFluxServerHttpResponDecorator extends ServerHttpResponseDecorator {

    public MyWebFluxServerHttpResponDecorator(ServerHttpResponse delegate) {
        super(delegate);
    }
}
