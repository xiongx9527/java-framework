package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MagicTinyflowSafeUrlImporter {

	private static final long DEFAULT_MAX_BYTES = 10L * 1024L * 1024L;
	private static final int DEFAULT_TIMEOUT_MILLIS = 5000;
	private static final List<String> DEFAULT_MEDIA_TYPES = Arrays.asList("text/plain", "text/markdown",
			"text/html", "text/csv", "application/pdf",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	private final DnsResolver dnsResolver;
	private final HttpTransport transport;
	private final long maxBytes;
	private final int maxRedirects;
	private final int connectTimeoutMillis;
	private final int readTimeoutMillis;
	private final List<String> allowedMediaTypes;

	public MagicTinyflowSafeUrlImporter() {
		this(new SystemDnsResolver(), new DirectSocketHttpTransport(), DEFAULT_MAX_BYTES, 3,
				DEFAULT_TIMEOUT_MILLIS, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MEDIA_TYPES);
	}

	MagicTinyflowSafeUrlImporter(DnsResolver dnsResolver, HttpTransport transport, long maxBytes,
			int maxRedirects, int connectTimeoutMillis, int readTimeoutMillis, List<String> allowedMediaTypes) {
		if (dnsResolver == null || transport == null) throw new IllegalArgumentException("URL importer dependencies must not be null");
		if (maxBytes <= 0L || maxRedirects < 0 || connectTimeoutMillis <= 0 || readTimeoutMillis <= 0) {
			throw new IllegalArgumentException("URL importer limits must be valid");
		}
		if (allowedMediaTypes == null || allowedMediaTypes.isEmpty()) {
			throw new IllegalArgumentException("allowedMediaTypes must not be empty");
		}
		this.dnsResolver = dnsResolver; this.transport = transport; this.maxBytes = maxBytes;
		this.maxRedirects = maxRedirects; this.connectTimeoutMillis = connectTimeoutMillis;
		this.readTimeoutMillis = readTimeoutMillis;
		List<String> normalized = new ArrayList<String>();
		for (String value : allowedMediaTypes) normalized.add(normalizeMediaType(value));
		this.allowedMediaTypes = Collections.unmodifiableList(normalized);
	}

	public ImportedContent fetch(String value) throws IOException {
		URI current = parse(value);
		int redirects = 0;
		while (true) {
			InetAddress[] approved = approve(current);
			HttpResponse response = transport.execute(current, approved, connectTimeoutMillis,
					readTimeoutMillis, maxBytes);
			if (response == null) throw new IOException("URL import returned no response");
			if (redirect(response.status)) {
				if (redirects >= maxRedirects) throw new IllegalStateException("URL redirect limit exceeded");
				String location = response.header("location");
				if (location == null || location.trim().length() == 0) throw new IOException("URL redirect has no location");
				current = parse(current.resolve(location.trim()).toString());
				redirects++;
				continue;
			}
			if (response.status < 200 || response.status >= 300) {
				throw new IOException("URL import failed with status " + response.status);
			}
			if (response.body.length > maxBytes) throw new IllegalArgumentException("URL response exceeds size limit");
			String mediaType = normalizeMediaType(response.header("content-type"));
			if (!allowedMediaTypes.contains(mediaType)) throw new IllegalArgumentException("URL response type is not allowed");
			return new ImportedContent(current.toString(), filename(current), mediaType, response.body);
		}
	}

	private URI parse(String value) {
		try {
			if (value == null || value.trim().length() == 0) throw new IllegalArgumentException("url must not be blank");
			URI uri = new URI(value.trim());
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!"http".equals(scheme) && !"https".equals(scheme)) {
				throw new IllegalArgumentException("Only HTTP and HTTPS URLs are allowed");
			}
			if (uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException("URL host is invalid");
			return uri;
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException("URL is invalid", exception);
		}
	}

	private InetAddress[] approve(URI uri) throws IOException {
		InetAddress[] values = dnsResolver.resolve(uri.getHost());
		if (values == null || values.length == 0) throw new UnknownHostException(uri.getHost());
		InetAddress[] approved = values.clone();
		for (InetAddress address : approved) {
			if (address == null || restricted(address)) throw new SecurityException("URL target address is not public");
		}
		return approved;
	}

	private boolean restricted(InetAddress address) {
		if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
				|| address.isSiteLocalAddress() || address.isMulticastAddress()) return true;
		byte[] bytes = address.getAddress();
		if (bytes.length == 4) return restrictedIpv4(bytes);
		if (bytes.length == 16) {
			int first = bytes[0] & 0xff;
			if ((first & 0xfe) == 0xfc || first == 0xff) return true;
			if (mappedIpv4(bytes)) return restrictedIpv4(Arrays.copyOfRange(bytes, 12, 16));
		}
		return false;
	}

	private boolean restrictedIpv4(byte[] bytes) {
		int first = bytes[0] & 0xff;
		int second = bytes[1] & 0xff;
		return first == 0 || first == 10 || first == 127 || first >= 224
				|| first == 100 && second >= 64 && second <= 127
				|| first == 169 && second == 254
				|| first == 172 && second >= 16 && second <= 31
				|| first == 192 && (second == 0 || second == 168)
				|| first == 198 && (second == 18 || second == 19);
	}

	private boolean mappedIpv4(byte[] bytes) {
		for (int index = 0; index < 10; index++) if (bytes[index] != 0) return false;
		return (bytes[10] & 0xff) == 0xff && (bytes[11] & 0xff) == 0xff;
	}

	private boolean redirect(int status) {
		return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
	}

	private String normalizeMediaType(String value) {
		if (value == null) return "";
		int separator = value.indexOf(';');
		return (separator < 0 ? value : value.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
	}

	private String filename(URI uri) {
		String path = uri.getPath();
		int separator = path == null ? -1 : path.lastIndexOf('/');
		String value = separator < 0 ? path : path.substring(separator + 1);
		return value == null || value.length() == 0 ? "url-import" : value;
	}

	public interface DnsResolver {
		InetAddress[] resolve(String host) throws IOException;
	}

	public interface HttpTransport {
		HttpResponse execute(URI uri, InetAddress[] approvedAddresses, int connectTimeoutMillis,
				int readTimeoutMillis, long maxBytes) throws IOException;
	}

	public static final class HttpResponse {
		private final int status;
		private final Map<String, String> headers;
		private final byte[] body;
		public HttpResponse(int status, Map<String, String> headers, byte[] body) {
			this.status = status;
			Map<String, String> normalized = new LinkedHashMap<String, String>();
			if (headers != null) for (Map.Entry<String, String> entry : headers.entrySet()) {
				normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
			}
			this.headers = Collections.unmodifiableMap(normalized);
			this.body = body == null ? new byte[0] : body.clone();
		}
		private String header(String name) { return headers.get(name.toLowerCase(Locale.ROOT)); }
	}

	public static final class ImportedContent {
		private final String finalUrl;
		private final String filename;
		private final String mediaType;
		private final byte[] content;
		private ImportedContent(String finalUrl, String filename, String mediaType, byte[] content) {
			this.finalUrl = finalUrl; this.filename = filename; this.mediaType = mediaType; this.content = content.clone();
		}
		public String getFinalUrl() { return finalUrl; }
		public String getFilename() { return filename; }
		public String getMediaType() { return mediaType; }
		public byte[] getContent() { return content.clone(); }
	}

	private static final class SystemDnsResolver implements DnsResolver {
		@Override public InetAddress[] resolve(String host) throws IOException { return InetAddress.getAllByName(host); }
	}

	private static final class DirectSocketHttpTransport implements HttpTransport {
		@Override public HttpResponse execute(URI uri, InetAddress[] approvedAddresses, int connectTimeoutMillis,
				int readTimeoutMillis, long maxBytes) throws IOException {
			int port = uri.getPort() >= 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
			Socket socket = new Socket();
			try {
				socket.connect(new InetSocketAddress(approvedAddresses[0], port), connectTimeoutMillis);
				socket.setSoTimeout(readTimeoutMillis);
				if ("https".equalsIgnoreCase(uri.getScheme())) socket = secure(socket, uri.getHost(), port);
				writeRequest(socket.getOutputStream(), uri, port);
				return readResponse(socket.getInputStream(), maxBytes);
			} finally {
				try { socket.close(); } catch (IOException ignored) { }
			}
		}

		private Socket secure(Socket plain, String host, int port) throws IOException {
			SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket socket = (SSLSocket) factory.createSocket(plain, host, port, true);
			SSLParameters parameters = socket.getSSLParameters();
			parameters.setEndpointIdentificationAlgorithm("HTTPS");
			socket.setSSLParameters(parameters);
			socket.startHandshake();
			return socket;
		}

		private void writeRequest(OutputStream output, URI uri, int port) throws IOException {
			String target = uri.getRawPath();
			if (target == null || target.length() == 0) target = "/";
			if (uri.getRawQuery() != null) target += "?" + uri.getRawQuery();
			boolean defaultPort = "https".equalsIgnoreCase(uri.getScheme()) ? port == 443 : port == 80;
			String host = uri.getHost() + (defaultPort ? "" : ":" + port);
			String request = "GET " + target + " HTTP/1.1\r\nHost: " + host
					+ "\r\nAccept: */*\r\nAccept-Encoding: identity\r\nConnection: close\r\n\r\n";
			output.write(request.getBytes(StandardCharsets.ISO_8859_1)); output.flush();
		}

		private HttpResponse readResponse(InputStream input, long maxBytes) throws IOException {
			String statusLine = line(input, 8192);
			String[] statusParts = statusLine.split(" ", 3);
			if (statusParts.length < 2) throw new IOException("Invalid HTTP status line");
			int status;
			try { status = Integer.parseInt(statusParts[1]); }
			catch (NumberFormatException exception) { throw new IOException("Invalid HTTP status", exception); }
			Map<String, String> headers = new LinkedHashMap<String, String>();
			int headerBytes = statusLine.length();
			while (true) {
				String line = line(input, 8192); headerBytes += line.length();
				if (headerBytes > 65536) throw new IOException("HTTP headers exceed limit");
				if (line.length() == 0) break;
				int separator = line.indexOf(':');
				if (separator <= 0) throw new IOException("Invalid HTTP header");
				String name = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
				String value = line.substring(separator + 1).trim();
				String previous = headers.get(name);
				if ("content-length".equals(name) && previous != null && !previous.equals(value)) {
					throw new IOException("Conflicting Content-Length headers");
				}
				headers.put(name, previous == null || "content-length".equals(name) ? value : previous + "," + value);
			}
			String transferEncoding = header(headers, "transfer-encoding");
			byte[] body = transferEncoding != null && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked")
					? readChunked(input, maxBytes) : readBody(input, header(headers, "content-length"), maxBytes);
			return new HttpResponse(status, headers, body);
		}

		private byte[] readBody(InputStream input, String contentLength, long maxBytes) throws IOException {
			long expected = -1L;
			if (contentLength != null) {
				try { expected = Long.parseLong(contentLength); }
				catch (NumberFormatException exception) { throw new IOException("Invalid Content-Length", exception); }
				if (expected < 0L || expected > maxBytes) throw new IllegalArgumentException("URL response exceeds size limit");
			}
			return readLimited(input, expected, maxBytes);
		}

		private byte[] readChunked(InputStream input, long maxBytes) throws IOException {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			while (true) {
				String sizeLine = line(input, 128);
				int extension = sizeLine.indexOf(';');
				String value = extension < 0 ? sizeLine : sizeLine.substring(0, extension);
				int size;
				try { size = Integer.parseInt(value.trim(), 16); }
				catch (NumberFormatException exception) { throw new IOException("Invalid chunk size", exception); }
				if (size == 0) { while (line(input, 8192).length() > 0) { } return output.toByteArray(); }
				if ((long) output.size() + size > maxBytes) throw new IllegalArgumentException("URL response exceeds size limit");
				copyExact(input, output, size);
				if (input.read() != '\r' || input.read() != '\n') throw new IOException("Invalid chunk terminator");
			}
		}

		private byte[] readLimited(InputStream input, long expected, long maxBytes) throws IOException {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			long remaining = expected;
			while (expected < 0L || remaining > 0L) {
				int requested = expected < 0L ? buffer.length : (int) Math.min(buffer.length, remaining);
				int length = input.read(buffer, 0, requested);
				if (length < 0) break;
				if ((long) output.size() + length > maxBytes) throw new IllegalArgumentException("URL response exceeds size limit");
				output.write(buffer, 0, length); if (expected >= 0L) remaining -= length;
			}
			if (expected >= 0L && remaining != 0L) throw new EOFException("HTTP response ended early");
			return output.toByteArray();
		}

		private void copyExact(InputStream input, OutputStream output, int length) throws IOException {
			byte[] buffer = new byte[8192]; int remaining = length;
			while (remaining > 0) { int count = input.read(buffer, 0, Math.min(buffer.length, remaining));
				if (count < 0) throw new EOFException("HTTP chunk ended early"); output.write(buffer, 0, count); remaining -= count; }
		}

		private String line(InputStream input, int limit) throws IOException {
			ByteArrayOutputStream output = new ByteArrayOutputStream(); int previous = -1;
			while (true) { int value = input.read(); if (value < 0) throw new EOFException("HTTP line ended early");
				if (previous == '\r' && value == '\n') { byte[] bytes = output.toByteArray();
					return new String(bytes, 0, Math.max(0, bytes.length - 1), StandardCharsets.ISO_8859_1); }
				output.write(value); if (output.size() > limit) throw new IOException("HTTP line exceeds limit"); previous = value; }
		}

		private String header(Map<String, String> headers, String name) {
			for (Map.Entry<String, String> entry : headers.entrySet()) if (name.equalsIgnoreCase(entry.getKey())) return entry.getValue();
			return null;
		}
	}
}
