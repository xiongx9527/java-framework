package com.xiongx9527.fw.plus.boot.common.web.flux.http;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Mono;

public abstract class MyWebFluxServerHttpRequestDecorator extends ServerHttpRequestDecorator {

    public MyWebFluxServerHttpRequestDecorator(ServerHttpRequest delegate) {
        super(delegate);
    }
}
