package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MyFileStorageService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import org.dromara.x.file.storage.core.FileInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MagicApiTinyflowObjectStorage implements TinyflowObjectStorage {

	private static final String VERSION = "tfs3v2";
	private static final String LEGACY_VERSION = "tfs3v1";
	private static final String S3_KEY = "s3Key";
	private static final long DEFAULT_MAX_BYTES = 20L * 1024L * 1024L;
	private final StorageBackend backend;
	private final long maxBytes;

	public MagicApiTinyflowObjectStorage(MagicDynamicS3DataSource dataSource) {
		this(new MagicDynamicS3StorageBackend(dataSource), DEFAULT_MAX_BYTES);
	}

	MagicApiTinyflowObjectStorage(StorageBackend backend, long maxBytes) {
		if (backend == null) throw new IllegalArgumentException("storage backend must not be null");
		if (maxBytes <= 0L) throw new IllegalArgumentException("maxBytes must be positive");
		this.backend = backend;
		this.maxBytes = maxBytes;
	}

	@Override
	public StoredObject put(String tenantId, String objectKey, InputStream input, String mediaType,
			Map<String, Object> metadata) throws IOException {
		String tenant = required(tenantId, "tenantId");
		String filename = filename(objectKey);
		if (input == null) throw new IllegalArgumentException("input must not be null");
		String s3Key = required(metadata == null ? null : string(metadata.get(S3_KEY)), S3_KEY);
		byte[] content = readAll(input, maxBytes);
		String path = "tinyflow/" + encode(tenant) + "/" + UUID.randomUUID().toString().replace("-", "");
		UploadedObject uploaded = backend.upload(s3Key, path, content, filename, mediaType);
		if (uploaded == null) throw new IOException("S3 upload did not return an object reference");
		ObjectReference reference = new ObjectReference(s3Key, tenant,
				uploaded.path == null ? path : uploaded.path, uploaded.filename == null ? filename : uploaded.filename);
		Map<String, Object> safeMetadata = new LinkedHashMap<String, Object>();
		safeMetadata.put(S3_KEY, s3Key);
		safeMetadata.put("filename", reference.filename);
		return new StoredObject(tenant, reference.encode(), mediaType, content.length, sha256(content), safeMetadata);
	}

	@Override
	public InputStream get(String tenantId, String objectKey) throws IOException {
		return backend.get(reference(tenantId, objectKey), maxBytes);
	}

	@Override
	public boolean exists(String tenantId, String objectKey) {
		return backend.exists(reference(tenantId, objectKey));
	}

	@Override
	public void delete(String tenantId, String objectKey) {
		ObjectReference reference = reference(tenantId, objectKey);
		try {
			backend.delete(reference);
		} catch (RuntimeException exception) {
			if (backend.exists(reference)) throw exception;
		}
	}

	private ObjectReference reference(String tenantId, String objectKey) {
		String tenant = required(tenantId, "tenantId");
		ObjectReference reference = ObjectReference.decode(objectKey);
		if (!tenant.equals(reference.tenantId)) throw new SecurityException("Object does not belong to tenant");
		return reference;
	}

	private String filename(String value) {
		String filename = required(value, "objectKey");
		if (filename.length() > 180 || ".".equals(filename) || "..".equals(filename)
				|| filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) {
			throw new IllegalArgumentException("objectKey must be a safe filename");
		}
		for (int index = 0; index < filename.length(); index++) {
			if (Character.isISOControl(filename.charAt(index))) {
				throw new IllegalArgumentException("objectKey must be a safe filename");
			}
		}
		return filename;
	}

	private static byte[] readAll(InputStream input, long limit) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		long total = 0L;
		int length;
		while ((length = input.read(buffer)) >= 0) {
			total += length;
			if (total > limit) throw new IllegalArgumentException("Object exceeds size limit");
			output.write(buffer, 0, length);
		}
		return output.toByteArray();
	}

	private String sha256(byte[] content) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);
			StringBuilder result = new StringBuilder(hash.length * 2);
			for (byte value : hash) result.append(String.format("%02x", value & 0xff));
			return result.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private static String required(String value, String field) {
		if (value == null || value.trim().length() == 0) throw new IllegalArgumentException(field + " must not be blank");
		return value.trim();
	}

	private String string(Object value) { return value == null ? null : String.valueOf(value); }
	private static String encode(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}
	private static String decode(String value) {
		return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
	}

	interface StorageBackend {
		UploadedObject upload(String s3Key, String path, byte[] content, String filename, String mediaType) throws IOException;
		InputStream get(ObjectReference reference, long maxBytes) throws IOException;
		boolean exists(ObjectReference reference);
		void delete(ObjectReference reference);
	}

	static final class UploadedObject {
		private final String url;
		private final String path;
		private final String filename;
		UploadedObject(String url, String path, String filename) {
			this.url = url; this.path = path; this.filename = filename;
		}
	}

	static final class ObjectReference {
		private final String s3Key;
		private final String tenantId;
		private final String path;
		private final String filename;

		private ObjectReference(String s3Key, String tenantId, String path, String filename) {
			this.s3Key = required(s3Key, S3_KEY); this.tenantId = required(tenantId, "tenantId");
			this.path = required(path, "path"); this.filename = required(filename, "filename");
		}

		private String encode() {
			return VERSION + "." + MagicApiTinyflowObjectStorage.encode(s3Key) + "."
					+ MagicApiTinyflowObjectStorage.encode(tenantId) + "."
					+ MagicApiTinyflowObjectStorage.encode(path) + "." + MagicApiTinyflowObjectStorage.encode(filename);
		}

		private static ObjectReference decode(String value) {
			try {
				String[] parts = required(value, "objectKey").split("\\.", -1);
				if (parts.length == 5 && VERSION.equals(parts[0])) {
					return new ObjectReference(MagicApiTinyflowObjectStorage.decode(parts[1]),
							MagicApiTinyflowObjectStorage.decode(parts[2]), MagicApiTinyflowObjectStorage.decode(parts[3]),
							MagicApiTinyflowObjectStorage.decode(parts[4]));
				}
				if (parts.length == 6 && LEGACY_VERSION.equals(parts[0])) {
					return new ObjectReference(MagicApiTinyflowObjectStorage.decode(parts[1]),
							MagicApiTinyflowObjectStorage.decode(parts[2]), MagicApiTinyflowObjectStorage.decode(parts[4]),
							MagicApiTinyflowObjectStorage.decode(parts[5]));
				}
				throw new IllegalArgumentException("Invalid S3 object reference");
			} catch (IllegalArgumentException exception) {
				throw new IllegalArgumentException("Invalid S3 object reference", exception);
			}
		}
	}

	private static final class MagicDynamicS3StorageBackend implements StorageBackend {
		private static final int TIMEOUT_MILLIS = 5000;
		private final MagicDynamicS3DataSource dataSource;

		private MagicDynamicS3StorageBackend(MagicDynamicS3DataSource dataSource) {
			if (dataSource == null) throw new IllegalArgumentException("MagicDynamicS3DataSource must not be null");
			this.dataSource = dataSource;
		}

		@Override public UploadedObject upload(String s3Key, String path, byte[] content, String filename,
				String mediaType) throws IOException {
			MyFileStorageService service = service(s3Key);
			FileInfo value;
			try {
				value = service.upload(path, content, filename, mediaType);
			} catch (RuntimeException exception) {
				throw new IOException("S3 upload failed", exception);
			}
			if (value == null || value.getUrl() == null) throw new IOException("S3 upload returned no file information");
			return new UploadedObject(value.getUrl(), value.getPath(), value.getFilename());
		}

		@Override public InputStream get(ObjectReference reference, long maxBytes) throws IOException {
			MyFileStorageService service = service(reference.s3Key);
			String objectUrl = objectUrl(reference);
			String signedUrl = service.generatePresignedUrlByUrl(objectUrl, null, reference.path, reference.filename);
			validateUrl(reference, signedUrl);
			HttpURLConnection connection = connection(signedUrl, "GET");
			try {
				int status = connection.getResponseCode();
				if (status < 200 || status >= 300) throw new IOException("S3 download failed with status " + status);
				return new ByteArrayInputStream(readAll(connection.getInputStream(), maxBytes));
			} finally {
				connection.disconnect();
			}
		}

		@Override public boolean exists(ObjectReference reference) {
			try {
				MyFileStorageService service = service(reference.s3Key);
				String objectUrl = objectUrl(reference);
				String signedUrl = service.generatePresignedUrlByUrl(objectUrl, null, reference.path, reference.filename);
				validateUrl(reference, signedUrl);
				HttpURLConnection connection = connection(signedUrl, "HEAD");
				try { int status = connection.getResponseCode(); return status >= 200 && status < 300; }
				finally { connection.disconnect(); }
			} catch (IOException exception) {
				return false;
			}
		}

		@Override public void delete(ObjectReference reference) {
			if (!service(reference.s3Key).delete(objectUrl(reference))) {
				throw new IllegalStateException("S3 object delete failed");
			}
		}

		private MyFileStorageService service(String key) {
			MyFileStorageService service = dataSource.getFileStorageService(key);
			if (service == null) throw new IllegalArgumentException("S3 data source not found: " + key);
			return service;
		}

		private void validateUrl(ObjectReference reference, String value) {
			try {
				S3DataSourceInfo info = info(reference.s3Key);
				URI uri = new URI(value);
				String endpoint = trimSlashes(info.getEndPoint());
				String authority = info.getBucketName() + "." + endpoint;
				String suffix = "/" + trimSlashes(reference.path) + "/" + reference.filename;
				if (!info.getEndPointSchema().equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
						|| uri.getRawAuthority() == null || !authority.equalsIgnoreCase(uri.getRawAuthority())
						|| uri.getPath() == null || !uri.getPath().endsWith(suffix) || uri.getPath().contains("..")) {
					throw new SecurityException("S3 object reference is outside configured data source");
				}
			} catch (java.net.URISyntaxException exception) {
				throw new SecurityException("S3 object reference is invalid", exception);
			}
		}

		private String objectUrl(ObjectReference reference) {
			try {
				S3DataSourceInfo info = info(reference.s3Key);
				String authority = info.getBucketName() + "." + trimSlashes(info.getEndPoint());
				URI uri = new URI(info.getEndPointSchema(), null, authority, -1,
						"/" + trimSlashes(reference.path) + "/" + reference.filename, null, null);
				String value = uri.toASCIIString();
				validateUrl(reference, value);
				return value;
			} catch (java.net.URISyntaxException exception) {
				throw new SecurityException("S3 object reference is invalid", exception);
			}
		}

		private S3DataSourceInfo info(String key) {
			for (S3DataSourceInfo value : dataSource.datasourceNodes()) {
				if (value != null && key.equals(value.getKey())) return value;
			}
			throw new IllegalArgumentException("S3 data source not found: " + key);
		}

		private HttpURLConnection connection(String value, String method) throws IOException {
			HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection();
			connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(TIMEOUT_MILLIS);
			connection.setReadTimeout(TIMEOUT_MILLIS); connection.setRequestMethod(method);
			return connection;
		}

		private String trimSlashes(String value) {
			String result = value == null ? "" : value.trim();
			while (result.startsWith("/")) result = result.substring(1);
			while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
			return result;
		}
	}
}
