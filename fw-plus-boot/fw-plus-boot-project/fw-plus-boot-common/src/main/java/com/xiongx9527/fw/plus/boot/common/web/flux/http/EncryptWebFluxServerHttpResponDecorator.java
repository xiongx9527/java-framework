package com.xiongx9527.fw.plus.boot.common.web.flux.http;

import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptor;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptorProperties;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class EncryptWebFluxServerHttpResponDecorator extends MyWebFluxServerHttpResponDecorator {

    private ServerHttpResponse delegate;
    private MyHttpEncryptor encryptor = null;
    private MyHttpEncryptorProperties encryptorProperties = null;

    public EncryptWebFluxServerHttpResponDecorator(ServerHttpResponse delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    public EncryptWebFluxServerHttpResponDecorator(ServerHttpResponse delegate,
                                                   MyHttpEncryptor encryptor,
                                                   MyHttpEncryptorProperties encryptorProperties) {
        this(delegate);
        this.encryptor = encryptor;
        this.encryptorProperties = encryptorProperties;
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        Flux<DataBuffer> bufferFlux = Flux.from(body)
                .handle((dataBuffer, sink) -> {
                    byte[] originalContent = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(originalContent);
                    DataBufferUtils.release(dataBuffer);
                    try {
                        // 使用加密器对原始内容进行加密
                        String encryptedContent = encryptor.encrypt(encryptorProperties.getAlgorithm(), encryptorProperties.getPublicKey(),
                                encryptorProperties.getIv(), encryptorProperties.getSalt(), originalContent);
                        sink.next(getDelegate().bufferFactory().wrap(encryptedContent.getBytes()));
                    } catch (Throwable e) {
                        sink.error(new RuntimeException("Encryption failed", e));
                    }
                });
        return super.writeWith(bufferFlux);
    }

}
