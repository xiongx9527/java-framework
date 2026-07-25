package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3DataSourceMagicDynamicRegistryTest {

	@Test
	void registerPersistedDataSourcesRegistersPersistedS3DatasourceResources() {
		S3DataSourceInfo info = new S3DataSourceInfo();
		info.setId("test1-id");
		info.setKey("test1");
		info.setName("测试存储");

		S3DataSourceInfoMagicResourceStorage storage = mock(S3DataSourceInfoMagicResourceStorage.class);
		when(storage.findAll()).thenReturn(Collections.singletonList(info));
		when(storage.buildKey(info)).thenReturn("test1-0");
		MagicDynamicS3DataSource dynamicS3DataSource = mock(MagicDynamicS3DataSource.class);

		S3DataSourceMagicDynamicRegistry registry = new S3DataSourceMagicDynamicRegistry(storage, dynamicS3DataSource);

		registry.registerPersistedDataSources();

		verify(dynamicS3DataSource).buildFileStorageService(info);
	}
}
