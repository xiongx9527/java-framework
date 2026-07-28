package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicS3PlatformOptionProviderTest {

	@Test
	void shouldFilterUnsupportedAliasFromDefaultPlatforms() {
		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				new MagicS3PlatformOptionProperties(),
				MagicS3PlatformOptionProviderTest.class.getClassLoader());

		List<MagicS3PlatformOption> options = provider.getOptions();

		assertFalse(containsValue(options, "aws-s3"));
	}

	@Test
	void shouldFilterDefaultPlatformWhenConfigClassIsMissing() {
		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				new MagicS3PlatformOptionProperties(),
				new MissingMinioClassLoader());

		List<MagicS3PlatformOption> options = provider.getOptions();

		assertFalse(containsValue(options, "minio"));
		assertTrue(containsValue(options, "aliyun-oss"));
	}

	@Test
	void shouldFilterDefaultPlatformWhenRequiredSdkClassIsMissing() {
		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				new MagicS3PlatformOptionProperties(),
				new MissingMinioSdkClassLoader());

		List<MagicS3PlatformOption> options = provider.getOptions();

		assertFalse(containsValue(options, "minio"));
	}

	@Test
	void shouldFilterConfiguredSupportedPlatformWhenDefaultSdkClassIsMissing() {
		MagicS3PlatformCandidate candidate = new MagicS3PlatformCandidate();
		candidate.setText("MinIO Configured");
		candidate.setValue("minio");
		candidate.setConfigClassName(SpringFileStorageProperties.SpringMinioConfig.class.getName());

		MagicS3PlatformOptionProperties properties = new MagicS3PlatformOptionProperties();
		properties.setPlatformCandidates(Arrays.asList(candidate));

		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				properties,
				new MissingMinioSdkClassLoader());

		List<MagicS3PlatformOption> options = provider.getOptions();

		assertFalse(containsValue(options, "minio"));
	}

	@Test
	void shouldShowConfiguredCandidateWhenClassExistsAndPlatformIsSupported() {
		MagicS3PlatformCandidate duplicate = new MagicS3PlatformCandidate();
		duplicate.setText("MinIO Custom");
		duplicate.setValue("minio");
		duplicate.setConfigClassName(SpringFileStorageProperties.SpringMinioConfig.class.getName());
		duplicate.setRequiredClassNames(Arrays.asList(MagicS3PlatformOptionProviderTest.class.getName()));

		MagicS3PlatformOptionProperties properties = new MagicS3PlatformOptionProperties();
		properties.setPlatformCandidates(Arrays.asList(duplicate));

		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				properties,
				MagicS3PlatformOptionProviderTest.class.getClassLoader());

		List<MagicS3PlatformOption> options = provider.getOptions();

		assertEquals(1, countValue(options, "minio"));
		assertEquals("minio", findByValue(options, "minio").getText());
	}

	@Test
	void shouldHideConfiguredCandidateWhenClassIsMissingOrPlatformUnsupported() {
		MagicS3PlatformCandidate missingClass = new MagicS3PlatformCandidate();
		missingClass.setText("Missing");
		missingClass.setValue("missing-platform");
		missingClass.setConfigClassName("com.example.MissingS3Config");

		MagicS3PlatformCandidate unsupported = new MagicS3PlatformCandidate();
		unsupported.setText("FTP");
		unsupported.setValue("ftp");
		unsupported.setConfigClassName(SpringFileStorageProperties.SpringFtpConfig.class.getName());

		MagicS3PlatformOptionProperties properties = new MagicS3PlatformOptionProperties();
		properties.setPlatformCandidates(Arrays.asList(missingClass, unsupported));

		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				properties,
				MagicS3PlatformOptionProviderTest.class.getClassLoader());

		List<MagicS3PlatformOption> options = provider.getOptions();

		assertFalse(containsValue(options, "missing-platform"));
		assertFalse(containsValue(options, "ftp"));
	}

	@Test
	void shouldKeepDefaultCandidateOrderStable() {
		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				new MagicS3PlatformOptionProperties(),
				MagicS3PlatformOptionProviderTest.class.getClassLoader());

		List<MagicS3PlatformOption> options = provider.getOptions();

		List<String> expectedOrder = Arrays.asList(
				"minio",
				"aliyun-oss",
				"tencent-cos",
				"amazon-s3",
				"huawei-obs",
				"qiniu-kodo",
				"baidu-bos",
				"upyun-uss");
		int previousIndex = -1;
		for (MagicS3PlatformOption option : options) {
			int currentIndex = expectedOrder.indexOf(option.getValue());
			assertTrue(currentIndex > previousIndex);
			previousIndex = currentIndex;
		}
	}

	private boolean containsValue(List<MagicS3PlatformOption> options, String value) {
		return findByValue(options, value) != null;
	}

	private MagicS3PlatformOption findByValue(List<MagicS3PlatformOption> options, String value) {
		for (MagicS3PlatformOption option : options) {
			if (value.equals(option.getValue())) {
				return option;
			}
		}
		return null;
	}

	private int countValue(List<MagicS3PlatformOption> options, String value) {
		int count = 0;
		for (MagicS3PlatformOption option : options) {
			if (value.equals(option.getValue())) {
				count++;
			}
		}
		return count;
	}

	private int indexOfValue(List<MagicS3PlatformOption> options, String value) {
		for (int i = 0; i < options.size(); i++) {
			if (value.equals(options.get(i).getValue())) {
				return i;
			}
		}
		return -1;
	}

	private static class MissingMinioClassLoader extends ClassLoader {
		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (SpringFileStorageProperties.SpringMinioConfig.class.getName().equals(name)) {
				throw new ClassNotFoundException(name);
			}
			return super.loadClass(name, resolve);
		}
	}

	private static class MissingMinioSdkClassLoader extends ClassLoader {
		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if ("io.minio.MinioClient".equals(name)) {
				throw new ClassNotFoundException(name);
			}
			return super.loadClass(name, resolve);
		}
	}
}
