package com.xiongx9527.fw.plus.ai.askdata.capability;

/**
 * AskData 能力的不可变描述符。
 *
 * <p>Core 只读取描述符进行选择和兼容性校验，不持有厂商客户端或 SDK 类型。
 * 厂商适配器保留在 Provider 实现内部，从而避免 Core 与具体模型、向量库或可观测性实现耦合。</p>
 */
public final class AskDataCapabilityDescriptor {

	private final String key;
	private final AskDataCapabilityType type;
	private final String version;
	private final String minimumCoreVersion;
	private final String maximumCoreVersion;
	private final int priority;
	private final boolean clusterCompatible;

	public AskDataCapabilityDescriptor(String key, AskDataCapabilityType type, String version,
			String minimumCoreVersion, String maximumCoreVersion, int priority, boolean clusterCompatible) {
		this.key = requireNonBlank(key, "key");
		if (type == null) {
			throw new IllegalArgumentException("type 不能为空");
		}
		this.type = type;
		this.version = requireNonBlank(version, "version");
		this.minimumCoreVersion = requireNonBlank(minimumCoreVersion, "minimumCoreVersion");
		this.maximumCoreVersion = requireNonBlank(maximumCoreVersion, "maximumCoreVersion");
		if (DefaultAskDataCapabilityRegistry.compareVersions(this.minimumCoreVersion, this.maximumCoreVersion) > 0) {
			throw new IllegalArgumentException("minimumCoreVersion 不能大于 maximumCoreVersion");
		}
		this.priority = priority;
		this.clusterCompatible = clusterCompatible;
	}

	public String getKey() {
		return key;
	}

	public AskDataCapabilityType getType() {
		return type;
	}

	public String getVersion() {
		return version;
	}

	public String getMinimumCoreVersion() {
		return minimumCoreVersion;
	}

	public String getMaximumCoreVersion() {
		return maximumCoreVersion;
	}

	public int getPriority() {
		return priority;
	}

	public boolean isClusterCompatible() {
		return clusterCompatible;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}
}
