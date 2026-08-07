package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyFileStorageServiceTest {

	@Test
	void existsReturnsFalseForConfirmedMissingAndTrueForExistingObject() {
		FileStorageService storage = mock(FileStorageService.class);
		when(storage.exists(any(FileInfo.class))).thenReturn(false, true);
		MyFileStorageService service = service(storage);

		assertFalse(service.exists(objectUrl()));
		assertTrue(service.exists(objectUrl()));
	}

	@Test
	void existsPropagatesForbiddenServerAndIoFailures() {
		assertPropagated(new FileStorageRuntimeException("S3 request failed with status 403"));
		assertPropagated(new FileStorageRuntimeException("S3 request failed with status 503"));
		IOException ioFailure = new IOException("timeout");
		FileStorageRuntimeException wrappedIoFailure = new FileStorageRuntimeException("S3 request failed", ioFailure);
		assertPropagated(wrappedIoFailure);
		assertSame(ioFailure, wrappedIoFailure.getCause());
	}

	private void assertPropagated(FileStorageRuntimeException failure) {
		FileStorageService storage = mock(FileStorageService.class);
		when(storage.exists(any(FileInfo.class))).thenThrow(failure);
		assertSame(failure, assertThrows(FileStorageRuntimeException.class,
				() -> service(storage).exists(objectUrl())));
	}

	private MyFileStorageService service(FileStorageService storage) {
		S3DataSourceInfo info = new S3DataSourceInfo();
		info.setPlatform("amazon-s3");
		info.setEndPoint("storage.example");
		info.setBucketName("bucket");
		info.setBasePath("");
		return new MyFileStorageService(storage, info);
	}

	private String objectUrl() {
		return "https://bucket.storage.example/object.txt";
	}
}
