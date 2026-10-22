package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.uid;

import cn.hutool.core.date.DateUtil;
import com.xiongx9527.fw.plus.boot.autoconfigure.uid.jdbc.worker.DisposableWorkerIdAssigner;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.uid.UidGenerator;
import com.xiongx9527.fw.plus.common.uid.impl.CachedUidGenerator;
import com.xiongx9527.fw.plus.common.uid.worker.WorkerIdAssigner;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Magic-API uid 模块，支持多数据源索引访问。
 *
 * <p>默认模式（{@code uid.getUID()}）：使用全局 {@link UidGenerator} bean。</p>
 * <p>多数据源模式（{@code uid['xxx'].getUID()}）：按数据源 key 取 {@link MagicDynamicDataSource}
 * 中对应数据源，创建独立的 {@link CachedUidGenerator}（worker_node 表在对应库中）。</p>
 */
@MagicModule("uid")
public class UidModule implements DynamicAttribute<UidModule, UidModule>, DynamicModule<UidModule>, ApplicationContextAware {

	/**
	 * Magic-API 动态数据源，用于按 key 获取数据源（与 db['xxx'] 同套 key 体系）
	 */
	private final MagicDynamicDataSource magicDynamicDataSource;

	/**
	 * UID 纪元时间配置（与 fw.plus.uid.epochStr 一致）
	 */
	private final String epochStr;

	/**
	 * 按数据源 key 缓存的 UidGenerator（每个数据源独立 workerId 分配）
	 */
	private final ConcurrentHashMap<String, UidGenerator> generatorCache = new ConcurrentHashMap<>();

	/**
	 * 当前实例绑定的数据源 key（null = 默认模式）
	 */
	private String key;

	/**
	 * 数据源模式下的 UidGenerator
	 */
	private UidGenerator dataSourceGenerator;

	public UidModule(MagicDynamicDataSource magicDynamicDataSource, String epochStr) {
		this.magicDynamicDataSource = magicDynamicDataSource;
		this.epochStr = epochStr;
	}

	/**
	 * 获取唯一标识符（UID）
	 *
	 * @return long 返回一个唯一的长整型标识符
	 */
	public long getUID() {
		return getGenerator().getUID();
	}

	/**
	 * 解析 UID 为元素信息
	 *
	 * @param uid 需要解析的 UID 值
	 * @return 解析后的信息字符串
	 */
	public String parseUID(long uid) {
		return getGenerator().parseUID(uid);
	}

	/**
	 * 获取当前实例使用的 UidGenerator。
	 * 默认模式（key == null）使用全局 UidGenerator bean；
	 * 数据源模式使用按 key 创建的独立 generator。
	 */
	private UidGenerator getGenerator() {
		if (key == null) {
			return SpringUtils.getBean(UidGenerator.class);
		}
		return dataSourceGenerator;
	}

	/**
	 * 根据数据源 key 获取动态 UidModule 实例（处理 {@code uid['xxx']} 索引访问）。
	 * key 为空或 default 时返回当前实例（默认模式）。
	 *
	 * @param key 数据源标识键
	 * @return 绑定指定数据源的 UidModule 实例
	 */
	@Override
	public UidModule getDynamicAttribute(String key) {
		if (key == null || Options.DEFAULT_DATA_SOURCE.getValue().equalsIgnoreCase(key)
				|| "default".equalsIgnoreCase(key)) {
			return this;
		}
		return createForDataSource(key);
	}

	/**
	 * 根据脚本上下文获取动态 UidModule 实例。
	 * 从上下文读取默认数据源配置，未配置则返回当前实例。
	 *
	 * @param context MagicScript 脚本上下文
	 * @return 绑定指定数据源的 UidModule 实例或当前实例
	 */
	@Override
	public UidModule getDynamicModule(MagicScriptContext context) {
		String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		if (StringUtils.isEmpty(dataSourceKey)) {
			return this;
		}
		return createForDataSource(dataSourceKey);
	}

	/**
	 * 按数据源 key 创建绑定该数据源的 UidModule 实例。
	 * generator 按 key 缓存，同一数据源复用同一实例（workerId 只分配一次）。
	 */
	private UidModule createForDataSource(String dataSourceKey) {
		if (magicDynamicDataSource == null) {
			throw new IllegalStateException(I18nMessageResolver.resolveKey("fw.boot.uid.dynamicDataSourceNotConfigured"));
		}
		UidModule module = new UidModule(this.magicDynamicDataSource, this.epochStr);
		module.key = dataSourceKey;
		module.dataSourceGenerator = generatorCache.computeIfAbsent(dataSourceKey, this::createGenerator);
		return module;
	}

	/**
	 * 为指定数据源创建独立的 CachedUidGenerator。
	 * workerId 从该数据源的 worker_node 表分配（与全局模式互不影响）。
	 */
	private UidGenerator createGenerator(String dataSourceKey) {
		MagicDynamicDataSource.DataSourceNode node = magicDynamicDataSource.getDataSource(dataSourceKey);
		if (node == null || node.getDataSource() == null) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.uid.datasourceNotExists", dataSourceKey));
		}
		DataSource dataSource = node.getDataSource();
		WorkerIdAssigner workerIdAssigner = new DisposableWorkerIdAssigner(new JdbcTemplate(dataSource));
		CachedUidGenerator generator = new CachedUidGenerator();
		if (StringUtils.isNotBlank(epochStr)) {
			generator.setEpochStr(epochStr);
		} else {
			generator.setEpochStr(DateUtil.format(new Date(), "yyyy-MM-dd"));
		}
		generator.setWorkerIdAssigner(workerIdAssigner);
		try {
			generator.afterPropertiesSet();
		} catch (Exception e) {
			throw new IllegalStateException(I18nMessageResolver.resolveKey("fw.boot.uid.initUidGeneratorFailed", dataSourceKey), e);
		}
		return generator;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
	}

}
