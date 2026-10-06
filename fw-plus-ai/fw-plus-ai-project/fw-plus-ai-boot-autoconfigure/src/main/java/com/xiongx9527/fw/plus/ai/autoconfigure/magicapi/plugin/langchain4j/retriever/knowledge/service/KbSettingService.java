package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSetting;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSettingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 知识库全局设置的读取与热更新（取代原先 yaml 绑定的 {@code KnowledgeProperties}）。
 *
 * <p>持有不可变快照 {@link #current}，启动时从 {@code kb_setting} 单行表加载；
 * 表不存在（沿用旧库、未执行新 DDL）或无记录时回落 {@link #defaults()}。
 *
 * <p>消费方一律注入本服务并<b>每次调用 {@link #current()}</b> 取值，不在构造期固化，
 * 因此控制台「设置」页保存后 {@link #save(KbSetting)} 立即替换快照，各消费方下一次调用即生效。
 *
 * <p>加载失败不「一次判死」：{@code kb_setting} 表可能晚于本服务创建才落库（{@code spring.sql.init}
 * 在单例实例化之后才执行），此时回落默认值并按 {@link #RETRY_INTERVAL_MILLIS} 间隔在读取时重试，
 * 建表后自动切到真实配置，无需重启。
 */
public class KbSettingService implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(KbSettingService.class);

	/** 文件存储介质：本地目录 */
	public static final String STORAGE_TYPE_LOCAL = "local";

	/** 文件存储介质：复用 fw-plus-boot magicapi s3 插件 */
	public static final String STORAGE_TYPE_S3 = "s3";

	/** 默认 ES 索引名前缀 */
	public static final String DEFAULT_ES_INDEX_PREFIX = "fw-kb";

	/** 对外 Open API 的默认认证 provider：留空时保持改造前的 Sa-Token 登录态校验 */
	public static final String DEFAULT_OPEN_API_AUTH_PROVIDER = "sa-token-login";

	/** 分段模式：通用（分段直接嵌入） */
	public static final String SEGMENT_MODE_GENERAL = "general";

	/** 分段模式：父子（父段落表不嵌入，子段嵌入并带 parent_id） */
	public static final String SEGMENT_MODE_PARENT_CHILD = "parent-child";

	/** 分段模式：QA（由对话模型抽取问答对） */
	public static final String SEGMENT_MODE_QA = "qa";

	/** 合法分段模式集合（新建数据集的默认分段模式只能取这些值） */
	public static final java.util.List<String> SEGMENT_MODES =
			java.util.Collections.unmodifiableList(java.util.Arrays.asList(
					SEGMENT_MODE_GENERAL, SEGMENT_MODE_PARENT_CHILD, SEGMENT_MODE_QA));

	/** 默认召回数量 */
	public static final int DEFAULT_RETRIEVAL_TOP_K = 4;

	/** 默认分段最大长度（字符） */
	public static final int DEFAULT_SEGMENT_MAX_LENGTH = 1000;

	/** 默认分段重叠长度（字符） */
	public static final int DEFAULT_SEGMENT_OVERLAP = 200;

	/** 索引技术：高质量（分段嵌入并入向量库） */
	public static final String INDEXING_TECHNIQUE_HIGH_QUALITY = "high_quality";

	/** 索引技术：经济（不入向量库，仅关键词单路检索） */
	public static final String INDEXING_TECHNIQUE_ECONOMY = "economy";

	/** 合法索引技术集合（新建数据集的默认索引技术只能取这些值） */
	public static final java.util.List<String> INDEXING_TECHNIQUES =
			java.util.Collections.unmodifiableList(java.util.Arrays.asList(
					INDEXING_TECHNIQUE_HIGH_QUALITY, INDEXING_TECHNIQUE_ECONOMY));

	/**
	 * 加载失败后的重试间隔（毫秒）。
	 *
	 * <p>{@code spring.sql.init} 在所有单例实例化之后才执行 DDL，早于它创建的 bean 首次读表必然失败；
	 * 一次判死会让设置永远停在默认值，必须留出自愈窗口。10 秒兼顾「建表后尽快生效」与不打扰数据库。
	 */
	static final long RETRY_INTERVAL_MILLIS = 10_000L;

	private final KbSettingMapper settingMapper;

	/** 当前生效快照；整体替换而非原地改，读方无需加锁 */
	private volatile KbSetting current;

	/** 上次读表是否成功；false 时按 {@link #RETRY_INTERVAL_MILLIS} 间隔在读取路径重试 */
	private volatile boolean loaded;

	private volatile long lastLoadTime;

	public KbSettingService(KbSettingMapper settingMapper) {
		this.settingMapper = settingMapper;
		this.current = defaults();
	}

	@Override
	public void afterPropertiesSet() {
		load();
	}

	private void load() {
		try {
			KbSetting stored = settingMapper.selectById(KbSetting.SINGLETON_ID);
			current = stored == null ? defaults() : normalize(stored);
			loaded = true;
			logger.info("知识库设置已加载：storageType={}, hybridSearch={}, esEnabled={}",
					current.getStorageType(), current.getHybridSearch(), current.getEsEnabled());
		} catch (Throwable e) {
			// 沿用旧库未建 kb_setting 表时不能拖垮启动，回落默认值并给出可操作提示；
			// 不置 loaded=true，留出自愈重试（表可能稍后才由 spring.sql.init 建出来）
			current = defaults();
			loaded = false;
			logger.warn("知识库设置表（kb_setting）不可用，暂用默认设置；"
					+ "请执行 docs/sql/fw-plus-ai-knowledge.sql，建表后将自动生效。原因: {}", e.getMessage());
		}
		finally {
			lastLoadTime = System.currentTimeMillis();
		}
	}

	/** 当前生效设置的只读快照。 */
	public KbSetting current() {
		if (!loaded && System.currentTimeMillis() - lastLoadTime >= RETRY_INTERVAL_MILLIS) {
			load();
		}
		return current;
	}

	/** 运行期总开关是否启用。 */
	public boolean enabled() {
		Boolean enabled = current().getEnabled();
		return enabled == null || enabled;
	}

	/** 混合检索（向量路 + 关键词路融合）是否启用。 */
	public boolean hybridSearch() {
		Boolean hybrid = current().getHybridSearch();
		return hybrid == null || hybrid;
	}

	/** ES 全文检索是否启用。 */
	public boolean esEnabled() {
		Boolean enabled = current().getEsEnabled();
		return enabled != null && enabled;
	}

	/**
	 * 对外 Open API 当前生效的认证 provider：留空回落 {@link #DEFAULT_OPEN_API_AUTH_PROVIDER}。
	 */
	public String openApiAuthProvider() {
		String provider = current().getOpenApiAuthProvider();
		return StringUtils.hasText(provider) ? provider.trim() : DEFAULT_OPEN_API_AUTH_PROVIDER;
	}

	/**
	 * 保存设置：归一化后落库并热替换快照，返回生效后的设置。
	 *
	 * @param input 页面提交内容，{@code id} 与时间字段由本方法补齐
	 * @return 生效后的设置快照
	 * @throws IllegalArgumentException 取值非法（未知存储介质等）
	 */
	public KbSetting save(KbSetting input) {
		if (input == null) {
			throw new IllegalArgumentException("设置内容不能为空");
		}
		KbSetting normalized = normalize(input);
		normalized.setId(KbSetting.SINGLETON_ID);

		KbSetting stored = settingMapper.selectById(KbSetting.SINGLETON_ID);
		if (stored == null) {
			normalized.setCreateTime(new Date());
			normalized.setUpdateTime(new Date());
			settingMapper.insert(normalized);
		} else {
			normalized.setCreateTime(stored.getCreateTime());
			normalized.setUpdateTime(new Date());
			settingMapper.updateById(normalized);
		}

		current = normalized;
		// 表确认可用，关掉自愈重试，否则读取路径会把刚落库的值又刷回默认值
		loaded = true;
		lastLoadTime = System.currentTimeMillis();
		logger.info("知识库设置已更新并热生效：storageType={}, storagePath={}, s3Key={}, "
						+ "hybridSearch={}, esEnabled={}, esKey={}, esIndexPrefix={}, enabled={}",
				normalized.getStorageType(), normalized.getStoragePath(), normalized.getS3Key(),
				normalized.getHybridSearch(), normalized.getEsEnabled(), normalized.getEsKey(),
				normalized.getEsIndexPrefix(), normalized.getEnabled());
		return current;
	}

	/** 回落默认值（与原 {@code KnowledgeProperties} 的字段默认值一致）。 */
	public static KbSetting defaults() {
		KbSetting setting = new KbSetting();
		setting.setId(KbSetting.SINGLETON_ID);
		setting.setEnabled(Boolean.TRUE);
		setting.setStorageType(STORAGE_TYPE_LOCAL);
		// 留空表示用 LocalKbOriginalFileStore 内部的 ${user.home}/.fw-plus/kb
		setting.setStoragePath(null);
		setting.setS3Key(null);
		setting.setHybridSearch(Boolean.TRUE);
		setting.setEsEnabled(Boolean.FALSE);
		setting.setEsKey(null);
		setting.setEsIndexPrefix(DEFAULT_ES_INDEX_PREFIX);
		setting.setOpenApiAuthProvider(DEFAULT_OPEN_API_AUTH_PROVIDER);
		// 模型默认值不给内置默认：内置一个 key 只会指向一个多半不存在的资源，
		// 让页面显式配一次更可控（配错的 key 会在建数据集时报「资源不存在」，不会静默走偏）
		setting.setEmbeddingModelKey(null);
		setting.setEmbeddingStoreKey(null);
		setting.setScoringModelKey(null);
		setting.setChatModelKey(null);
		// 切分/召回参数的默认值与 kb_dataset 的 DDL 默认值保持一致（不配也能建数据集）
		setting.setDefaultRetrievalTopK(DEFAULT_RETRIEVAL_TOP_K);
		setting.setDefaultRetrievalScoreThreshold(new java.math.BigDecimal("0.0000"));
		setting.setDefaultSegmentMaxLength(DEFAULT_SEGMENT_MAX_LENGTH);
		setting.setDefaultSegmentOverlap(DEFAULT_SEGMENT_OVERLAP);
		setting.setDefaultSegmentDelimiter(null);
		setting.setDefaultSegmentMode(SEGMENT_MODE_GENERAL);
		setting.setDefaultIndexingTechnique(INDEXING_TECHNIQUE_HIGH_QUALITY);
		return setting;
	}

	/**
	 * 归一化：补默认、去空白、校验枚举值，避免把 null 带进消费方。
	 *
	 * <p>包级可见以便 {@code KbSettingDefaultsTest} 直接覆盖这些规则。
	 */
	KbSetting normalize(KbSetting input) {
		KbSetting setting = new KbSetting();
		setting.setId(KbSetting.SINGLETON_ID);
		setting.setEnabled(input.getEnabled() == null ? Boolean.TRUE : input.getEnabled());
		setting.setHybridSearch(input.getHybridSearch() == null ? Boolean.TRUE : input.getHybridSearch());
		setting.setEsEnabled(input.getEsEnabled() != null && input.getEsEnabled());

		String storageType = trimToNull(input.getStorageType());
		if (storageType == null) {
			storageType = STORAGE_TYPE_LOCAL;
		}
		storageType = storageType.toLowerCase();
		if (!STORAGE_TYPE_LOCAL.equals(storageType) && !STORAGE_TYPE_S3.equals(storageType)) {
			throw new IllegalArgumentException("未知的存储介质: " + input.getStorageType()
					+ "（可选 " + STORAGE_TYPE_LOCAL + " / " + STORAGE_TYPE_S3 + "）");
		}
		setting.setStorageType(storageType);
		setting.setStoragePath(trimToNull(input.getStoragePath()));
		setting.setS3Key(trimToNull(input.getS3Key()));
		setting.setEsKey(trimToNull(input.getEsKey()));
		// 新建数据集的模型默认值：留空即「无默认」，建数据集时该项仍需手填
		setting.setEmbeddingModelKey(trimToNull(input.getEmbeddingModelKey()));
		setting.setEmbeddingStoreKey(trimToNull(input.getEmbeddingStoreKey()));
		setting.setScoringModelKey(trimToNull(input.getScoringModelKey()));
		setting.setChatModelKey(trimToNull(input.getChatModelKey()));

		// ---------------- 切分/召回参数默认值 ----------------
		// 数值项：留空表示「不提供默认」，由建数据集时的硬编码兜底；给了就校验范围，
		// 否则一个 0 或负的重叠长度会在切分时静默产出空分段，比启动失败更难查
		setting.setDefaultRetrievalTopK(positiveOrNull(input.getDefaultRetrievalTopK(), "默认召回数量"));
		setting.setDefaultSegmentMaxLength(positiveOrNull(input.getDefaultSegmentMaxLength(), "默认分段最大长度"));
		Integer overlap = input.getDefaultSegmentOverlap();
		if (overlap != null && overlap < 0) {
			throw new IllegalArgumentException("默认分段重叠不能为负数: " + overlap);
		}
		setting.setDefaultSegmentOverlap(overlap);
		java.math.BigDecimal threshold = input.getDefaultRetrievalScoreThreshold();
		if (threshold != null && threshold.signum() < 0) {
			throw new IllegalArgumentException("默认分数阈值不能为负数: " + threshold);
		}
		setting.setDefaultRetrievalScoreThreshold(threshold);

		setting.setDefaultSegmentDelimiter(trimToNull(input.getDefaultSegmentDelimiter()));
		String segmentMode = trimToNull(input.getDefaultSegmentMode());
		if (segmentMode == null) {
			segmentMode = SEGMENT_MODE_GENERAL;
		}
		if (!SEGMENT_MODES.contains(segmentMode)) {
			throw new IllegalArgumentException("不支持的默认分段模式: " + input.getDefaultSegmentMode()
					+ "（可选 " + String.join(" / ", SEGMENT_MODES) + "）");
		}
		setting.setDefaultSegmentMode(segmentMode);

		String indexingTechnique = trimToNull(input.getDefaultIndexingTechnique());
		if (indexingTechnique == null) {
			indexingTechnique = INDEXING_TECHNIQUE_HIGH_QUALITY;
		}
		if (!INDEXING_TECHNIQUES.contains(indexingTechnique)) {
			throw new IllegalArgumentException("不支持的默认索引技术: " + input.getDefaultIndexingTechnique()
					+ "（可选 " + String.join(" / ", INDEXING_TECHNIQUES) + "）");
		}
		setting.setDefaultIndexingTechnique(indexingTechnique);

		String prefix = trimToNull(input.getEsIndexPrefix());
		setting.setEsIndexPrefix(prefix == null ? DEFAULT_ES_INDEX_PREFIX : prefix);

		// 对外 Open API 认证 provider：留空回落默认；未知取值不在保存期拦截，由请求期 fail-closed。
		String authProvider = trimToNull(input.getOpenApiAuthProvider());
		setting.setOpenApiAuthProvider(authProvider == null ? DEFAULT_OPEN_API_AUTH_PROVIDER : authProvider);
		return setting;
	}

	/** 非空且必须为正数，否则视为非法配置。 */
	private Integer positiveOrNull(Integer value, String field) {
		if (value != null && value <= 0) {
			throw new IllegalArgumentException(field + "必须大于 0: " + value);
		}
		return value;
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
