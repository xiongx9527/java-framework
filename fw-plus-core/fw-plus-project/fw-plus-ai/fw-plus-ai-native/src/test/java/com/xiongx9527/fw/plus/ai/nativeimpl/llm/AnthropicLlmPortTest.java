package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnthropicLlmPortTest {
    private ServerSocket ss;
    private Thread t;
    private int port;
    @BeforeEach
    void start() throws Exception { ss = new ServerSocket(0); port = ss.getLocalPort(); t = new Thread(this::loop); t.setDaemon(true); t.start(); }
    @AfterEach
    void stop() throws Exception { ss.close(); }
    @Test
    void anthropicCompleteParsesTextAndUsage() {
        AnthropicLlmPort client = new AnthropicLlmPort("http://localhost:" + port + "/v1", "k", "claude");
        LlmResponse r = client.complete(LlmRequest.builder().addMessage(ChatMessage.user("hi")).build());
        assertEquals("Hi", r.getContent());
        assertEquals(1, r.getUsage().getOutputTokens());
    }
    private void loop() {
        while (!ss.isClosed()) {
            try { Socket s = ss.accept(); handle(s); } catch (Exception ignored) { return; }
        }
    }
    private void handle(Socket s) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        String req = br.readLine();
        if (req == null) { s.close(); return; }
        int len = 0; String line;
        while ((line = br.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) len = Integer.parseInt(line.substring(15).trim());
        }
        if (len > 0) { char[] buf = new char[len]; int read = 0; while (read < len) { int n = br.read(buf, read, len - read); if (n < 0) break; read += n; } }
        OutputStream os = s.getOutputStream();
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nConnection: close\r\n\r\n");
        sb.append("event: message_start\ndata: {\"type\":\"message_start\",\"message\":{\"usage\":{\"input_tokens\":4}}}\n\n");
        sb.append("event: content_block_delta\ndata: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"Hi\"}}\n\n");
        sb.append("event: message_delta\ndata: {\"type\":\"message_delta\",\"delta\":{},\"usage\":{\"output_tokens\":1}}\n\n");
        sb.append("event: message_stop\ndata: {\"type\":\"message_stop\"}\n\n");
        os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        os.flush(); s.close();
    }
}
