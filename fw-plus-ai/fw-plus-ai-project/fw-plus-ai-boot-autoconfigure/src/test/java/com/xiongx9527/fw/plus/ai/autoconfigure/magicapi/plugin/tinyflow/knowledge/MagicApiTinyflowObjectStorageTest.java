package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MagicApiTinyflowObjectStorageTest {

	@Test
	public void shouldUploadToServerControlledTenantPathAndReturnSafeReference() throws Exception {
		RecordingBackend backend = new RecordingBackend();
		MagicApiTinyflowObjectStorage storage = new MagicApiTinyflowObjectStorage(backend, 1024L);
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("s3Key", "main-s3"); metadata.put("secretKey", "plain-secret");
		byte[] content = "knowledge body".getBytes(StandardCharsets.UTF_8);

		TinyflowObjectStorage.StoredObject stored = storage.put("tenant/a", "manual.txt",
				new ByteArrayInputStream(content), "text/plain", metadata);

		assertEquals("main-s3", backend.s3Key);
		assertTrue(backend.path.matches("tinyflow/[A-Za-z0-9_-]+/[a-f0-9]{32}"));
		assertEquals("manual.txt", backend.filename);
		assertEquals(content.length, stored.getSize());
		assertEquals("92870a2cf2e72cedab5410465b859756818bcc825935956c6b518ade794df85b", stored.getChecksum());
		assertFalse(stored.getObjectKey().contains("plain-secret"));
		assertFalse(stored.getMetadata().containsKey("secretKey"));
		assertEquals("manual.txt", stored.getMetadata().get("filename"));
		String[] referenceParts = stored.getObjectKey().split("\\.", -1);
		assertEquals("tfs3v2", referenceParts[0]);
		assertEquals(5, referenceParts.length);
		for (int index = 1; index < referenceParts.length; index++) {
			String value = new String(Base64.getUrlDecoder().decode(referenceParts[index]), StandardCharsets.UTF_8);
			assertFalse(value.contains("X-Amz-Credential"));
		}
	}

	@Test
	public void shouldRejectPathTraversalAndOversizedInputBeforeUpload() {
		RecordingBackend backend = new RecordingBackend();
		MagicApiTinyflowObjectStorage storage = new MagicApiTinyflowObjectStorage(backend, 4L);
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("s3Key", "main-s3");

		assertThrows(IllegalArgumentException.class, () -> storage.put("tenant-a", "../secret.txt",
				new ByteArrayInputStream(new byte[]{1}), "text/plain", metadata));
		assertThrows(IllegalArgumentException.class, () -> storage.put("tenant-a", "..\\secret.txt",
				new ByteArrayInputStream(new byte[]{1}), "text/plain", metadata));
		assertThrows(IllegalArgumentException.class, () -> storage.put("tenant-a", "large.txt",
				new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}), "text/plain", metadata));
		assertEquals(0, backend.uploads);
	}

	@Test
	public void shouldBindReadExistsAndDeleteToOriginalTenant() throws Exception {
		RecordingBackend backend = new RecordingBackend();
		MagicApiTinyflowObjectStorage storage = new MagicApiTinyflowObjectStorage(backend, 1024L);
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("s3Key", "main-s3");
		TinyflowObjectStorage.StoredObject stored = storage.put("tenant-a", "manual.txt",
				new ByteArrayInputStream("body".getBytes(StandardCharsets.UTF_8)), "text/plain", metadata);

		assertThrows(SecurityException.class, () -> storage.get("tenant-b", stored.getObjectKey()));
		assertThrows(SecurityException.class, () -> storage.exists("tenant-b", stored.getObjectKey()));
		assertThrows(SecurityException.class, () -> storage.delete("tenant-b", stored.getObjectKey()));
		assertEquals(0, backend.reads);
		assertArrayEquals("body".getBytes(StandardCharsets.UTF_8), read(storage.get("tenant-a", stored.getObjectKey())));
		assertTrue(storage.exists("tenant-a", stored.getObjectKey()));
		storage.delete("tenant-a", stored.getObjectKey());
		assertEquals(1, backend.deletes);
	}

	private byte[] read(InputStream input) throws Exception {
		byte[] bytes = new byte[16];
		int length = input.read(bytes);
		byte[] result = new byte[length];
		System.arraycopy(bytes, 0, result, 0, length);
		return result;
	}

	private static final class RecordingBackend implements MagicApiTinyflowObjectStorage.StorageBackend {
		private String s3Key;
		private String path;
		private String filename;
		private int uploads;
		private int reads;
		private int deletes;

		@Override public MagicApiTinyflowObjectStorage.UploadedObject upload(String s3Key, String path,
				byte[] content, String filename, String mediaType) {
			this.s3Key = s3Key; this.path = path; this.filename = filename; uploads++;
			return new MagicApiTinyflowObjectStorage.UploadedObject(
					"https://bucket.example.test/" + path + "/" + filename
							+ "?X-Amz-Credential=temporary-secret", path, filename);
		}

		@Override public InputStream get(MagicApiTinyflowObjectStorage.ObjectReference reference, long maxBytes) {
			reads++;
			return new ByteArrayInputStream("body".getBytes(StandardCharsets.UTF_8));
		}

		@Override public boolean exists(MagicApiTinyflowObjectStorage.ObjectReference reference) { return true; }
		@Override public void delete(MagicApiTinyflowObjectStorage.ObjectReference reference) { deletes++; }
	}
}
