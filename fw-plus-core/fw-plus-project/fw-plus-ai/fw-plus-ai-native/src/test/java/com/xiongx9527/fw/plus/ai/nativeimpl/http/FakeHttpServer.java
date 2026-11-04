package com.xiongx9527.fw.plus.ai.nativeimpl.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 进程内 HTTP 桩：native 后端的契约测试用它模拟真实 REST 报文，不依赖外部服务。 */
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
            this.headers = headers;
        }

        public String header(String name) {
            return headers.get(name.toLowerCase(java.util.Locale.ROOT));
        }
    }

    public static final class Response {
        public final int status;
        public final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    public static Response json(int status, String body) {
        return new Response(status, body);
    }

    /** 把单引号写成双引号，便于测试里直接写 JSON 字面量而不必在 Java 源码里转义引号。 */
    public static String body(String singleQuoted) {
        return singleQuoted.replace('\'', '"');
    }

    private final ServerSocket serverSocket;
    private final Thread thread;
    private volatile Handler handler;
    private final List<Request> requests = Collections.synchronizedList(new ArrayList<Request>());

    public FakeHttpServer(Handler handler) throws Exception {
        this.handler = handler;
        this.serverSocket = new ServerSocket(0);
        this.thread = new Thread(this::loop);
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public String url() {
        return "http://localhost:" + port();
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

    private void loop() {
        while (!serverSocket.isClosed()) {
            try (Socket socket = serverSocket.accept()) {
                handle(socket);
            } catch (Exception ignored) {
                return;
            }
        }
    }

    private void handle(Socket socket) throws Exception {
        InputStream in = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String requestLine = reader.readLine();
        if (requestLine == null) {
            return;
        }
        String[] parts = requestLine.split(" ");
        String method = parts.length > 0 ? parts[0] : "GET";
        String path = parts.length > 1 ? parts[1] : "/";
        Map<String, String> headers = new LinkedHashMap<String, String>();
        int length = 0;
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                headers.put(key.toLowerCase(java.util.Locale.ROOT), value);
                if ("Content-Length".equalsIgnoreCase(key)) {
                    length = Integer.parseInt(value);
                }
            }
        }
        StringBuilder body = new StringBuilder();
        if (length > 0) {
            char[] buf = new char[length];
            int read = 0;
            while (read < length) {
                int n = reader.read(buf, read, length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
            body.append(buf, 0, read);
        }
        Request request = new Request(method, path, body.toString(), headers);
        requests.add(request);
        Response response;
        try {
            Handler current = handler;
            response = current == null ? json(200, "{}") : current.handle(request);
        } catch (Exception ex) {
            response = json(500, FakeHttpServer.body("{'error':'handler failed'}"));
        }
        byte[] payload = response.body.getBytes(StandardCharsets.UTF_8);
        OutputStream os = socket.getOutputStream();
        os.write(("HTTP/1.1 " + response.status + " OK" + CRLF
                + "Content-Type: application/json" + CRLF
                + "Content-Length: " + payload.length + CRLF
                + "Connection: close" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
        os.write(payload);
        os.flush();
    }

    private static final String CRLF = "\r\n";

    @Override
    public void close() throws Exception {
        serverSocket.close();
    }
}
