package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineOfficialAiWebFluxControllerTest {

	@Test
	void mapsUpstreamAuthenticationFailureToBadGateway() {
		TinyEngineOpenAiCompatibleGateway gateway = new TinyEngineOpenAiCompatibleGateway() {
			@Override
			public TinyEngineOfficialAiResponse execute(TinyEngineOfficialAiRequest request) {
				return new TinyEngineOfficialAiResponse(401, new HttpHeaders(),
						"{\"error\":{\"message\":\"invalid key\"}}".getBytes(StandardCharsets.UTF_8));
			}

			@Override
			public void stream(TinyEngineOfficialAiRequest request, TinyEngineSseConsumer consumer) {
				throw new UnsupportedOperationException();
			}
		};
		TinyEngineOfficialAiWebFluxController controller = new TinyEngineOfficialAiWebFluxController(gateway,
				new TinyEngineAiCredentialCipher() {
					@Override public String encrypt(String plainText) { return plainText; }
					@Override public String decrypt(String token) { return token; }
				});
		Map<String, Object> request = new LinkedHashMap<String, Object>();
		request.put("baseUrl", "http://example.test");
		request.put("apiKey", "model-key");
		request.put("model", "gpt-test");

		org.springframework.http.ResponseEntity<?> response = controller.chat(request, "Bearer engine-token").block();

		assertEquals(502, response.getStatusCodeValue());
	}

	@Test
	void returnsRawSseFramesWithNoBufferingHeadersAndClosesGatewayOnCancellation() {
		AtomicBoolean closed = new AtomicBoolean();
		TinyEngineOpenAiCompatibleGateway gateway = new TinyEngineOpenAiCompatibleGateway() {
			@Override
			public TinyEngineOfficialAiResponse execute(TinyEngineOfficialAiRequest request) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void stream(TinyEngineOfficialAiRequest request, TinyEngineSseConsumer consumer) {
				consumer.onSession(() -> closed.set(true));
				consumer.accept("data: {\"vendor\":{\"reasoning\":\"kept\"}}\n\n".getBytes(StandardCharsets.UTF_8));
			}
		};
		TinyEngineOfficialAiWebFluxController controller = new TinyEngineOfficialAiWebFluxController(gateway,
				new TinyEngineAiCredentialCipher() {
					@Override public String encrypt(String plainText) { return plainText; }
					@Override public String decrypt(String token) { return token; }
				});

		Map<String, Object> request = new LinkedHashMap<String, Object>();
		request.put("baseUrl", "http://example.test");
		request.put("apiKey", "model-key");
		request.put("model", "gpt-test");
		request.put("stream", true);
		org.springframework.http.ResponseEntity<?> response = controller.chat(request, "Bearer test-key").block();
		assertEquals(MediaType.TEXT_EVENT_STREAM, response.getHeaders().getContentType());
		assertEquals("no-cache", response.getHeaders().getCacheControl());
		assertEquals("no", response.getHeaders().getFirst("X-Accel-Buffering"));

		@SuppressWarnings("unchecked")
		Flux<DataBuffer> body = (Flux<DataBuffer>) response.getBody();
		StepVerifier.create(body.map(buffer -> {
			try {
				return buffer.toString(StandardCharsets.UTF_8);
			} finally {
				DataBufferUtils.release(buffer);
			}
		}))
				.expectNext("data: {\"vendor\":{\"reasoning\":\"kept\"}}\n\n")
				.thenCancel()
				.verify();
		assertTrue(closed.get());
	}
}
