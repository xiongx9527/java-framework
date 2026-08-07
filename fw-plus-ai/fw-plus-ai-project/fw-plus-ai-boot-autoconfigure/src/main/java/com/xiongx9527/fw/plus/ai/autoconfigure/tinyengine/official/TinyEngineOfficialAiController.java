package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;

@RestController
@RequestMapping("/app-center/api")
public class TinyEngineOfficialAiController {

	private final TinyEngineOpenAiCompatibleGateway gateway;
	private final TinyEngineAiCredentialCipher credentialCipher;
	private final TinyEngineOfficialRagGateway ragGateway;

	public TinyEngineOfficialAiController(TinyEngineOpenAiCompatibleGateway gateway,
			TinyEngineAiCredentialCipher credentialCipher) {
		this(gateway, credentialCipher, null);
	}

	public TinyEngineOfficialAiController(TinyEngineOpenAiCompatibleGateway gateway,
			TinyEngineAiCredentialCipher credentialCipher, TinyEngineOfficialRagGateway ragGateway) {
		this.gateway = gateway;
		this.credentialCipher = credentialCipher;
		this.ragGateway = ragGateway;
	}

	@PostMapping({"/ai/chat", "/chat/completions"})
	public ResponseEntity<?> chat(@RequestBody Map<String, Object> body,
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		TinyEngineOfficialAiRequest request;
		try {
			request = TinyEngineOfficialAiRequest.parse(body, authorization, credentialCipher);
		} catch (IllegalArgumentException ex) {
			return jsonError(400, "TinyEngine AI credential is invalid");
		} catch (IllegalStateException ex) {
			return jsonError(500, "TinyEngine AI credential cipher is unavailable");
		}
		if (isStream(body)) {
			return streamResponse(request);
		}
		TinyEngineOfficialAiResponse response = gateway.execute(request);
		return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(response.getBody());
	}

	private ResponseEntity<StreamingResponseBody> streamResponse(final TinyEngineOfficialAiRequest request) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_EVENT_STREAM);
		headers.setCacheControl("no-cache");
		headers.set("X-Accel-Buffering", "no");
		return ResponseEntity.ok().headers(headers).body(new StreamingResponseBody() {
			@Override
			public void writeTo(final OutputStream outputStream) {
				final AtomicReference<TinyEngineSseSession> session = new AtomicReference<TinyEngineSseSession>();
				try {
					gateway.stream(request, new TinyEngineSseConsumer() {
						@Override
						public void onSession(TinyEngineSseSession value) {
							session.set(value);
						}

						@Override
						public void accept(byte[] bytes) {
							try {
								outputStream.write(bytes);
								outputStream.flush();
							} catch (IOException ex) {
								TinyEngineSseSession active = session.get();
								if (active != null) {
									active.close();
								}
								throw new IllegalStateException("TinyEngine AI client disconnected", ex);
							}
						}
					});
				} finally {
					TinyEngineSseSession active = session.get();
					if (active != null) {
						active.close();
					}
				}
			}
		});
	}

	private boolean isStream(Map<String, Object> body) {
		return body != null && Boolean.TRUE.equals(body.get("stream"));
	}

	@PostMapping("/encrypt-key")
	public ResponseEntity<byte[]> encryptKey(@RequestBody Map<String, Object> body) {
		String apiKey = body == null || body.get("apiKey") == null ? null : String.valueOf(body.get("apiKey"));
		if (apiKey == null || apiKey.trim().length() == 0) {
			return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
					.body("{\"code\":\"400\",\"success\":false,\"message\":\"TinyEngine AI apiKey is required\"}"
							.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
	}

	@PostMapping("/ai/search")
	public ResponseEntity<byte[]> search(@RequestBody Map<String, Object> body) {
		if (ragGateway == null) {
			return jsonError(503, "TinyEngine AI RAG is unavailable");
		}
		TinyEngineOfficialAiResponse response = ragGateway.search(body);
		return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(response.getBody());
	}

	private ResponseEntity<byte[]> jsonError(int statusCode, String message) {
		String response = "{\"code\":\"" + statusCode + "\",\"success\":false,\"message\":\"" + message + "\"}";
		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON)
				.body(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}
}
