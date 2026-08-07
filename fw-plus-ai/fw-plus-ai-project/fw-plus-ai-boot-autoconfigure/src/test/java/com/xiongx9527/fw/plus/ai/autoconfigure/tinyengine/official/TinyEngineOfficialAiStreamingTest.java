package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineOfficialAiStreamingTest {

	private HttpServer upstream;

	@AfterEach
	void stopUpstream() {
		if (upstream != null) {
			upstream.stop(0);
		}
	}

	@Test
	void streamsRawSseFramesBeforeUpstreamCompletesAndClosesOnConsumerCancellation() throws Exception {
		CountDownLatch firstFrameWritten = new CountDownLatch(1);
		CountDownLatch releaseUpstream = new CountDownLatch(1);
		CountDownLatch upstreamClosed = new CountDownLatch(1);
		upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		upstream.createContext("/v1/chat/completions", new DelayedSseHandler(firstFrameWritten, releaseUpstream, upstreamClosed));
		upstream.start();

		TinyEngineAiProperties properties = new TinyEngineAiProperties();
		DefaultTinyEngineOpenAiCompatibleGateway gateway = new DefaultTinyEngineOpenAiCompatibleGateway(properties,
				new ObjectMapper());
		AtomicReference<String> firstFrame = new AtomicReference<String>();
		CountDownLatch received = new CountDownLatch(1);
		AtomicReference<TinyEngineSseSession> session = new AtomicReference<TinyEngineSseSession>();
		Thread streamingThread = new Thread(() -> gateway.stream(request(), new TinyEngineSseConsumer() {
			@Override public void onSession(TinyEngineSseSession value) { session.set(value); }
			@Override public void accept(byte[] bytes) {
				firstFrame.set(new String(bytes, StandardCharsets.UTF_8));
				received.countDown();
			}
		}));
		streamingThread.start();

		assertTrue(firstFrameWritten.await(2, TimeUnit.SECONDS));
		assertTrue(received.await(2, TimeUnit.SECONDS));
		assertEquals("data: {\"choices\":[{\"delta\":{\"content\":\"A\"}}]}\n\n", firstFrame.get());
		session.get().close();
		releaseUpstream.countDown();
		assertTrue(upstreamClosed.await(2, TimeUnit.SECONDS));
	}

	private TinyEngineOfficialAiRequest request() {
		return TinyEngineOfficialAiRequest.parse(new java.util.LinkedHashMap<String, Object>() {{
			put("baseUrl", "http://127.0.0.1:" + upstream.getAddress().getPort());
			put("apiKey", "test-key");
			put("model", "test-model");
			put("stream", true);
		}}, null, null);
	}

	private static final class DelayedSseHandler implements HttpHandler {
		private final CountDownLatch firstFrameWritten;
		private final CountDownLatch releaseUpstream;
		private final CountDownLatch upstreamClosed;

		private DelayedSseHandler(CountDownLatch firstFrameWritten, CountDownLatch releaseUpstream,
				CountDownLatch upstreamClosed) {
			this.firstFrameWritten = firstFrameWritten;
			this.releaseUpstream = releaseUpstream;
			this.upstreamClosed = upstreamClosed;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			exchange.getRequestBody().close();
			exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
			exchange.sendResponseHeaders(200, 0);
			try {
				exchange.getResponseBody().write("data: {\"choices\":[{\"delta\":{\"content\":\"A\"}}]}\n\n"
						.getBytes(StandardCharsets.UTF_8));
				exchange.getResponseBody().flush();
				firstFrameWritten.countDown();
				releaseUpstream.await(5, TimeUnit.SECONDS);
				exchange.getResponseBody().write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			} finally {
				upstreamClosed.countDown();
				exchange.close();
			}
		}
	}
}
