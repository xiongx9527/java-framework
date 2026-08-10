package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MagicTinyflowSafeUrlImporterTest {

	@Test
	public void shouldRejectUnsupportedProtocolAndPrivateOrMetadataAddressesBeforeTransport() throws Exception {
		RecordingTransport transport = new RecordingTransport();
		MapResolver resolver = new MapResolver();
		resolver.put("loop.test", "127.0.0.1");
		resolver.put("private.test", "10.10.0.1");
		resolver.put("private-172.test", "172.16.0.1");
		resolver.put("cgnat.test", "100.64.0.1");
		resolver.put("metadata.test", "169.254.169.254");
		resolver.put("ula.test", "fd00::1");
		resolver.put("mapped.test", mappedIpv4("10.0.0.1"));
		resolver.put("mixed.test", InetAddress.getByName("93.184.216.34"), InetAddress.getByName("192.168.1.8"));
		MagicTinyflowSafeUrlImporter importer = importer(resolver, transport, 128L, 2);

		assertThrows(IllegalArgumentException.class, () -> importer.fetch("ftp://public.test/file.txt"));
		assertThrows(SecurityException.class, () -> importer.fetch("http://loop.test/file.txt"));
		assertThrows(SecurityException.class, () -> importer.fetch("http://private.test/file.txt"));
		assertThrows(SecurityException.class, () -> importer.fetch("http://private-172.test/file.txt"));
		assertThrows(SecurityException.class, () -> importer.fetch("http://cgnat.test/file.txt"));
		assertThrows(SecurityException.class, () -> importer.fetch("http://metadata.test/latest/meta-data"));
		assertThrows(SecurityException.class, () -> importer.fetch("https://ula.test/file.txt"));
		assertThrows(SecurityException.class, () -> importer.fetch("https://mapped.test/file.txt"));
		assertThrows(SecurityException.class, () -> importer.fetch("https://mixed.test/file.txt"));
		assertEquals(0, transport.calls);
	}

	@Test
	public void shouldRevalidateDnsOnEveryRedirectAndBlockRebinding() throws Exception {
		SequencedResolver resolver = new SequencedResolver("93.184.216.34", "10.0.0.8");
		RecordingTransport transport = new RecordingTransport();
		transport.responses.add(response(302, "text/plain", new byte[0], "/next"));
		MagicTinyflowSafeUrlImporter importer = importer(resolver, transport, 128L, 3);

		assertThrows(SecurityException.class, () -> importer.fetch("https://public.test/start"));

		assertEquals(1, transport.calls);
		assertEquals("93.184.216.34", transport.approved.get(0).getHostAddress());
		assertEquals(2, resolver.calls);
	}

	@Test
	public void shouldBlockPrivateRedirectAndRedirectLoops() throws Exception {
		MapResolver resolver = new MapResolver();
		resolver.put("public.test", "93.184.216.34"); resolver.put("private.test", "192.168.1.8");
		RecordingTransport privateRedirect = new RecordingTransport();
		privateRedirect.responses.add(response(302, "text/plain", new byte[0], "http://private.test/secret"));
		assertThrows(SecurityException.class,
				() -> importer(resolver, privateRedirect, 128L, 3).fetch("https://public.test/start"));

		RecordingTransport loop = new RecordingTransport();
		loop.responses.add(response(302, "text/plain", new byte[0], "/1"));
		loop.responses.add(response(302, "text/plain", new byte[0], "/2"));
		loop.responses.add(response(302, "text/plain", new byte[0], "/3"));
		assertThrows(IllegalStateException.class,
				() -> importer(resolver, loop, 128L, 2).fetch("https://public.test/start"));
	}

	@Test
	public void shouldEnforceResponseTypeSizeAndTimeout() throws Exception {
		MapResolver resolver = new MapResolver();
		resolver.put("public.test", "93.184.216.34");
		RecordingTransport wrongType = new RecordingTransport();
		wrongType.responses.add(response(200, "application/octet-stream", new byte[]{1}, null));
		assertThrows(IllegalArgumentException.class,
				() -> importer(resolver, wrongType, 4L, 1).fetch("https://public.test/file.bin"));

		RecordingTransport oversized = new RecordingTransport();
		oversized.responses.add(response(200, "text/plain; charset=UTF-8", new byte[]{1, 2, 3, 4, 5}, null));
		assertThrows(IllegalArgumentException.class,
				() -> importer(resolver, oversized, 4L, 1).fetch("https://public.test/file.txt"));

		RecordingTransport timeout = new RecordingTransport();
		timeout.failure = new SocketTimeoutException("read timed out");
		assertThrows(SocketTimeoutException.class,
				() -> importer(resolver, timeout, 4L, 1).fetch("https://public.test/file.txt"));
	}

	@Test
	public void shouldReturnSafeImportedContentForAllowedResponse() throws Exception {
		MapResolver resolver = new MapResolver();
		resolver.put("public.test", "93.184.216.34");
		RecordingTransport transport = new RecordingTransport();
		transport.responses.add(response(200, "text/markdown; charset=UTF-8",
				"# title".getBytes(StandardCharsets.UTF_8), null));

		MagicTinyflowSafeUrlImporter.ImportedContent result = importer(resolver, transport, 128L, 2)
				.fetch("https://public.test/docs/manual.md");

		assertEquals("text/markdown", result.getMediaType());
		assertEquals("manual.md", result.getFilename());
		assertEquals("https://public.test/docs/manual.md", result.getFinalUrl());
		assertTrue(Arrays.equals("# title".getBytes(StandardCharsets.UTF_8), result.getContent()));
	}

	@Test
	public void shouldRejectDeclaredContentLengthAboveLimitBeforeReadingBody() throws Exception {
		String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n"
				+ "Connection: close\r\n\r\n";
		try (RawHttpServer server = new RawHttpServer(response.getBytes(StandardCharsets.ISO_8859_1))) {
			assertThrows(IllegalArgumentException.class, () -> directTransport().execute(server.uri(),
					new InetAddress[]{server.address()}, 1000, 1000, 4L));
		}
	}

	@Test
	public void shouldRejectActualResponseStreamAboveLimitWithoutContentLength() throws Exception {
		String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\n12345";
		try (RawHttpServer server = new RawHttpServer(response.getBytes(StandardCharsets.ISO_8859_1))) {
			assertThrows(IllegalArgumentException.class, () -> directTransport().execute(server.uri(),
					new InetAddress[]{server.address()}, 1000, 1000, 4L));
		}
	}

	@Test
	public void shouldRejectChunkedResponseAboveLimit() throws Exception {
		String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nTransfer-Encoding: chunked\r\n"
				+ "Connection: close\r\n\r\n5\r\n12345\r\n0\r\n\r\n";
		try (RawHttpServer server = new RawHttpServer(response.getBytes(StandardCharsets.ISO_8859_1))) {
			assertThrows(IllegalArgumentException.class, () -> directTransport().execute(server.uri(),
					new InetAddress[]{server.address()}, 1000, 1000, 4L));
		}
	}

	@Test
	public void shouldFailClosedForConflictingContentLengthHeaders() throws Exception {
		String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n"
				+ "Content-Length: 1\r\nConnection: close\r\n\r\n1";
		try (RawHttpServer server = new RawHttpServer(response.getBytes(StandardCharsets.ISO_8859_1))) {
			assertThrows(IOException.class, () -> directTransport().execute(server.uri(),
					new InetAddress[]{server.address()}, 1000, 1000, 8L));
		}
	}

	private MagicTinyflowSafeUrlImporter importer(MagicTinyflowSafeUrlImporter.DnsResolver resolver,
			MagicTinyflowSafeUrlImporter.HttpTransport transport, long maxBytes, int redirects) {
		return new MagicTinyflowSafeUrlImporter(resolver, transport, maxBytes, redirects, 100, 100,
				Arrays.asList("text/plain", "text/markdown", "text/html", "application/pdf"));
	}

	private MagicTinyflowSafeUrlImporter.HttpResponse response(int status, String mediaType, byte[] content,
			String location) {
		Map<String, String> headers = new LinkedHashMap<String, String>();
		if (mediaType != null) headers.put("Content-Type", mediaType);
		if (location != null) headers.put("Location", location);
		return new MagicTinyflowSafeUrlImporter.HttpResponse(status, headers, content);
	}

	private MagicTinyflowSafeUrlImporter.HttpTransport directTransport() throws Exception {
		Class<?> type = Class.forName(MagicTinyflowSafeUrlImporter.class.getName() + "$DirectSocketHttpTransport");
		java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor();
		constructor.setAccessible(true);
		return (MagicTinyflowSafeUrlImporter.HttpTransport) constructor.newInstance();
	}

	private static InetAddress mappedIpv4(String value) throws Exception {
		byte[] ipv4 = InetAddress.getByName(value).getAddress();
		byte[] mapped = new byte[16];
		mapped[10] = (byte) 0xff;
		mapped[11] = (byte) 0xff;
		System.arraycopy(ipv4, 0, mapped, 12, ipv4.length);
		return Inet6Address.getByAddress(null, mapped, -1);
	}

	private static class MapResolver implements MagicTinyflowSafeUrlImporter.DnsResolver {
		private final Map<String, InetAddress[]> addresses = new LinkedHashMap<String, InetAddress[]>();
		void put(String host, String address) throws Exception {
			addresses.put(host, new InetAddress[]{InetAddress.getByName(address)});
		}
		void put(String host, InetAddress... values) {
			addresses.put(host, values);
		}
		@Override public InetAddress[] resolve(String host) { return addresses.get(host); }
	}

	private static final class SequencedResolver implements MagicTinyflowSafeUrlImporter.DnsResolver {
		private final Deque<InetAddress> addresses = new ArrayDeque<InetAddress>();
		private int calls;
		private SequencedResolver(String... values) throws Exception {
			for (String value : values) addresses.add(InetAddress.getByName(value));
		}
		@Override public InetAddress[] resolve(String host) {
			calls++;
			return new InetAddress[]{addresses.removeFirst()};
		}
	}

	private static final class RecordingTransport implements MagicTinyflowSafeUrlImporter.HttpTransport {
		private final Deque<MagicTinyflowSafeUrlImporter.HttpResponse> responses =
				new ArrayDeque<MagicTinyflowSafeUrlImporter.HttpResponse>();
		private final List<InetAddress> approved = new java.util.ArrayList<InetAddress>();
		private int calls;
		private java.io.IOException failure;

		@Override public MagicTinyflowSafeUrlImporter.HttpResponse execute(URI uri, InetAddress[] addresses,
				int connectTimeoutMillis, int readTimeoutMillis, long maxBytes) throws java.io.IOException {
			calls++; approved.addAll(Arrays.asList(addresses));
			if (failure != null) throw failure;
			return responses.removeFirst();
		}
	}

	private static final class RawHttpServer implements AutoCloseable {
		private final ServerSocket server;
		private final Thread worker;
		private volatile Throwable failure;

		private RawHttpServer(final byte[] response) throws Exception {
			server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
			worker = new Thread(new Runnable() {
				@Override public void run() {
					try (Socket socket = server.accept(); OutputStream output = socket.getOutputStream()) {
						output.write(response);
						output.flush();
					} catch (Throwable throwable) {
						failure = throwable;
					}
				}
			}, "tinyflow-url-import-test-server");
			worker.start();
		}

		private InetAddress address() {
			return server.getInetAddress();
		}

		private URI uri() throws Exception {
			return new URI("http://public.test:" + server.getLocalPort() + "/document.txt");
		}

		@Override public void close() throws Exception {
			server.close();
			worker.join(1000L);
			if (worker.isAlive()) throw new IllegalStateException("test HTTP server did not stop");
			if (failure != null) throw new AssertionError("test HTTP server failed", failure);
		}
	}
}
