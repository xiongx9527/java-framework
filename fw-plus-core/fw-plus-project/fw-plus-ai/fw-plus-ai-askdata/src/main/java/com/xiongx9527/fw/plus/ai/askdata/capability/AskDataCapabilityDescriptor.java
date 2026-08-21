package com.xiongx9527.fw.plus.ai.askdata.capability;

/**
 * AskData 能力的不可变描述符。
 *
 * <p>Core 只读取描述符进行选择和兼容性校验，不持有厂商客户端或 SDK 类型。
 * 厂商适配器保留在 Provider 实现内部，从而避免 Core 与具体模型、向量库或可观测性实现耦合。
 * 描述符以能力 key 作为集群内唯一标识，以 Core 版本范围和集群兼容标志表达可装配条件。</p>
 */
public final class AskDataCapabilityDescriptor {

	private final String key;
	private final AskDataCapabilityType type;
	private final String version;
	private final String minimumCoreVersion;
	private final String maximumCoreVersion;
	private final int priority;
	private final boolean clusterCompatible;
	private final String configurationNamespace;

	/**
	 * 创建不可变能力描述符。
	 *
	 * <p>Core 版本上下界按数字点分段比较，末尾 {@code -SNAPSHOT} 仅表示构建态，
	 * 与相同数字段的发布版本等价；不接受其他预发布或厂商限定词。最小版本不得高于最大版本。</p>
	 *
	 * @param key 集群内唯一的能力标识，不能为空白
	 * @param type 能力类别，不能为空
	 * @param version Provider 自身版本，不能为空白
	 * @param minimumCoreVersion 可支持的最小 Core 版本，不能为空白
	 * @param maximumCoreVersion 可支持的最大 Core 版本，不能为空白
	 * @param priority 同类别选择时的优先级，数值越大越优先
	 * @param clusterCompatible 是否可在集群模式装配
	 */
	public AskDataCapabilityDescriptor(String key, AskDataCapabilityType type, String version,
			String minimumCoreVersion, String maximumCoreVersion, int priority, boolean clusterCompatible) {
		this(key, type, version, minimumCoreVersion, maximumCoreVersion, priority, clusterCompatible,
				"fw.plus.ai.askdata.capabilities." + requireNonBlank(key, "key"));
	}

	/**
	 * 创建带专属配置命名空间的不可变能力描述符。
	 *
	 * <p>命名空间用于 Starter 在启动期定位实现专属配置和输出诊断。Core 只保存名称，不读取配置值，
	 * 因而不会依赖 Spring 或具体实现。</p>
	 */
	public AskDataCapabilityDescriptor(String key, AskDataCapabilityType type, String version,
			String minimumCoreVersion, String maximumCoreVersion, int priority, boolean clusterCompatible,
			String configurationNamespace) {
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
		this.configurationNamespace = requireNonBlank(configurationNamespace, "configurationNamespace");
	}

	/**
	 * 获取集群内唯一的能力标识。
	 *
	 * @return 能力 key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * 获取能力类别，用于在同类候选中进行确定性排序和选择。
	 *
	 * @return 能力类别
	 */
	public AskDataCapabilityType getType() {
		return type;
	}

	/**
	 * 获取 Provider 自身版本，仅用于诊断和审计，不参与 Core 版本范围协商。
	 *
	 * @return Provider 版本
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * 获取 Provider 可支持的最小 Core 版本。
	 *
	 * @return 最小 Core 版本
	 */
	public String getMinimumCoreVersion() {
		return minimumCoreVersion;
	}

	/**
	 * 获取 Provider 可支持的最大 Core 版本。
	 *
	 * @return 最大 Core 版本
	 */
	public String getMaximumCoreVersion() {
		return maximumCoreVersion;
	}

	/**
	 * 获取同类别候选的优先级，数值越大越优先。
	 *
	 * @return 优先级
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * 判断该能力是否可在集群模式启用。
	 *
	 * @return {@code true} 表示可在集群模式装配
	 */
	public boolean isClusterCompatible() {
		return clusterCompatible;
	}

	/**
	 * 获取实现专属配置命名空间。
	 *
	 * @return 稳定且非空白的配置前缀
	 */
	public String getConfigurationNamespace() {
		return configurationNamespace;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}
}
