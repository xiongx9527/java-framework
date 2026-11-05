package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 校验原生 OpenAI 兼容端口补齐的事件面：finish_reason→onFinish、增量工具调用→onPartialToolCall /
 * onCompleteToolCall、原始帧→onUnmappedRawEvent 都已派发。
 */
class NativeLlmEventSurfaceTest {

    private ServerSocket server;
    private Thread thread;
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
    void emitsFinishReasonPartialToolCallAndRawEvent() throws Exception {
        response = "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"function\":{\"name\":\"search\",\"arguments\":\"foo\"}}]}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"bar\"}}]}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"tool_calls\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":3,\"total_tokens\":8}}\n\n"
                + "data: [DONE]\n\n";

        OpenAiCompatibleLlmPort port = new OpenAiCompatibleLlmPort(url(), "k", "gpt");
        CapturingHandler handler = new CapturingHandler();
        LlmTokenStream stream = port.stream(LlmRequest.builder().addMessage(ChatMessage.user("hi")).build());
        stream.handler(handler).start();
        assertTrue(handler.done.await(10, TimeUnit.SECONDS), "stream should finish");

        assertEquals("tool_calls", handler.finishReason);
        assertEquals(1, handler.completeToolCalls.size());
        assertEquals("search", handler.completeToolCalls.get(0).getName());
        assertTrue(handler.partialToolCalls.contains("search"));
        // 每个 SSE 帧（非 [DONE]）都应透传原始事件
        assertTrue(handler.rawEvents.size() >= 3, "raw events should be forwarded: " + handler.rawEvents.size());
    }

    private String url() {
        return "http://localhost:" + server.getLocalPort() + "/v1";
    }

    private void serve() {
        try (Socket socket = server.accept()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            reader.readLine();
            int length = 0;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    length = Integer.parseInt(line.substring(15).trim());
                }
            }
            char[] chars = new char[length];
            int read = 0;
            while (read < length) {
                int count = reader.read(chars, read, length - read);
                if (count < 0) {
                    break;
                }
                read += count;
            }
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

    private static final class CapturingHandler implements LlmStreamHandler {
        final CountDownLatch done = new CountDownLatch(1);
        String finishReason;
        final List<ToolExecutionRequest> completeToolCalls = new ArrayList<ToolExecutionRequest>();
        final List<String> partialToolCalls = new ArrayList<String>();
        final List<String> partialArgs = new ArrayList<String>();
        final List<Object> rawEvents = new ArrayList<Object>();

        @Override
        public void onToken(String content, String reasoningContent) {
        }

        @Override
        public void onComplete() {
            done.countDown();
        }

        @Override
        public void onError(String message) {
            done.countDown();
        }

        @Override
        public void onPartialToolCall(String id, String name, String argumentsDelta) {
            partialToolCalls.add(name);
            partialArgs.add(argumentsDelta == null ? "" : argumentsDelta);
        }

        @Override
        public void onCompleteToolCall(ToolExecutionRequest call) {
            completeToolCalls.add(call);
        }

        @Override
        public void onFinish(String finishReason) {
            this.finishReason = finishReason;
        }

        @Override
        public void onUnmappedRawEvent(Object rawEvent) {
            rawEvents.add(rawEvent);
        }
    }
}
