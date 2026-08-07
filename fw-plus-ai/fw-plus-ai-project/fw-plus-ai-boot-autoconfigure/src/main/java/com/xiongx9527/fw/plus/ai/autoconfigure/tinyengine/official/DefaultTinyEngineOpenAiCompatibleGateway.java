package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultTinyEngineOpenAiCompatibleGateway implements TinyEngineOpenAiCompatibleGateway {

	private static final List<String> FORWARDED_RESPONSE_HEADERS = Arrays.asList(
			HttpHeaders.CONTENT_TYPE, HttpHeaders.CACHE_CONTROL, "X-Request-Id");
	private final TinyEngineAiProperties properties;
	private final ObjectMapper objectMapper;

	public DefaultTinyEngineOpenAiCompatibleGateway(TinyEngineAiProperties properties, ObjectMapper objectMapper) {
		this.properties = properties == null ? new TinyEngineAiProperties() : properties;
		this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
	}

	@Override
	public TinyEngineOfficialAiResponse execute(TinyEngineOfficialAiRequest request) {
		try {
			validate(request);
			byte[] requestBody = objectMapper.writeValueAsBytes(request.getForwardBody());
			HttpURLConnection connection = openConnection(normalizeChatCompletionsUrl(request.getBaseUrl()));
			connection.setRequestMethod("POST");
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(properties.getOfficial().getConnectTimeoutMillis());
			connection.setReadTimeout(properties.getOfficial().getReadTimeoutMillis());
			connection.setDoOutput(true);
			connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
			connection.setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
			connection.setRequestProperty(HttpHeaders.AUTHORIZATION, request.getAuthorization());
			try (OutputStream outputStream = connection.getOutputStream()) {
				outputStream.write(requestBody);
			}
			int statusCode = connection.getResponseCode();
			return new TinyEngineOfficialAiResponse(statusCode, responseHeaders(connection),
					readLimited(responseStream(connection), properties.getOfficial().getMaxResponseBytes()));
		} catch (SocketTimeoutException ex) {
			return error(504, "TinyEngine AI upstream timeout");
		} catch (IllegalArgumentException ex) {
			return error(400, ex.getMessage());
		} catch (IOException ex) {
			return error(502, "TinyEngine AI upstream connection failed");
		}
	}

	@Override
	public void stream(TinyEngineOfficialAiRequest request, TinyEngineSseConsumer consumer) {
		HttpURLConnection connection = null;
		final AtomicBoolean closed = new AtomicBoolean(false);
		try {
			validate(request);
			if (consumer == null) {
				throw new IllegalArgumentException("TinyEngine AI SSE consumer is required");
			}
			byte[] requestBody = objectMapper.writeValueAsBytes(request.getForwardBody());
			connection = openConnection(normalizeChatCompletionsUrl(request.getBaseUrl()));
			connection.setRequestMethod("POST");
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(properties.getOfficial().getConnectTimeoutMillis());
			connection.setReadTimeout(properties.getOfficial().getReadTimeoutMillis());
			connection.setDoOutput(true);
			connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
			connection.setRequestProperty(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);
			connection.setRequestProperty(HttpHeaders.AUTHORIZATION, request.getAuthorization());
			final HttpURLConnection activeConnection = connection;
			consumer.onSession(new TinyEngineSseSession() {
				@Override
				public void close() {
					closed.set(true);
					activeConnection.disconnect();
				}
			});
			try (OutputStream outputStream = connection.getOutputStream()) {
				outputStream.write(requestBody);
			}
			try (InputStream inputStream = responseStream(connection)) {
				if (inputStream == null) {
					return;
				}
				readSseFrames(inputStream, consumer);
			}
		} catch (IOException ex) {
			if (!closed.get()) {
				throw new IllegalStateException("TinyEngine AI upstream streaming failed", ex);
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private void readSseFrames(InputStream inputStream, TinyEngineSseConsumer consumer) throws IOException {
		ByteArrayOutputStream pending = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int read;
		while ((read = inputStream.read(buffer)) != -1) {
			pending.write(buffer, 0, read);
			byte[] bytes = pending.toByteArray();
			int frameEnd;
			int consumed = 0;
			while ((frameEnd = frameEnd(bytes, consumed)) >= 0) {
				byte[] frame = new byte[frameEnd - consumed];
				System.arraycopy(bytes, consumed, frame, 0, frame.length);
				consumer.accept(frame);
				consumed = frameEnd;
			}
			if (consumed > 0) {
				pending.reset();
				if (consumed < bytes.length) {
					pending.write(bytes, consumed, bytes.length - consumed);
				}
			}
		}
		byte[] remaining = pending.toByteArray();
		if (remaining.length > 0) {
			consumer.accept(remaining);
		}
	}

	private int frameEnd(byte[] bytes, int start) {
		for (int i = start; i < bytes.length - 1; i++) {
			if (bytes[i] == '\n' && bytes[i + 1] == '\n') {
				return i + 2;
			}
			if (i < bytes.length - 3 && bytes[i] == '\r' && bytes[i + 1] == '\n'
					&& bytes[i + 2] == '\r' && bytes[i + 3] == '\n') {
				return i + 4;
			}
		}
		return -1;
	}

	private HttpURLConnection openConnection(String targetUrl) throws IOException {
		return (HttpURLConnection) new URL(targetUrl).openConnection();
	}

	private void validate(TinyEngineOfficialAiRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("TinyEngine AI request is required");
		}
		if (isBlank(request.getBaseUrl())) {
			throw new IllegalArgumentException("TinyEngine AI baseUrl is required");
		}
		if (isBlank(request.getModel())) {
			throw new IllegalArgumentException("TinyEngine AI model is required");
		}
		if (isBlank(request.getApiKey())) {
			throw new IllegalArgumentException("TinyEngine AI apiKey is required");
		}
	}

	private String normalizeChatCompletionsUrl(String baseUrl) {
		String normalized = baseUrl.trim();
		if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
			normalized = "https://" + normalized;
		}
		try {
			URI uri = new URI(normalized);
			String scheme = uri.getScheme();
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				throw new IllegalArgumentException("TinyEngine AI baseUrl only supports HTTP/HTTPS");
			}
		} catch (URISyntaxException ex) {
			throw new IllegalArgumentException("TinyEngine AI baseUrl is invalid");
		}
		if (normalized.indexOf("/chat/completions") >= 0 || normalized.indexOf("/v1/chat/completions") >= 0) {
			return stripForceExactSuffix(normalized);
		}
		if (normalized.indexOf("v1") >= 0) {
			return trimTrailingSlash(stripForceExactSuffix(normalized)) + "/chat/completions";
		}
		return trimTrailingSlash(stripForceExactSuffix(normalized)) + "/v1/chat/completions";
	}

	private String stripForceExactSuffix(String value) {
		return value.endsWith("#") ? value.substring(0, value.length() - 1) : value;
	}

	private String trimTrailingSlash(String value) {
		String result = value;
		while (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	private InputStream responseStream(HttpURLConnection connection) throws IOException {
		return connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
	}

	private HttpHeaders responseHeaders(HttpURLConnection connection) {
		HttpHeaders headers = new HttpHeaders();
		for (String header : FORWARDED_RESPONSE_HEADERS) {
			String value = connection.getHeaderField(header);
			if (!isBlank(value)) {
				headers.add(header, value);
			}
		}
		return headers;
	}

	private byte[] readLimited(InputStream inputStream, int maxResponseBytes) throws IOException {
		if (inputStream == null) {
			return new byte[0];
		}
		int maxBytes = maxResponseBytes <= 0 ? 10 * 1024 * 1024 : maxResponseBytes;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int read;
		while ((read = inputStream.read(buffer)) != -1) {
			if (outputStream.size() + read > maxBytes) {
				throw new IOException("TinyEngine AI upstream response is too large");
			}
			outputStream.write(buffer, 0, read);
		}
		return outputStream.toByteArray();
	}

	private TinyEngineOfficialAiResponse error(int statusCode, String message) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("code", String.valueOf(statusCode));
		body.put("message", message);
		try {
			return new TinyEngineOfficialAiResponse(statusCode, headers, objectMapper.writeValueAsBytes(body));
		} catch (JsonProcessingException ex) {
			return new TinyEngineOfficialAiResponse(statusCode, headers,
					("{\"code\":\"" + statusCode + "\",\"message\":\"TinyEngine AI request failed\"}")
							.getBytes(StandardCharsets.UTF_8));
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
