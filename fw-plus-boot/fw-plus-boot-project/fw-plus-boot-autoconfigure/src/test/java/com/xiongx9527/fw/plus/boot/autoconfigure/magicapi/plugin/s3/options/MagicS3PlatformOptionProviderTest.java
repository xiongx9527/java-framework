package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicS3PlatformOptionProviderTest {

	private static final String MINIO_CONFIG = SpringFileStorageProperties.SpringMinioConfig.class.getName();

	private static final String MINIO_SDK = "io.minio.MinioClient";
	private static final String ALIYUN_OSS_SDK = "com.aliyun.oss.OSS";
	private static final String TENCENT_COS_SDK = "com.qcloud.cos.COSClient";
	private static final String AMAZON_S3_SDK = "com.amazonaws.services.s3.AmazonS3";
	private static final String HUAWEI_OBS_SDK = "com.obs.services.ObsClient";
	private static final String QINIU_KODO_SDK = "com.qiniu.storage.UploadManager";
	private static final String BAIDU_BOS_SDK = "com.baidubce.services.bos.BosClient";
	private static final String UPYUN_USS_SDK = "com.UpYun";

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
		// 伪造 minio 配置类缺失、aliyun SDK 存在，验证 minio 被单独过滤
		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				new MagicS3PlatformOptionProperties(),
				new SdkPresenceClassLoader(
						Collections.singleton(ALIYUN_OSS_SDK),
						Collections.singleton(MINIO_CONFIG)));

		List<MagicS3PlatformOption> options = provider.getOptions();

		assertFalse(containsValue(options, "minio"));
		assertTrue(containsValue(options, "aliyun-oss"));
	}

	@Test
	void shouldFilterDefaultPlatformWhenRequiredSdkClassIsMissing() {
		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				new MagicS3PlatformOptionProperties(),
				new SdkPresenceClassLoader(
						Collections.<String>emptySet(),
						Collections.singleton(MINIO_SDK)));

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
				new SdkPresenceClassLoader(
						Collections.<String>emptySet(),
						Collections.singleton(MINIO_SDK)));

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
				new SdkPresenceClassLoader(
						Collections.singleton(MINIO_SDK),
						Collections.<String>emptySet()));

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
		// 伪造全部 8 个平台 SDK 可见，验证默认平台的完整顺序
		MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
				new MagicS3PlatformOptionProperties(),
				new SdkPresenceClassLoader(
						new HashSet<String>(Arrays.asList(
								MINIO_SDK, ALIYUN_OSS_SDK, TENCENT_COS_SDK, AMAZON_S3_SDK,
								HUAWEI_OBS_SDK, QINIU_KODO_SDK, BAIDU_BOS_SDK, UPYUN_USS_SDK)),
						Collections.<String>emptySet()));

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
		assertEquals(expectedOrder.size(), options.size());
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

	/**
	 * 伪造类可见性的 ClassLoader：默认委派给真实测试 ClassLoader；
	 * {@code presentNames} 中的类名会被动态定义为一个空类（供存在性探测），
	 * {@code absentNames} 中的类名视为缺失。
	 *
	 * <p>本模块测试 classpath 不含任何 S3 SDK（均为文件存储 starter 的运行时依赖），
	 * 因此平台过滤逻辑必须用伪造类加载器验证，不依赖外部 SDK 是否在 classpath。</p>
	 */
	private static class SdkPresenceClassLoader extends ClassLoader {

		private final Set<String> presentNames;

		private final Set<String> absentNames;

		SdkPresenceClassLoader(Set<String> presentNames, Set<String> absentNames) {
			super(MagicS3PlatformOptionProviderTest.class.getClassLoader());
			this.presentNames = presentNames;
			this.absentNames = absentNames;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (absentNames.contains(name)) {
				throw new ClassNotFoundException(name);
			}
			if (presentNames.contains(name)) {
				Class<?> loaded = findLoadedClass(name);
				if (loaded == null) {
					byte[] bytes = emptyClassBytes(name.replace('.', '/'));
					loaded = defineClass(name, bytes, 0, bytes.length);
				}
				if (resolve) {
					resolveClass(loaded);
				}
				return loaded;
			}
			return super.loadClass(name, resolve);
		}

		/** 生成仅含类头、无字段/方法的最小合法 class 文件（继承 Object） */
		private static byte[] emptyClassBytes(String internalName) {
			byte[] name = internalName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			byte[] object = "java/lang/Object".getBytes(java.nio.charset.StandardCharsets.UTF_8);
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			try {
				out.writeInt(0xCAFEBABE);
				out.writeShort(0); // minor version
				out.writeShort(52); // major version (Java 8)
				out.writeShort(5); // constant_pool_count：4 个常量 + 1
				out.writeByte(7); // #1 CONSTANT_Class -> #3（类自身名）
				out.writeShort(3);
				out.writeByte(7); // #2 CONSTANT_Class -> #4（super，必须是 Class 项）
				out.writeShort(4);
				out.writeByte(1); // #3 CONSTANT_Utf8 类自身名
				out.writeShort(name.length);
				out.write(name);
				out.writeByte(1); // #4 CONSTANT_Utf8 "java/lang/Object"
				out.writeShort(object.length);
				out.write(object);
				out.writeShort(0x0021); // ACC_PUBLIC | ACC_SUPER
				out.writeShort(1); // this_class = #1
				out.writeShort(2); // super_class = #2
				out.writeShort(0); // interfaces
				out.writeShort(0); // fields
				out.writeShort(0); // methods
				out.writeShort(0); // attributes
				out.flush();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			return bytes.toByteArray();
		}
	}
}
