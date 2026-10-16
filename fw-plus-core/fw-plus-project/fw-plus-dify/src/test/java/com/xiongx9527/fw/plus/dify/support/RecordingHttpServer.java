package com.xiongx9527.fw.plus.dify.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 测试用本地 HTTP 记录服务器（基于 JDK 内置 com.sun.net.httpserver.HttpServer）。
 * <p>记录每个请求的方法、路径、原始 query、Authorization 头与请求体，
 * 并按注入的 {@link Responder} 返回响应。类名不含 Test，Surefire 不会将其作为测试类执行。</p>
 */
public class RecordingHttpServer implements Closeable {

    /**
     * 响应应答器：根据记录到的请求决定状态码、响应头与响应体
     */
    public interface Responder {
        void respond(Recorded request, HttpExchange exchange) throws IOException;
    }

    /**
     * 一次已记录的请求
     */
    public static class Recorded {
        public final String method;
        public final String path;
        public final String rawQuery;
        public final String authorization;
        public final String body;

        Recorded(String method, String path, String rawQuery, String authorization, String body) {
            this.method = method;
            this.path = path;
            this.rawQuery = rawQuery;
            this.authorization = authorization;
            this.body = body;
        }

        /**
         * 解码后的 query 参数（键与值均 URL 解码）
         */
        public Map<String, String> query() {
            Map<String, String> result = new HashMap<String, String>();
            if (rawQuery == null || rawQuery.length() == 0) {
                return result;
            }
            String[] pairs = rawQuery.split("&");
            for (String pair : pairs) {
                if (pair.length() == 0) {
                    continue;
                }
                int idx = pair.indexOf('=');
                String key = idx >= 0 ? pair.substring(0, idx) : pair;
                String value = idx >= 0 ? pair.substring(idx + 1) : "";
                result.put(decode(key), decode(value));
            }
            return result;
        }

        /**
         * 取单个解码后的 query 参数值，不存在返回 null
         */
        public String query(String key) {
            return query().get(key);
        }

        private static String decode(String value) {
            try {
                return URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static final Responder DEFAULT_RESPONDER = json(200, "{\"ok\":true}");

    private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<Recorded>());
    private volatile Responder responder = DEFAULT_RESPONDER;
    private HttpServer server;
    private ExecutorService executor;

    public RecordingHttpServer() {
    }

    public RecordingHttpServer(Responder responder) {
        this.responder = responder;
    }

    /**
     * 启动服务器（随机空闲端口）
     */
    public RecordingHttpServer start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                Recorded recorded = new Recorded(
                        exchange.getRequestMethod(),
                        exchange.getRequestURI().getPath(),
                        exchange.getRequestURI().getQuery(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        readBody(exchange.getRequestBody())
                );
                requests.add(recorded);
                Responder current = responder;
                if (current == null) {
                    current = DEFAULT_RESPONDER;
                }
                current.respond(recorded, exchange);
            }
        });
        server.start();
        return this;
    }

    /**
     * 服务基址（如 http://127.0.0.1:12345）
     */
    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /**
     * 最近一次记录的请求
     */
    public Recorded last() {
        synchronized (requests) {
            if (requests.isEmpty()) {
                throw new AssertionError("no request recorded");
            }
            return requests.get(requests.size() - 1);
        }
    }

    /**
     * 已记录请求总数
     */
    public int count() {
        return requests.size();
    }

    /**
     * 替换应答器（同一服务器上多阶段断言）
     */
    public RecordingHttpServer respondWith(Responder responder) {
        this.responder = responder;
        return this;
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * JSON 响应应答器
     */
    public static Responder json(final int status, final String body) {
        return new Responder() {
            @Override
            public void respond(Recorded request, HttpExchange exchange) throws IOException {
                byte[] data = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(status, data.length);
                OutputStream out = exchange.getResponseBody();
                out.write(data);
                out.close();
            }
        };
    }

    /**
     * 二进制响应应答器（文件下载、音频、ZIP）
     */
    public static Responder bytes(final int status, final String contentType, final byte[] data) {
        return new Responder() {
            @Override
            public void respond(Recorded request, HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(status, data.length);
                OutputStream out = exchange.getResponseBody();
                out.write(data);
                out.close();
            }
        };
    }

    /**
     * SSE（text/event-stream）响应应答器，写完 payload 后关闭连接
     */
    public static Responder sse(final String payload) {
        return new Responder() {
            @Override
            public void respond(Recorded request, HttpExchange exchange) throws IOException {
                byte[] data = payload.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, 0);
                OutputStream out = exchange.getResponseBody();
                out.write(data);
                out.flush();
                out.close();
            }
        };
    }

    /**
     * 按调用次序返回不同响应的应答器（第 1 次用第一个，之后用最后一个）
     */
    public static Responder sequence(final Responder... responders) {
        return new Responder() {
            private int index = 0;

            @Override
            public void respond(Recorded request, HttpExchange exchange) throws IOException {
                Responder target = responders[Math.min(index, responders.length - 1)];
                index++;
                target.respond(request, exchange);
            }
        };
    }

    private static String readBody(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
