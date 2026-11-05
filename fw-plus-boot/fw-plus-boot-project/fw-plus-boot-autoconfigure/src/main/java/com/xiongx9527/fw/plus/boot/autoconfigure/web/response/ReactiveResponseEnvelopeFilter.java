package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.common.model.JsonResult;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * WebFlux 栈的统一响应信封。
 *
 * <p><b>为什么不是 ResponseBodyAdvice</b>：Spring WebFlux 没有提供
 * {@code ResponseBodyAdvice} 扩展点（该接口只存在于 Spring MVC），因此这里用
 * {@link WebFilter} + {@link ServerHttpResponseDecorator} 在写出前重写 JSON 响应体。</p>
 *
 * <p>跳过规则与 Servlet 版一致：非 JSON（含 {@code text/event-stream} 流式）、附件下载、
 * 空响应、超过 {@code max-buffer-bytes} 的大响应、已经是信封结构的响应。</p>
 */
public class ReactiveResponseEnvelopeFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ReactiveResponseEnvelopeFilter.class);

    private final ResponseProperties properties;

    private final ResponseEnvelopeFactory factory;

    private final ObjectMapper objectMapper;

    public ReactiveResponseEnvelopeFilter(ResponseProperties properties,
                                          ResponseEnvelopeFactory factory,
                                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.factory = factory;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        ServerHttpResponseDecorator decorator = new EnvelopeResponseDecorator(exchange.getResponse());
        return chain.filter(exchange.mutate().response(decorator).build());
    }

    private final class EnvelopeResponseDecorator extends ServerHttpResponseDecorator {

        EnvelopeResponseDecorator(ServerHttpResponse delegate) {
            super(delegate);
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            if (!shouldWrap()) {
                return super.writeWith(body);
            }
            return super.writeWith(DataBufferUtils.join(body).flatMap(buffer -> {
                byte[] raw = new byte[buffer.readableByteCount()];
                buffer.read(raw);
                DataBufferUtils.release(buffer);
                byte[] out = wrap(raw);
                getHeaders().setContentLength(out.length);
                return Mono.just(getDelegate().bufferFactory().wrap(out));
            }));
        }

        private boolean shouldWrap() {
            if (isCommitted()) {
                return false;
            }
            MediaType contentType = getHeaders().getContentType();
            if (contentType == null || !isJson(contentType)) {
                return false;
            }
            String disposition = getHeaders().getFirst("Content-Disposition");
            if (disposition != null && disposition.toLowerCase().contains("attachment")) {
                return false;
            }
            Long length = getHeaders().getContentLength();
            return length < 0 || length <= properties.getMaxBufferBytes();
        }

        private byte[] wrap(byte[] raw) {
            if (raw.length == 0) {
                return raw;
            }
            try {
                JsonNode node = objectMapper.readTree(raw);
                if (node != null && node.isObject()
                        && node.has(properties.getCodeKey()) && node.has(properties.getDataKey())) {
                    return raw;
                }
                JsonResult envelope = factory.success(node);
                return objectMapper.writeValueAsBytes(envelope);
            } catch (Exception ex) {
                log.debug("skip envelope for non-json body: {}", ex.getMessage());
                return raw;
            }
        }
    }

    private static boolean isJson(MediaType mediaType) {
        return MediaType.APPLICATION_JSON.includes(mediaType)
                || (mediaType.getSubtype() != null && mediaType.getSubtype().endsWith("+json"));
    }

    /** 供测试与诊断：把 JSON 文本包成信封（不经过 WebFlux 管道）。 */
    String wrapText(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json.getBytes(StandardCharsets.UTF_8));
        return objectMapper.writeValueAsString(factory.success(node));
    }
}
