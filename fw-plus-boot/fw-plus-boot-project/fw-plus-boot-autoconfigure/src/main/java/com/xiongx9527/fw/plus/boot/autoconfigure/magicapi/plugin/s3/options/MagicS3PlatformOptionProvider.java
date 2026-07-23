package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MagicS3PlatformOptionProvider {

	private static final String MINIO = "minio";
	private static final String ALIYUN_OSS = "aliyun-oss";
	private static final String TENCENT_COS = "tencent-cos";
	private static final String AMAZON_S3 = "amazon-s3";
	private static final String HUAWEI_OBS = "huawei-obs";
	private static final String QINIU_KODO = "qiniu-kodo";
	private static final String BAIDU_BOS = "baidu-bos";
	private static final String UPYUN_USS = "upyun-uss";

	private static final Set<String> SUPPORTED_PLATFORMS = new LinkedHashSet<String>(Arrays.asList(
			MINIO,
			ALIYUN_OSS,
			TENCENT_COS,
			AMAZON_S3,
			HUAWEI_OBS,
			QINIU_KODO,
			BAIDU_BOS,
			UPYUN_USS
	));

	private static final Map<String, List<String>> DEFAULT_REQUIRED_CLASS_NAMES = new LinkedHashMap<String, List<String>>();

	static {
		DEFAULT_REQUIRED_CLASS_NAMES.put(MINIO, Arrays.asList("io.minio.MinioClient"));
		DEFAULT_REQUIRED_CLASS_NAMES.put(ALIYUN_OSS, Arrays.asList("com.aliyun.oss.OSS"));
		DEFAULT_REQUIRED_CLASS_NAMES.put(TENCENT_COS, Arrays.asList("com.qcloud.cos.COSClient"));
		DEFAULT_REQUIRED_CLASS_NAMES.put(AMAZON_S3, Arrays.asList("com.amazonaws.services.s3.AmazonS3"));
		DEFAULT_REQUIRED_CLASS_NAMES.put(HUAWEI_OBS, Arrays.asList("com.obs.services.ObsClient"));
		DEFAULT_REQUIRED_CLASS_NAMES.put(QINIU_KODO, Arrays.asList("com.qiniu.storage.UploadManager"));
		DEFAULT_REQUIRED_CLASS_NAMES.put(BAIDU_BOS, Arrays.asList("com.baidubce.services.bos.BosClient"));
		DEFAULT_REQUIRED_CLASS_NAMES.put(UPYUN_USS, Arrays.asList("com.UpYun"));
	}

	private final MagicS3PlatformOptionProperties properties;

	private final ClassLoader classLoader;

	public MagicS3PlatformOptionProvider(MagicS3PlatformOptionProperties properties, ClassLoader classLoader) {
		this.properties = properties == null ? new MagicS3PlatformOptionProperties() : properties;
		this.classLoader = classLoader == null ? MagicS3PlatformOptionProvider.class.getClassLoader() : classLoader;
	}

	public List<MagicS3PlatformOption> getOptions() {
		List<MagicS3PlatformOption> result = new ArrayList<MagicS3PlatformOption>();
		Set<String> seen = new LinkedHashSet<String>();
		addCandidates(result, seen, defaultCandidates());
		addCandidates(result, seen, properties.getPlatformCandidates());
		return result;
	}

	private void addCandidates(List<MagicS3PlatformOption> result,
							   Set<String> seen,
							   List<MagicS3PlatformCandidate> candidates) {
		if (candidates == null) {
			return;
		}
		for (MagicS3PlatformCandidate candidate : candidates) {
			if (!isAvailable(candidate) || seen.contains(candidate.getValue())) {
				continue;
			}
			seen.add(candidate.getValue());
			result.add(new MagicS3PlatformOption(resolveText(candidate), candidate.getValue()));
		}
	}

	private boolean isAvailable(MagicS3PlatformCandidate candidate) {
		return candidate != null
				&& StringUtils.isNotBlank(candidate.getValue())
			&& SUPPORTED_PLATFORMS.contains(candidate.getValue())
			&& StringUtils.isNotBlank(candidate.getConfigClassName())
			&& ClassUtils.isPresent(candidate.getConfigClassName(), classLoader)
			&& areClassNamesPresent(DEFAULT_REQUIRED_CLASS_NAMES.get(candidate.getValue()))
			&& areRequiredClassesPresent(candidate);
	}

	private boolean areRequiredClassesPresent(MagicS3PlatformCandidate candidate) {
		return areClassNamesPresent(candidate.getRequiredClassNames());
	}

	private boolean areClassNamesPresent(List<String> classNames) {
		if (classNames == null) {
			return true;
		}
		for (String className : classNames) {
			if (StringUtils.isNotBlank(className) && !ClassUtils.isPresent(className, classLoader)) {
				return false;
			}
		}
		return true;
	}

	private String resolveText(MagicS3PlatformCandidate candidate) {
		return StringUtils.isBlank(candidate.getText()) ? candidate.getValue() : candidate.getText();
	}

	private List<MagicS3PlatformCandidate> defaultCandidates() {
		List<MagicS3PlatformCandidate> candidates = new ArrayList<MagicS3PlatformCandidate>();
		candidates.add(new MagicS3PlatformCandidate(MINIO, MINIO, SpringFileStorageProperties.SpringMinioConfig.class.getName()));
		candidates.add(new MagicS3PlatformCandidate(ALIYUN_OSS, ALIYUN_OSS, SpringFileStorageProperties.SpringAliyunOssConfig.class.getName()));
		candidates.add(new MagicS3PlatformCandidate(TENCENT_COS, TENCENT_COS, SpringFileStorageProperties.SpringTencentCosConfig.class.getName()));
		candidates.add(new MagicS3PlatformCandidate(AMAZON_S3, AMAZON_S3, SpringFileStorageProperties.SpringAmazonS3Config.class.getName()));
		candidates.add(new MagicS3PlatformCandidate(HUAWEI_OBS, HUAWEI_OBS, SpringFileStorageProperties.SpringHuaweiObsConfig.class.getName()));
		candidates.add(new MagicS3PlatformCandidate(QINIU_KODO, QINIU_KODO, SpringFileStorageProperties.SpringQiniuKodoConfig.class.getName()));
		candidates.add(new MagicS3PlatformCandidate(BAIDU_BOS, BAIDU_BOS, SpringFileStorageProperties.SpringBaiduBosConfig.class.getName()));
		candidates.add(new MagicS3PlatformCandidate(UPYUN_USS, UPYUN_USS, SpringFileStorageProperties.SpringUpyunUssConfig.class.getName()));
		return candidates;
	}
}
