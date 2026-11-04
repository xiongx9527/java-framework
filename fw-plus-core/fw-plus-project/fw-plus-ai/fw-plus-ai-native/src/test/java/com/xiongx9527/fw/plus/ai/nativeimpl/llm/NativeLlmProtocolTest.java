package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
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

class NativeLlmProtocolTest {
    private ServerSocket server;
    private Thread thread;
    private final AtomicReference<String> requestLine = new AtomicReference<String>();
    private final AtomicReference<String> requestBody = new AtomicReference<String>();
    private final AtomicReference<String> requestHeaders = new AtomicReference<String>();
    private volatile String response;

    @BeforeEach
    void start() throws Exception {
        server = new ServerSocket(0);
        thread = new Thread(this::serve);
        thread.setDaemon(true);
        thread.start();
    }

    @AfterEach
    void stop() throws Exception {
        server.close();
    }

    @Test
    void responsesPortUsesResponsesEndpointAndParsesOutput() {
        response = "event: response.output_text.delta\ndata: {\"type\":\"response.output_text.delta\",\"delta\":\"Hello\"}\n\n"
                + "event: response.completed\ndata: {\"type\":\"response.completed\",\"response\":{\"usage\":{\"input_tokens\":2,\"output_tokens\":1,\"total_tokens\":3}}}\n\n";
        OpenAiResponsesLlmPort port = new OpenAiResponsesLlmPort(url(), "key", "gpt-responses");
        LlmResponse result = port.complete(LlmRequest.builder().addMessage(ChatMessage.user("hi")).build());
        assertEquals("Hello", result.getContent());
        assertEquals(3, result.getUsage().getTotalTokens());
        assertTrue(requestLine.get().contains("POST /v1/responses "));
        assertTrue(requestBody.get().contains("\"input\""));
    }

    @Test
    void geminiPortUsesNativeStreamingEndpointAndParsesOutput() {
        response = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hi Gemini\"}]}}],\"usageMetadata\":{\"promptTokenCount\":4,\"candidatesTokenCount\":2,\"totalTokenCount\":6}}\n\n";
        GeminiLlmPort port = new GeminiLlmPort(url(), "key", "gemini-2.0-flash");
        LlmResponse result = port.complete(LlmRequest.builder().addMessage(ChatMessage.user("hi")).build());
        assertEquals("Hi Gemini", result.getContent());
        assertEquals(6, result.getUsage().getTotalTokens());
        assertTrue(requestLine.get().contains("POST /v1beta/models/gemini-2.0-flash:streamGenerateContent?alt=sse "));
        assertTrue(requestBody.get().contains("\"contents\""));
    }

    @Test
    void azurePortUsesDeploymentEndpointAndApiKeyHeader() {
        response = "data: {\"choices\":[{\"delta\":{\"content\":\"Hello Azure\"}}]}\n\n"
                + "data: [DONE]\n\n";
        AzureOpenAiLlmPort port = new AzureOpenAiLlmPort(url(), "azure-key", "deployment-1", "2025-01-01");
        LlmResponse result = port.complete(LlmRequest.builder().addMessage(ChatMessage.user("hi")).build());
        assertEquals("Hello Azure", result.getContent());
        assertTrue(requestLine.get().contains(
                "POST /v1/openai/deployments/deployment-1/chat/completions?api-version=2025-01-01 "));
        assertTrue(requestHeaders.get().contains("api-key: azure-key"));
        assertTrue(!requestHeaders.get().contains("Authorization: Bearer"));
    }

    @Test
    void azurePortListsModelsFromAzureModelsEndpoint() {
        response = "{\"data\":[{\"id\":\"deployment-1\"}]}";
        AzureOpenAiLlmPort port = new AzureOpenAiLlmPort(url(), "azure-key", "deployment-1", "2025-01-01");
        assertEquals("deployment-1", port.listModels().get(0).getId());
        assertTrue(requestLine.get().contains("GET /v1/openai/models?api-version=2025-01-01 "));
        assertTrue(requestHeaders.get().contains("api-key: azure-key"));
    }

    private String url() { return "http://localhost:" + server.getLocalPort() + "/v1"; }

    private void serve() {
        try (Socket socket = server.accept()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            requestLine.set(reader.readLine());
            int length = 0;
            String line;
            StringBuilder headers = new StringBuilder();
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                headers.append(line).append("\n");
                if (line.toLowerCase().startsWith("content-length:")) length = Integer.parseInt(line.substring(15).trim());
            }
            requestHeaders.set(headers.toString());
            char[] chars = new char[length];
            int read = 0;
            while (read < length) {
                int count = reader.read(chars, read, length - read);
                if (count < 0) break;
                read += count;
            }
            requestBody.set(new String(chars, 0, read));
            OutputStream output = socket.getOutputStream();
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            String header = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nContent-Length: "
                    + body.length + "\r\nConnection: close\r\n\r\n";
            output.write(header.getBytes(StandardCharsets.UTF_8));
            output.write(body);
            output.flush();
        } catch (Exception ignored) {
        }
    }
}
