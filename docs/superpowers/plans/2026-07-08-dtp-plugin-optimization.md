# DTP Plugin Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 标准化并严格优化 MagicAPI DTP 插件，补齐配置校验、线程池生命周期、自动配置覆盖点、脚本模块行为和回归测试。

**Architecture:** 保留 `magic-api.plugin.dtp` 配置前缀、`dtp-datasource/*.json` 资源格式和 `import dtp` 脚本入口。新增聚合校验工具类，动态数据源先构建后替换，自动配置复用容器 bean，模块指定不存在数据源时抛清晰异常。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Magic-API 2.2.2、DynamicTp 1.2.2、JUnit 5、AssertJ、ApplicationContextRunner。

## Global Constraints

- 使用简体中文沟通和提交说明。
- 目标源码保持 Java 1.8 兼容，不使用 Java 9+ API。
- 不引入新的外部依赖版本。
- 不升级 Spring Boot、DynamicTp、MagicAPI 或 Magic-API 插件依赖版本。
- 保留配置前缀 `magic-api.plugin.dtp`。
- 保留资源目录 `dtp-datasource` 和 `.json` 格式。
- 保留脚本侧 `import dtp`、`dtp.execute(...)`、`dtp['key']` 的使用方式。
- 指定不存在 DTP 数据源时不再回退默认线程池，改为抛出清晰异常。
- 使用系统 `mvn` 执行验证；当前仓库 Maven Wrapper 缺 wrapper 文件。
- 不提交已有无关未跟踪文件：`docs/superpowers/reports/2026-07-07-optimize-web-autoconfigure-verify.md`、`fw-plus-boot/.../src/test/java/.../web/`、`openspec/changes/archive/2026-07-07-optimize-web-autoconfigure/`、`openspec/specs/web-slow-request-monitoring/`。

---

## File Structure

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/model/DtpDataSourceValidator.java`
  - 集中校验 DTP 数据源字段和配置枚举。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/model/MagicDynamicDtpDataSource.java`
  - 使用 validator，先构建后替换 executor，补查询和销毁方法。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/common/BaseService.java`
  - 复用 validator 做资源级校验。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/service/DtpDataSourceInfoMagicResourceStorage.java`
  - 校验 key 文件名、重复 key、key 不可改、线程池参数。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/service/DtpDataSourceMagicDynamicRegistry.java`
  - 记录事件、注册、注销异常，不静默吞掉诊断信息。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/starter/MagicAPIDtpConfiguration.java`
  - 标准化自动配置、补 backoff、controller 复用同一个数据源 bean。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/module/DtpModule.java`
  - 数据源缺失抛异常，空任务列表返回非 null Future。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/module/MagicApiFuncTask.java`
  - 补函数名日志上下文。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/module/MagicApiScriptTask.java`
  - 补脚本日志上下文。
- Create tests under: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/`

---

### Task 1: DTP 数据源校验

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/model/DtpDataSourceValidator.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/common/BaseService.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/service/DtpDataSourceInfoMagicResourceStorage.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/DtpDataSourceValidatorTest.java`

**Interfaces:**
- Produces: `DtpDataSourceValidator.validate(DtpDataSourceInfo info)`
- Produces: `DtpDataSourceValidator.validateOtherJson(String other)`
- Produces: `DtpDataSourceValidator.isSupportedQueueType(String queueType)`
- Produces: `DtpDataSourceValidator.isSupportedRejectedPolicy(String rejectedPolicy)`
- Produces: `DtpDataSourceValidator.isBoundedQueue(String queueType)`

- [ ] **Step 1: Write the failing validator test**

Create `DtpDataSourceValidatorTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtpDataSourceValidatorTest {

	@Test
	void validInfoPassesValidation() {
		DtpDataSourceInfo info = validInfo();

		assertThatCode(() -> DtpDataSourceValidator.validate(info)).doesNotThrowAnyException();
	}

	@Test
	void rejectsBlankKey() {
		DtpDataSourceInfo info = validInfo();
		info.setKey(" ");

		assertThatThrownBy(() -> DtpDataSourceValidator.validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("key");
	}

	@Test
	void rejectsInvalidPoolSizes() {
		DtpDataSourceInfo zeroCore = validInfo();
		zeroCore.setCorePoolSize(0);
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(zeroCore))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("corePoolSize");

		DtpDataSourceInfo maxLessThanCore = validInfo();
		maxLessThanCore.setCorePoolSize(4);
		maxLessThanCore.setMaximumPoolSize(2);
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(maxLessThanCore))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("maximumPoolSize");
	}

	@Test
	void rejectsInvalidQueueAndRejectedPolicy() {
		DtpDataSourceInfo badQueue = validInfo();
		badQueue.setQueueType("unknown");
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(badQueue))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("queueType");

		DtpDataSourceInfo badPolicy = validInfo();
		badPolicy.setRejectedPolicy("unknown");
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(badPolicy))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("rejectedPolicy");
	}

	@Test
	void validatesOtherJsonObject() {
		DtpDataSourceInfo arrayJson = validInfo();
		arrayJson.setOther("[]");
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(arrayJson))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("other");

		DtpDataSourceInfo invalidJson = validInfo();
		invalidJson.setOther("{");
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(invalidJson))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("other");
	}

	@Test
	void unboundedQueuesDoNotRequirePositiveCapacity() {
		DtpDataSourceInfo synchronous = validInfo();
		synchronous.setQueueType("synchronous");
		synchronous.setQueueCapacity(0);

		assertThatCode(() -> DtpDataSourceValidator.validate(synchronous)).doesNotThrowAnyException();
	}

	private DtpDataSourceInfo validInfo() {
		DtpDataSourceInfo info = new DtpDataSourceInfo();
		info.setName("任务池");
		info.setKey("bizPool");
		info.setCorePoolSize(2);
		info.setMaximumPoolSize(4);
		info.setKeepAliveTime(30);
		info.setQueueType("linked");
		info.setQueueCapacity(128);
		info.setRejectedPolicy("abort");
		info.setOther("{\"awaitTerminationSeconds\":1}");
		return info;
	}
}
```

- [ ] **Step 2: Run validator test and verify it fails**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=DtpDataSourceValidatorTest test
```

Expected: FAIL because `DtpDataSourceValidator` does not exist.

- [ ] **Step 3: Implement `DtpDataSourceValidator`**

Create `DtpDataSourceValidator.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DtpDataSourceValidator {

	private static final Set<String> QUEUE_TYPES = new HashSet<>(Arrays.asList(
			"array", "synchronous", "linked", "priority", "delayed", "linkedtransferqueue", "linkedblockingdeque"));

	private static final Set<String> UNBOUNDED_OR_CAPACITY_IGNORED_QUEUE_TYPES = new HashSet<>(Arrays.asList(
			"synchronous", "linkedtransferqueue", "delayed"));

	private static final Set<String> REJECTED_POLICIES = new HashSet<>(Arrays.asList(
			"caller_runs", "abort", "discard", "discard_oldest"));

	private DtpDataSourceValidator() {
	}

	public static void validate(DtpDataSourceInfo info) {
		if (info == null) {
			throw new IllegalArgumentException("DTP 数据源配置不能为空");
		}
		requireText(info.getKey(), "key不能为空");
		if (info.getCorePoolSize() <= 0) {
			throw new IllegalArgumentException("corePoolSize必须大于0");
		}
		if (info.getMaximumPoolSize() < info.getCorePoolSize()) {
			throw new IllegalArgumentException("maximumPoolSize必须大于等于corePoolSize");
		}
		if (info.getKeepAliveTime() < 0) {
			throw new IllegalArgumentException("keepAliveTime不能小于0");
		}
		String queueType = normalizeQueueType(info.getQueueType());
		if (!isSupportedQueueType(queueType)) {
			throw new IllegalArgumentException("queueType不支持: " + info.getQueueType());
		}
		if (isBoundedQueue(queueType) && info.getQueueCapacity() <= 0) {
			throw new IllegalArgumentException("queueCapacity必须大于0");
		}
		String rejectedPolicy = normalizeRejectedPolicy(info.getRejectedPolicy());
		if (!isSupportedRejectedPolicy(rejectedPolicy)) {
			throw new IllegalArgumentException("rejectedPolicy不支持: " + info.getRejectedPolicy());
		}
		validateOtherJson(info.getOther());
	}

	public static void validateOtherJson(String other) {
		if (StringUtils.isBlank(other)) {
			return;
		}
		try {
			Object parsed = JSON.parse(other);
			if (!(parsed instanceof JSONObject)) {
				throw new IllegalArgumentException("other必须是JSON对象");
			}
		} catch (JSONException ex) {
			throw new IllegalArgumentException("other必须是合法JSON对象", ex);
		}
	}

	public static boolean isSupportedQueueType(String queueType) {
		return QUEUE_TYPES.contains(normalizeQueueType(queueType));
	}

	public static boolean isSupportedRejectedPolicy(String rejectedPolicy) {
		return REJECTED_POLICIES.contains(normalizeRejectedPolicy(rejectedPolicy));
	}

	public static boolean isBoundedQueue(String queueType) {
		return !UNBOUNDED_OR_CAPACITY_IGNORED_QUEUE_TYPES.contains(normalizeQueueType(queueType));
	}

	public static String normalizeQueueType(String queueType) {
		return StringUtils.defaultIfBlank(queueType, "linked").toLowerCase(Locale.ENGLISH);
	}

	public static String normalizeRejectedPolicy(String rejectedPolicy) {
		return StringUtils.defaultIfBlank(rejectedPolicy, "abort").toLowerCase(Locale.ENGLISH);
	}

	private static void requireText(String value, String message) {
		if (StringUtils.isBlank(value)) {
			throw new IllegalArgumentException(message);
		}
	}
}
```

- [ ] **Step 4: Reuse validator from `BaseService` and storage**

Modify `BaseService.validateField`:

```java
default void validateField(DtpDataSourceInfo entity) {
	validateRequiredField(entity.getName(), "name");
	validateRequiredField(entity.getKey(), "key");
	DtpDataSourceValidator.validate(entity);
}
```

Add import:

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceValidator;
```

Modify `DtpDataSourceInfoMagicResourceStorage.validate`:

```java
@Override
public void validate(DtpDataSourceInfo entity) {
	validateField(entity);
	if (!IoUtils.validateFileName(entity.getKey())) {
		throw new IllegalArgumentException("key包含非法文件名字符");
	}
	validateKeyUnique(entity.getKey(), entity.getId());

	if (entity.getId() != null) {
		DtpDataSourceInfo oldInfo = magicResourceService.file(entity.getId());
		if (oldInfo != null && !oldInfo.getKey().equals(entity.getKey())) {
			throw new IllegalArgumentException("key不可修改");
		}
	}
}
```

Add import:

```java
import org.ssssssss.magicapi.utils.IoUtils;
```

- [ ] **Step 5: Run validator test and verify it passes**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=DtpDataSourceValidatorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/model/DtpDataSourceValidator.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/common/BaseService.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/service/DtpDataSourceInfoMagicResourceStorage.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/DtpDataSourceValidatorTest.java
git commit -m "feat: 强化 DTP 数据源校验"
```

---

### Task 2: DTP 动态数据源生命周期

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/model/MagicDynamicDtpDataSource.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/MagicDynamicDtpDataSourceTest.java`

**Interfaces:**
- Consumes: `DtpDataSourceValidator.validate(DtpDataSourceInfo info)`
- Produces: `MagicDynamicDtpDataSource.createClient(DtpDataSourceInfo info)` that builds before replacing.
- Produces: `MagicDynamicDtpDataSource.getInfo(String key)`
- Produces: `MagicDynamicDtpDataSource.size()`

- [ ] **Step 1: Write failing lifecycle tests**

Create `MagicDynamicDtpDataSourceTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagicDynamicDtpDataSourceTest {

	@Test
	void buildClientAppliesPoolQueueThreadNameAndRejectedPolicy() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();

		DtpExecutor executor = dataSource.buildClient(validInfo("bizPool"));

		assertThat(executor.getCorePoolSize()).isEqualTo(2);
		assertThat(executor.getMaximumPoolSize()).isEqualTo(4);
		assertThat(executor.getQueue().remainingCapacity()).isEqualTo(128);
		assertThat(executor.getThreadFactory().newThread(() -> {}).getName()).startsWith("bizPool-thread-");
		executor.shutdownNow();
	}

	@Test
	void createClientReplacesOnlyAfterNewExecutorBuildsSuccessfully() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();
		DtpDataSourceInfo oldInfo = validInfo("bizPool");
		oldInfo.setQueueCapacity(64);
		dataSource.createClient(oldInfo);
		DtpExecutor oldExecutor = dataSource.getClient("bizPool");

		DtpDataSourceInfo invalidNewInfo = validInfo("bizPool");
		invalidNewInfo.setMaximumPoolSize(1);

		assertThatThrownBy(() -> dataSource.createClient(invalidNewInfo))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("maximumPoolSize");
		assertThat(dataSource.getClient("bizPool")).isSameAs(oldExecutor);
		assertThat(dataSource.getInfo("bizPool").getQueueCapacity()).isEqualTo(64);

		dataSource.destroy();
	}

	@Test
	void destroyClientByKeyRemovesInfoAndExecutor() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();
		dataSource.createClient(validInfo("bizPool"));

		dataSource.destroyClientByKey("bizPool");

		assertThat(dataSource.getClient("bizPool")).isNull();
		assertThat(dataSource.getInfo("bizPool")).isNull();
		assertThat(dataSource.size()).isZero();
	}

	@Test
	void destroyClearsAllClients() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();
		dataSource.createClient(validInfo("poolA"));
		dataSource.createClient(validInfo("poolB"));

		dataSource.destroy();

		assertThat(dataSource.size()).isZero();
	}

	private DtpDataSourceInfo validInfo(String key) {
		DtpDataSourceInfo info = new DtpDataSourceInfo();
		info.setName(key);
		info.setKey(key);
		info.setCorePoolSize(2);
		info.setMaximumPoolSize(4);
		info.setKeepAliveTime(30);
		info.setQueueType("linked");
		info.setQueueCapacity(128);
		info.setRejectedPolicy("abort");
		return info;
	}
}
```

- [ ] **Step 2: Run lifecycle tests and verify they fail**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicDynamicDtpDataSourceTest,DtpDataSourceValidatorTest test
```

Expected: FAIL because `getInfo` and `size` do not exist and create-update behavior is not safe.

- [ ] **Step 3: Implement safe lifecycle in `MagicDynamicDtpDataSource`**

Modify `buildClient` to validate and use normalized config:

```java
public DtpExecutor buildClient(DtpDataSourceInfo info) {
	DtpDataSourceValidator.validate(info);
	DtpExecutor client = new DtpExecutor(
			info.getCorePoolSize(),
			info.getMaximumPoolSize(),
			info.getKeepAliveTime(),
			TimeUnit.SECONDS,
			createWorkQueue(info),
			createThreadFactory(info),
			createRejectedHandler(info)
	);
	client.setThreadPoolName(info.getKey());
	configureDtpExecutorFromOtherInfo(client, info);
	return client;
}
```

Modify `createWorkQueue` to use validator normalization:

```java
private BlockingQueue<Runnable> createWorkQueue(DtpDataSourceInfo info) {
	String queueType = DtpDataSourceValidator.normalizeQueueType(
			MagicAPIConfigUtils.getDtpConfig(info.getKey(), "queueType", info.getQueueType()));
	int capacity = Integer.parseInt(MagicAPIConfigUtils.getDtpConfig(info.getKey(), "queueCapacity",
			String.valueOf(info.getQueueCapacity())));
	if (DtpDataSourceValidator.isBoundedQueue(queueType) && capacity <= 0) {
		throw new IllegalArgumentException("queueCapacity必须大于0");
	}
	switch (queueType) {
		case "array":
			return new ArrayBlockingQueue<>(capacity);
		case "synchronous":
			return new SynchronousQueue<>();
		case "linked":
			return new LinkedBlockingQueue<>(capacity);
		case "priority":
			return new PriorityBlockingQueue<>(capacity);
		case "delayed":
			return new DelayQueue();
		case "linkedtransferqueue":
			return new LinkedTransferQueue<>();
		case "linkedblockingdeque":
			return new LinkedBlockingDeque<>(capacity);
		default:
			throw new IllegalArgumentException("queueType不支持: " + queueType);
	}
}
```

Modify `createRejectedHandler`:

```java
private RejectedExecutionHandler createRejectedHandler(DtpDataSourceInfo info) {
	String rejectedPolicy = DtpDataSourceValidator.normalizeRejectedPolicy(
			MagicAPIConfigUtils.getDtpConfig(info.getKey(), "rejectedPolicy", info.getRejectedPolicy()));
	switch (rejectedPolicy) {
		case "caller_runs":
			return new ThreadPoolExecutor.CallerRunsPolicy();
		case "abort":
			return new ThreadPoolExecutor.AbortPolicy();
		case "discard":
			return new ThreadPoolExecutor.DiscardPolicy();
		case "discard_oldest":
			return new ThreadPoolExecutor.DiscardOldestPolicy();
		default:
			throw new IllegalArgumentException("rejectedPolicy不支持: " + rejectedPolicy);
	}
}
```

Modify `createClient`:

```java
public void createClient(DtpDataSourceInfo info) {
	DtpExecutor client = buildClient(info);
	DtpExecutor oldClient = this.clientMap.put(info.getKey(), client);
	this.infoMap.put(info.getKey(), (DtpDataSourceInfo) info.copy());
	if (oldClient != null) {
		destroyClient(oldClient);
	}
}
```

Add methods:

```java
public DtpDataSourceInfo getInfo(String key) {
	return this.infoMap.get(key);
}

public int size() {
	return this.clientMap.size();
}
```

Add import:

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceValidator;
```

- [ ] **Step 4: Run lifecycle tests and verify they pass**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicDynamicDtpDataSourceTest,DtpDataSourceValidatorTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/model/MagicDynamicDtpDataSource.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/MagicDynamicDtpDataSourceTest.java
git commit -m "feat: 强化 DTP 动态数据源生命周期"
```

---

### Task 3: DTP 自动配置和注册器

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/starter/MagicAPIDtpConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/service/DtpDataSourceMagicDynamicRegistry.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/MagicAPIDtpConfigurationTest.java`

**Interfaces:**
- Consumes: `MagicDynamicDtpDataSource`
- Produces: DTP auto-config defaults with `@ConditionalOnMissingBean`.
- Produces: `MagicAPIDtpConfiguration(ObjectProvider<MagicDynamicDtpDataSource> magicDynamicDtpDataSourceProvider)` constructor.

- [ ] **Step 1: Write failing auto-configuration tests**

Create `MagicAPIDtpConfigurationTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module.DtpModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service.DtpDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service.DtpDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.starter.MagicAPIDtpConfiguration;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIDtpConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MagicAPIDtpConfiguration.class));

	@Test
	void backsOffWhenPluginDisabled() {
		contextRunner
				.withPropertyValues("magic-api.plugin.dtp.enabled=false")
				.run(context -> assertThat(context)
						.doesNotHaveBean(MagicDynamicDtpDataSource.class)
						.doesNotHaveBean(DtpExecutor.class)
						.doesNotHaveBean(DtpModule.class));
	}

	@Test
	void createsDefaultBeansWhenEnabled() {
		contextRunner
				.withPropertyValues("magic-api.plugin.dtp.enabled=true")
				.run(context -> assertThat(context)
						.hasSingleBean(MagicDynamicDtpDataSource.class)
						.hasSingleBean(DtpExecutor.class)
						.hasSingleBean(DtpModule.class)
						.hasSingleBean(DtpDataSourceInfoMagicResourceStorage.class)
						.hasSingleBean(DtpDataSourceMagicDynamicRegistry.class));
	}

	@Test
	void backsOffForUserBeans() {
		contextRunner
				.withPropertyValues("magic-api.plugin.dtp.enabled=true")
				.withUserConfiguration(UserDtpConfiguration.class)
				.run(context -> {
					assertThat(context).hasSingleBean(MagicDynamicDtpDataSource.class);
					assertThat(context).hasSingleBean(DtpExecutor.class);
					assertThat(context).hasSingleBean(DtpModule.class);
					assertThat(context).hasSingleBean(DtpDataSourceInfoMagicResourceStorage.class);
					assertThat(context).hasSingleBean(DtpDataSourceMagicDynamicRegistry.class);
					assertThat(context.getBean(MagicDynamicDtpDataSource.class))
							.isSameAs(context.getBean("customDynamicDtpDataSource"));
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class UserDtpConfiguration {

		@Bean
		MagicDynamicDtpDataSource customDynamicDtpDataSource() {
			return new MagicDynamicDtpDataSource();
		}

		@Bean
		DtpExecutor customDtpExecutor() {
			return new DtpExecutor(1, 1, 0L, java.util.concurrent.TimeUnit.SECONDS,
					new java.util.concurrent.LinkedBlockingQueue<>(1));
		}

		@Bean
		DtpModule customDtpModule(MagicDynamicDtpDataSource dataSource, DtpExecutor executor) {
			return new DtpModule(dataSource, executor);
		}

		@Bean
		DtpDataSourceInfoMagicResourceStorage customStorage() {
			return new DtpDataSourceInfoMagicResourceStorage();
		}

		@Bean
		DtpDataSourceMagicDynamicRegistry customRegistry() {
			return mock(DtpDataSourceMagicDynamicRegistry.class);
		}
	}
}
```

- [ ] **Step 2: Run auto-configuration tests and verify they fail**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIDtpConfigurationTest test
```

Expected: FAIL because `DtpModule` and storage do not back off completely.

- [ ] **Step 3: Standardize `MagicAPIDtpConfiguration`**

Replace class shape:

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "magic-api.plugin.dtp", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.dromara.dynamictp.core.executor.DtpExecutor"})
@EnableConfigurationProperties(MagicAPIDtpConfig.class)
public class MagicAPIDtpConfiguration implements MagicPluginConfiguration, BaseService {

	private final ObjectProvider<MagicDynamicDtpDataSource> magicDynamicDtpDataSourceProvider;

	public MagicAPIDtpConfiguration(ObjectProvider<MagicDynamicDtpDataSource> magicDynamicDtpDataSourceProvider) {
		this.magicDynamicDtpDataSourceProvider = magicDynamicDtpDataSourceProvider;
	}
```

Add import:

```java
import org.springframework.beans.factory.ObjectProvider;
```

Add `@ConditionalOnMissingBean`:

```java
@Bean
@ConditionalOnMissingBean
public DtpModule dtpModule(MagicDynamicDtpDataSource magicDynamicDtpDataSource, DtpExecutor dtpExecutor) {
	return new DtpModule(magicDynamicDtpDataSource, dtpExecutor);
}

@Bean
@ConditionalOnMissingBean
public DtpDataSourceInfoMagicResourceStorage dtpDataSourceInfoMagicResourceStorage() {
	return new DtpDataSourceInfoMagicResourceStorage();
}
```

Modify `controllerRegister`:

```java
@Override
public MagicControllerRegister controllerRegister() {
	return (mapping, configuration) -> mapping.registerController(
			new MagicDtpController(configuration, this.magicDynamicDtpDataSourceProvider.getObject()));
}
```

- [ ] **Step 4: Improve registry diagnostics**

Modify `DtpDataSourceMagicDynamicRegistry` event handlers:

```java
@EventListener(condition = "#event.type == 'dtp-datasource'")
public void onFileEvent(FileEvent event) {
	try {
		processEvent(event);
	} catch (Exception e) {
		log.warn("处理 DTP 数据源文件事件失败: {}", event, e);
	}
}

@EventListener(condition = "#event.type == 'dtp-datasource'")
public void onGroupEvent(GroupEvent event) {
	try {
		processEvent(event);
	} catch (Exception e) {
		log.warn("处理 DTP 数据源分组事件失败: {}", event, e);
	}
}
```

Modify register/unregister catches to log the key and exception:

```java
@Override
public boolean register(DtpDataSourceInfo entity) {
	try {
		return super.register(entity);
	} catch (Exception e) {
		log.warn("注册 DTP 数据源失败: {}", entity == null ? null : entity.getKey(), e);
		return false;
	}
}
```

Keep `protected register(MappingNode<DtpDataSourceInfo> mappingNode)` creating the client before `super.register(mappingNode)`, relying on Task 2 safe replacement.

- [ ] **Step 5: Run auto-configuration and previous DTP tests**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIDtpConfigurationTest,MagicDynamicDtpDataSourceTest,DtpDataSourceValidatorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/starter/MagicAPIDtpConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/service/DtpDataSourceMagicDynamicRegistry.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/MagicAPIDtpConfigurationTest.java
git commit -m "feat: 标准化 DTP 插件自动配置"
```

---

### Task 4: DTP 模块脚本 API 行为

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/module/DtpModule.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/module/MagicApiFuncTask.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/module/MagicApiScriptTask.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/DtpModuleTest.java`

**Interfaces:**
- Consumes: `MagicDynamicDtpDataSource.createClient(DtpDataSourceInfo info)`
- Produces: `DtpModule.getDynamicAttribute(String key)` throws `IllegalArgumentException` when key is missing.
- Produces: empty task list methods returning non-null completed futures.

- [ ] **Step 1: Write failing module tests**

Create `DtpModuleTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module.DtpModule;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtpModuleTest {

	@Test
	void dynamicAttributeUsesNamedExecutor() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();
		dataSource.createClient(validInfo("bizPool"));
		DtpExecutor defaultExecutor = new DtpExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));
		DtpModule module = new DtpModule(dataSource, defaultExecutor);

		DtpModule dynamic = module.getDynamicAttribute("bizPool");

		assertThat(dynamic.getDtpExecutor()).isSameAs(dataSource.getClient("bizPool"));
		dataSource.destroy();
		defaultExecutor.shutdownNow();
	}

	@Test
	void missingDynamicAttributeThrowsClearException() {
		DtpModule module = new DtpModule(new MagicDynamicDtpDataSource(),
				new DtpExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1)));

		assertThatThrownBy(() -> module.getDynamicAttribute("missing"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("missing");

		module.getDtpExecutor().shutdownNow();
	}

	@Test
	void emptyTaskListsReturnCompletedFutures() throws Exception {
		DtpExecutor executor = new DtpExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));
		DtpModule module = new DtpModule(new MagicDynamicDtpDataSource(), executor);

		CompletableFuture<List<Object>> all = module.allOfWithAllResults(Collections.emptyList());
		CompletableFuture<Object> any = module.anyOfWithFirstResult(Collections.emptyList());
		CompletableFuture<Void> anyVoid = module.anyOfWithoutResult(Collections.emptyList());

		assertThat(all).isCompletedWithValue(Collections.emptyList());
		assertThat(any).isCompletedWithValue(null);
		assertThat(anyVoid).isDone();
		executor.shutdownNow();
	}

	private DtpDataSourceInfo validInfo(String key) {
		DtpDataSourceInfo info = new DtpDataSourceInfo();
		info.setName(key);
		info.setKey(key);
		info.setCorePoolSize(1);
		info.setMaximumPoolSize(1);
		info.setKeepAliveTime(0);
		info.setQueueType("linked");
		info.setQueueCapacity(8);
		info.setRejectedPolicy("abort");
		return info;
	}
}
```

- [ ] **Step 2: Run module tests and verify they fail**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=DtpModuleTest test
```

Expected: FAIL because missing key falls back and empty task lists return null.

- [ ] **Step 3: Implement module behavior**

Modify empty list returns:

```java
public <T> CompletableFuture<List<T>> allOfWithAllResults(List<Supplier<T>> tasks) {
	if (CollectionUtils.isEmpty(tasks)) {
		return CompletableFuture.completedFuture(java.util.Collections.emptyList());
	}
	...
}

public <T> CompletableFuture<T> anyOfWithFirstResult(List<Supplier<T>> tasks) {
	if (CollectionUtils.isEmpty(tasks)) {
		return CompletableFuture.completedFuture(null);
	}
	...
}

public CompletableFuture<Void> anyOfWithoutResult(List<Runnable> tasks) {
	if (CollectionUtils.isEmpty(tasks)) {
		return CompletableFuture.completedFuture(null);
	}
	...
}
```

Modify data source resolution:

```java
private DtpModule moduleForKey(String key) {
	DtpExecutor client = this.magicDynamicDtpDataSource.getClient(key);
	if (client == null) {
		throw new IllegalArgumentException("DTP数据源不存在: " + key);
	}
	return new DtpModule(this.magicDynamicDtpDataSource, client);
}

@Override
public DtpModule getDynamicModule(MagicScriptContext context) {
	String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
	if (StringUtils.isEmpty(dataSourceKey)) {
		return this;
	}
	return moduleForKey(dataSourceKey);
}

@Override
public DtpModule getDynamicAttribute(String key) {
	return moduleForKey(key);
}
```

Add import if needed:

```java
import java.util.Collections;
```

- [ ] **Step 4: Improve task logs**

Modify `MagicApiFuncTask` catch:

```java
} catch (Exception e) {
	log.warn("执行函数调用失败，funcName={}：{}", funcName, e.getMessage(), e);
	return java.util.Collections.emptyMap();
}
```

Modify `MagicApiScriptTask` catch:

```java
} catch (Exception e) {
	log.warn("执行脚本失败，scriptPrefix={}：{}", script == null ? null : script.substring(0, Math.min(script.length(), 80)), e.getMessage(), e);
	return java.util.Collections.emptyMap();
}
```

- [ ] **Step 5: Run all DTP tests**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Dtp*Test' test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/module/DtpModule.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/module/MagicApiFuncTask.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/module/MagicApiScriptTask.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp/DtpModuleTest.java
git commit -m "feat: 严格化 DTP 模块脚本行为"
```

---

### Task 5: 最终验证

**Files:**
- Modify only if verification exposes compile or test gaps in files from Tasks 1-4.

**Interfaces:**
- Consumes: all DTP implementation tasks.
- Produces: passing targeted DTP and related MagicAPI tests.

- [ ] **Step 1: Run DTP tests**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Dtp*Test' test
```

Expected: PASS.

- [ ] **Step 2: Run related MagicAPI plugin tests**

Run:

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Dtp*Test,*MagicAPI*Test' test
```

Expected: PASS. If the wildcard selects no additional MagicAPI tests in this Maven/Surefire version, run the known related tests available under `src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi`.

- [ ] **Step 3: Check worktree**

Run:

```bash
git status --short
```

Expected: no tracked DTP changes remain after commits. Known unrelated untracked files may remain.

- [ ] **Step 4: Commit verification fixes if needed**

If Step 1 or Step 2 required code/test fixes, commit only touched DTP files:

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp
git commit -m "test: 验证 DTP 插件优化"
```

Expected: no extra commit if verification already passed.

---

## Self-Review

- Spec coverage: Task 1 covers resource and config validation; Task 2 covers safe lifecycle; Task 3 covers auto-config, bean backoff, controller data source reuse, and registry diagnostics; Task 4 covers script module behavior and task logs; Task 5 covers final verification.
- Placeholder scan: no TBD/TODO/fill-in placeholders are used; commands and file paths are exact.
- Type consistency: all referenced DTP classes exist locally; `DtpExecutor` constructor and setters are verified against DynamicTp 1.2.2 local jar.
