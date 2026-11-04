package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.xiongx9527.fw.plus.ai.base.model.LlmException;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.OpenAiCompatibleEmbeddingPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmAndEmbeddingTest {
    private FakeServer server;
    private int port;
    @BeforeEach
    void start() throws Exception { server = new FakeServer("openai"); server.start(); port = server.port(); }
    @AfterEach
    void stop() throws Exception { server.stop(); }
    @Test
    void openAiCompleteAssemblesContentAndUsage() {
        OpenAiCompatibleLlmPort port_client = new OpenAiCompatibleLlmPort("http://localhost:" + port + "/v1", "k", "gpt");
        LlmResponse r = port_client.complete("s", "hello");
        assertEquals("Hello world", r.getContent());
        assertEquals(5, r.getUsage().getInputTokens());
        assertEquals(2, r.getUsage().getOutputTokens());
    }
    @Test
    void embeddingReturnsVectorAndDimension() {
        OpenAiCompatibleEmbeddingPort ep = new OpenAiCompatibleEmbeddingPort("http://localhost:" + port + "/v1", "k", "emb");
        double[] v = ep.embed("hi");
        assertNotNull(v);
        assertEquals(3, v.length);
        assertEquals(0.2, v[1], 1e-9);
        assertEquals(3, ep.dimension());
    }
    @Test
    void unreachableTriggersError() {
        OpenAiCompatibleLlmPort client = new OpenAiCompatibleLlmPort("http://localhost:1/v1", "k", "gpt");
        assertThrows(LlmException.class, () -> client.complete("s", "hi"));
    }
    @Test
    void capturesRequestBody() throws Exception {
        OpenAiCompatibleLlmPort client = new OpenAiCompatibleLlmPort("http://localhost:" + port + "/v1", "k", "gpt");
        client.complete("sys", "u");
        String body = server.lastBody();
        assertNotNull(body);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"model\":\"gpt\""));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"stream\":true"));
    }

    static final class FakeServer {
        private final String kind;
        private ServerSocket ss;
        private Thread t;
        private final AtomicReference<String> lastBody = new AtomicReference<String>();
        FakeServer(String kind) { this.kind = kind; }
        int port() { return ss.getLocalPort(); }
        void start() throws Exception { ss = new ServerSocket(0); t = new Thread(this::loop); t.setDaemon(true); t.start(); }
        void stop() throws Exception { ss.close(); }
        String lastBody() { return lastBody.get(); }
        private void loop() {
            while (!ss.isClosed()) {
                try { Socket s = ss.accept(); handle(s); } catch (Exception ignored) { return; }
            }
        }
        private void handle(Socket s) throws Exception {
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String req = br.readLine();
            if (req == null) { s.close(); return; }
            String path = req.split(" ")[1];
            int len = 0;
            String line;
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) len = Integer.parseInt(line.substring(15).trim());
            }
            StringBuilder body = new StringBuilder();
            if (len > 0) { char[] buf = new char[len]; int read = 0; while (read < len) { int n = br.read(buf, read, len - read); if (n < 0) break; read += n; } body.append(buf, 0, read); }
            lastBody.set(body.toString());
            OutputStream os = s.getOutputStream();
            if (path.contains("embeddings")) {
                String json = "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}],\"model\":\"emb\"}";
                String resp = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + json.length() + "\r\nConnection: close\r\n\r\n" + json;
                os.write(resp.getBytes(StandardCharsets.UTF_8));
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nConnection: close\r\n\r\n");
                sb.append("data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n");
                sb.append("data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n\n");
                sb.append("data: {\"choices\":[{\"delta\":{}}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":2,\"total_tokens\":7}}\n\n");
                sb.append("data: [DONE]\n\n");
                os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            os.flush(); s.close();
        }
    }
}
