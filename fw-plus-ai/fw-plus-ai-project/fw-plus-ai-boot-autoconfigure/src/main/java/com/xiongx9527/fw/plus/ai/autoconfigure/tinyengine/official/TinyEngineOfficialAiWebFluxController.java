package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/app-center/api")
public class TinyEngineOfficialAiWebFluxController {

	private final TinyEngineOpenAiCompatibleGateway gateway;
	private final TinyEngineAiCredentialCipher credentialCipher;
	private final TinyEngineOfficialRagGateway ragGateway;

	public TinyEngineOfficialAiWebFluxController(TinyEngineOpenAiCompatibleGateway gateway,
			TinyEngineAiCredentialCipher credentialCipher) {
		this(gateway, credentialCipher, null);
	}

	public TinyEngineOfficialAiWebFluxController(TinyEngineOpenAiCompatibleGateway gateway,
			TinyEngineAiCredentialCipher credentialCipher, TinyEngineOfficialRagGateway ragGateway) {
		this.gateway = gateway;
		this.credentialCipher = credentialCipher;
		this.ragGateway = ragGateway;
	}

	@PostMapping({"/ai/chat", "/chat/completions"})
	public Mono<ResponseEntity<?>> chat(@RequestBody Map<String, Object> body,
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return Mono.fromCallable(() -> {
			TinyEngineOfficialAiRequest request;
			try {
				request = request(body, authorization);
			} catch (IllegalArgumentException ex) {
				return jsonError(400, "TinyEngine AI credential is invalid");
			} catch (IllegalStateException ex) {
				return jsonError(500, "TinyEngine AI credential cipher is unavailable");
			}
			if (isStream(body)) {
				return sseResponse(request);
			}
			TinyEngineOfficialAiResponse response = gateway.execute(request);
				return (ResponseEntity<?>) ResponseEntity.status(response.getClientStatusCode()).headers(response.getHeaders())
					.body(response.getBody());
		}).subscribeOn(Schedulers.boundedElastic());
	}

	@PostMapping("/encrypt-key")
	public Mono<ResponseEntity<byte[]>> encryptKey(@RequestBody Map<String, Object> body) {
		return Mono.fromCallable(() -> {
			String apiKey = body == null || body.get("apiKey") == null ? null : String.valueOf(body.get("apiKey"));
			if (apiKey == null || apiKey.trim().length() == 0) {
				return jsonError(400, "TinyEngine AI apiKey is required");
			}
			String token;
			try {
				token = credentialCipher.encrypt(apiKey);
			} catch (RuntimeException ex) {
				return jsonError(500, "TinyEngine AI credential cipher is unavailable");
			}
			String response = "{\"code\":\"0\",\"success\":true,\"data\":{\"token\":\"" + token + "\"}}";
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
					.body(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}).subscribeOn(Schedulers.boundedElastic());
	}

	@PostMapping("/ai/search")
	public Mono<ResponseEntity<byte[]>> search(@RequestBody Map<String, Object> body) {
		return Mono.fromCallable(() -> {
			if (ragGateway == null) {
				return jsonError(503, "TinyEngine AI RAG is unavailable");
			}
			TinyEngineOfficialAiResponse response = ragGateway.search(body);
			return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(response.getBody());
		}).subscribeOn(Schedulers.boundedElastic());
	}

	private ResponseEntity<Flux<DataBuffer>> sseResponse(final TinyEngineOfficialAiRequest request) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_EVENT_STREAM);
		headers.setCacheControl("no-cache");
		headers.set("X-Accel-Buffering", "no");
		final AtomicReference<TinyEngineSseSession> session = new AtomicReference<TinyEngineSseSession>();
		Flux<DataBuffer> body = Flux.<DataBuffer>create(sink -> {
			sink.onCancel(() -> close(session));
			sink.onDispose(() -> close(session));
			try {
				gateway.stream(request, new TinyEngineSseConsumer() {
					@Override
					public void onSession(TinyEngineSseSession value) {
						session.set(value);
					}

					@Override
					public void accept(byte[] bytes) {
						if (!sink.isCancelled()) {
							sink.next(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
						}
					}
				});
				if (!sink.isCancelled()) {
					sink.complete();
				}
			} catch (RuntimeException ex) {
				if (!sink.isCancelled()) {
					sink.error(ex);
				}
			} finally {
				close(session);
			}
		}, FluxSink.OverflowStrategy.ERROR).subscribeOn(Schedulers.boundedElastic());
		return ResponseEntity.ok().headers(headers).body(body);
	}

	private TinyEngineOfficialAiRequest request(Map<String, Object> body, String authorization) {
		return TinyEngineOfficialAiRequest.parse(body, authorization, credentialCipher);
	}

	private boolean isStream(Map<String, Object> body) {
		return body != null && Boolean.TRUE.equals(body.get("stream"));
	}

	private ResponseEntity<byte[]> jsonError(int statusCode, String message) {
		String response = "{\"code\":\"" + statusCode + "\",\"success\":false,\"message\":\"" + message + "\"}";
		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON)
				.body(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private void close(AtomicReference<TinyEngineSseSession> session) {
		TinyEngineSseSession active = session.getAndSet(null);
		if (active != null) {
			active.close();
		}
	}
}
