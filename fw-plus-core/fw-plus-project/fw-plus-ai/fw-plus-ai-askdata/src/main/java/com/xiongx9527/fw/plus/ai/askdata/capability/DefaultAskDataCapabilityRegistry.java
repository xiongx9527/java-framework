package com.xiongx9527.fw.plus.ai.askdata.capability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于描述符的默认能力注册表。
 *
 * <p>Core 只保存厂商无关的描述符，绝不读取 Provider 内部的厂商 client，
 * 因此不同模型、向量库和运行时适配器可在不污染 Core API 的前提下替换。</p>
 */
public class DefaultAskDataCapabilityRegistry implements AskDataCapabilityRegistry {

	private final String coreVersion;
	private final boolean clusterEnabled;
	private final Map<String, AskDataCapabilityDescriptor> descriptors = new LinkedHashMap<String, AskDataCapabilityDescriptor>();

	public DefaultAskDataCapabilityRegistry(String coreVersion, boolean clusterEnabled) {
		this(coreVersion, clusterEnabled, Collections.<AskDataCapabilityProvider>emptyList());
	}

	public DefaultAskDataCapabilityRegistry(String coreVersion, boolean clusterEnabled,
			Collection<? extends AskDataCapabilityProvider> providers) {
		this.coreVersion = requireNonBlank(coreVersion, "coreVersion");
		this.clusterEnabled = clusterEnabled;
		List<AskDataCapabilityProvider> copiedProviders = providers == null
				? Collections.<AskDataCapabilityProvider>emptyList()
				: Collections.unmodifiableList(new ArrayList<AskDataCapabilityProvider>(providers));
		for (AskDataCapabilityProvider provider : copiedProviders) {
			register(provider);
		}
	}

	@Override
	public synchronized void register(AskDataCapabilityProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("provider 及其 descriptor 不能为空");
		}
		AskDataCapabilityDescriptor descriptor = provider.getDescriptor();
		if (descriptor == null) {
			throw new IllegalArgumentException("provider 及其 descriptor 不能为空");
		}
		if (descriptors.containsKey(descriptor.getKey())) {
			throw new IllegalArgumentException("能力 key 重复: " + descriptor.getKey());
		}
		if (!isCoreVersionCompatible(descriptor)) {
			throw new IllegalArgumentException("能力与 Core 版本不兼容: " + descriptor.getKey());
		}
		if (clusterEnabled && !descriptor.isClusterCompatible()) {
			throw new IllegalArgumentException("集群模式不支持该能力: " + descriptor.getKey());
		}
		descriptors.put(descriptor.getKey(), descriptor);
	}

	@Override
	public synchronized AskDataCapabilityDescriptor get(String key) {
		AskDataCapabilityDescriptor descriptor = descriptors.get(key);
		if (descriptor == null) {
			throw new IllegalArgumentException("未知能力 key: " + key + "，可用 key: " + descriptors.keySet());
		}
		return descriptor;
	}

	@Override
	public synchronized List<AskDataCapabilityDescriptor> list(AskDataCapabilityType type) {
		if (type == null) {
			throw new IllegalArgumentException("type 不能为空");
		}
		List<AskDataCapabilityDescriptor> result = new ArrayList<AskDataCapabilityDescriptor>();
		for (AskDataCapabilityDescriptor descriptor : descriptors.values()) {
			if (descriptor.getType() == type) {
				result.add(descriptor);
			}
		}
		Collections.sort(result, new Comparator<AskDataCapabilityDescriptor>() {
			@Override
			public int compare(AskDataCapabilityDescriptor left, AskDataCapabilityDescriptor right) {
				int priorityComparison = Integer.compare(right.getPriority(), left.getPriority());
				return priorityComparison != 0 ? priorityComparison : left.getKey().compareTo(right.getKey());
			}
		});
		return Collections.unmodifiableList(new ArrayList<AskDataCapabilityDescriptor>(result));
	}

	private boolean isCoreVersionCompatible(AskDataCapabilityDescriptor descriptor) {
		return compareVersions(coreVersion, descriptor.getMinimumCoreVersion()) >= 0
				&& compareVersions(coreVersion, descriptor.getMaximumCoreVersion()) <= 0;
	}

	static int compareVersions(String left, String right) {
		String[] leftParts = parseVersionParts(left);
		String[] rightParts = parseVersionParts(right);
		int length = Math.max(leftParts.length, rightParts.length);
		for (int index = 0; index < length; index++) {
			int leftPart = index < leftParts.length ? Integer.parseInt(leftParts[index]) : 0;
			int rightPart = index < rightParts.length ? Integer.parseInt(rightParts[index]) : 0;
			if (leftPart != rightPart) {
				return leftPart < rightPart ? -1 : 1;
			}
		}
		return 0;
	}

	private static String[] parseVersionParts(String version) {
		String numericVersion = version;
		if (numericVersion.endsWith("-SNAPSHOT")) {
			numericVersion = numericVersion.substring(0, numericVersion.length() - "-SNAPSHOT".length());
		}
		if (numericVersion.indexOf('-') >= 0) {
			throw new IllegalArgumentException("版本格式非法: " + version);
		}
		String[] parts = numericVersion.split("\\.", -1);
		for (String part : parts) {
			if (part.length() == 0) {
				throw new IllegalArgumentException("版本格式非法: " + version);
			}
			try {
				Integer.parseInt(part);
			} catch (NumberFormatException exception) {
				throw new IllegalArgumentException("版本格式非法: " + version, exception);
			}
		}
		return parts;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}
}
