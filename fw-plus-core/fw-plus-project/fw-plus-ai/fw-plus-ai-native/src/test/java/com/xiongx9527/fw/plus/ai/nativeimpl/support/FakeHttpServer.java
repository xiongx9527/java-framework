package com.xiongx9527.fw.plus.ai.nativeimpl.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * 进程内 HTTP 桩：native 后端的契约测试用它模拟真实 REST 报文，不依赖外部服务。
 * 基于 okhttp3 MockWebServer 实现（与原 nativeimpl.http.FakeHttpServer 保持兼容 API）。
 */
public final class FakeHttpServer implements AutoCloseable {

    public interface Handler {
        Response handle(Request request);
    }

    public static final class Request {
        public final String method;
        public final String path;
        public final String body;
        public final Map<String, String> headers;

        Request(String method, String path, String body, Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.body = body;
            this.headers = headers == null ? Collections.<String, String>emptyMap() : headers;
        }

        public String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }
    }

    public static final class Response {
        public final int status;
        public final String body;
        public final Map<String, String> headers;

        public Response(int status, String body, Map<String, String> headers) {
            this.status = status;
            this.body = body;
            this.headers = headers == null ? Collections.<String, String>emptyMap() : headers;
        }
    }

    public static Response json(int status, String body) {
        return new Response(status, body, null);
    }

    public static Response json(int status, String body, Map<String, String> headers) {
        return new Response(status, body, headers);
    }

    /** 把单引号写成双引号，便于测试里直接写 JSON 字面量而不必在 Java 源码里转义引号。 */
    public static String body(String singleQuoted) {
        return singleQuoted.replace('\'', '"');
    }

    private final MockWebServer server;
    private volatile Handler handler;
    private final List<Request> requests = new CopyOnWriteArrayList<Request>();

    public FakeHttpServer(Handler handler) throws Exception {
        this.handler = handler;
        this.server = new MockWebServer();
        this.server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest recorded) {
                Map<String, String> headerMap = new LinkedHashMap<String, String>();
                for (String name : recorded.getHeaders().names()) {
                    headerMap.put(name.toLowerCase(Locale.ROOT), recorded.getHeader(name));
                }
                Request req = new Request(recorded.getMethod(), recorded.getPath(),
                        safeBody(recorded), headerMap);
                requests.add(req);
                try {
                    Handler current = handler;
                    Response resp = current == null ? json(200, "{}") : current.handle(req);
                    if (resp == null) {
                        return new MockResponse().setResponseCode(500).setBody("{}");
                    }
                    MockResponse mr = new MockResponse().setResponseCode(resp.status)
                            .setBody(resp.body == null ? "" : resp.body);
                    if (resp.headers != null) {
                        for (Map.Entry<String, String> e : resp.headers.entrySet()) {
                            mr.setHeader(e.getKey(), e.getValue());
                        }
                    }
                    return mr;
                } catch (Exception ex) {
                    return new MockResponse().setResponseCode(500)
                            .setBody("{\"error\":\"handler failed\"}");
                }
            }
        });
        this.server.start();
    }

    public String url() {
        return server.url("").toString().replaceAll("/+$", "");
    }

    public void setHandler(Handler value) {
        this.handler = value;
    }

    public List<Request> requests() {
        synchronized (requests) {
            return new ArrayList<Request>(requests);
        }
    }

    public Request lastRequest() {
        synchronized (requests) {
            return requests.isEmpty() ? null : requests.get(requests.size() - 1);
        }
    }

    @Override
    public void close() throws Exception {
        server.shutdown();
    }

    private static String safeBody(RecordedRequest recorded) {
        try {
            okio.Buffer b = recorded.getBody();
            return b == null ? "" : b.readUtf8();
        } catch (Exception e) {
            return "";
        }
    }
}
