---
change: add-magicapi-repeat-submit-lock
design-doc: docs/superpowers/specs/2026-07-23-add-magicapi-repeat-submit-lock-design.md
base-ref: c9887910fc99d4bc110394ca49437f215a4a1871
archived-with: 2026-07-23-add-magicapi-repeat-submit-lock
---

# Magic-API 接口重复提交锁 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Magic-API `api` 类型 HTTP 请求接入可配置的重复提交锁，在接口脚本执行前阻止同一 repeat key 的重复请求，并在请求结束后释放当前请求持有的锁。

**Architecture:** 在 `fw-plus-boot-autoconfigure` 新增 Magic-API `RequestInterceptor`，复用现有 `Options.AVOID_REPEAT_SUBMIT_*`、`ParamConfig` 和 `Lock` Bean。key 生成拆到独立 resolver，默认后缀拆到 provider 接口，拦截器只负责读取开关、获取锁、短路响应和释放锁。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Magic-API 2.2.2, MagicScript 1.9.0, JUnit 5, Mockito, Maven Wrapper.

## Global Constraints

- 使用 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 模块，不修改 Magic-API core。
- 保持 JDK 1.8 兼容，不使用 `List.of()`、`Map.of()`、`Optional.orElseThrow()` 等 Java 9+ API。
- 保持 Magic-API 2.2.2 依赖版本和 `org.ssssssss` 依赖坐标不变。
- 使用现有 `Lock` Bean：`Lock.lock(String key, long expire, long acquireTimeout)` 与 `Lock.release(Object lockInfo)`。
- 配置读取沿用 `ParamConfig.get(info, key)`；`magic.api.<path>.options.*` 配置覆盖接口 options。
- `avoid_repeat_submit_timeout` 按毫秒解释，并作为 expire 和 acquireTimeout 传给 `Lock`。
- 重复提交时返回 Magic-API 统一 `JsonBean` 失败响应，不抛业务外异常，不执行接口脚本。
- 锁信息只绑定到本次请求的 `MagicScriptContext` 内部 key，不使用静态 Map。
- 使用模块内 Maven Wrapper 验证：`cd fw-plus-boot && ./mvnw ...`。

## File Structure

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitKeySuffixProvider.java`
  - 定义业务可替换的 repeat key 后缀接口。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/DefaultRepeatSubmitKeySuffixProvider.java`
  - 在没有可用业务 Bean 时基于 principal、请求属性、session、IP 生成通用后缀。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitKeyResolver.java`
  - 读取 repeat key 配置，解析安全 SpEL 或字面值，拼接接口维度、主 key、后缀和摘要。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitRequestInterceptor.java`
  - Magic-API 请求拦截器；获取锁、短路重复请求、请求完成释放锁。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
  - 注册默认后缀 provider、key resolver、重复提交拦截器 Bean。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/enums/Options.java`
  - 修正 `AVOID_REPEAT_SUBMIT_TIMEOUT` 展示文案单位为毫秒。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/ParamConfig.java`
  - 修正文档注释，明确外部配置覆盖接口 option。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/ParamConfigTest.java`
  - 覆盖 options 与 `magic.api.<path>.options.*` 的读取顺序。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitKeyResolverTest.java`
  - 覆盖 SpEL、默认字面值兼容、fallback、不同 key 隔离、业务后缀和默认后缀。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitRequestInterceptorTest.java`
  - 覆盖开关、锁获取、重复短路、释放和异常释放。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfigurationRepeatSubmitTest.java`
  - 覆盖自动配置注册和 `LockModuleConfiguration` 兜底行为。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIControllerJdbcOptionsTest.java`
  - 补充 `optionList` 仍暴露重复提交配置项且键名不变。

## OpenSpec Task Mapping

- `1.1` -> Task 3
- `1.2` -> Task 1
- `1.3` -> Task 2
- `2.1`-`2.4` -> Task 3
- `3.1`、`3.5` -> Task 1
- `3.2`-`3.4` -> Task 2
- `4.1`、`4.2` -> Task 4
- `4.3` -> Task 1
- `5.1`-`5.3` -> Task 5

### Task 1: 配置读取和控制台 options 元数据

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/ParamConfig.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/enums/Options.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/ParamConfigTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIControllerJdbcOptionsTest.java`

**Interfaces:**
- Consumes: `ParamConfig.get(ApiInfo info, String key)` and `ParamConfig.isEnabled(ApiInfo info, String key)`.
- Produces: documented config precedence and verified `Options.AVOID_REPEAT_SUBMIT_*` keys for later tasks.

- [x] **Step 1: Write failing config precedence tests**

Add `ParamConfigTest`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Option;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ParamConfigTest {

	private GenericApplicationContext context;

	@AfterEach
	void closeContext() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void externalMagicApiPathOptionOverridesInterfaceOption() {
		context = new GenericApplicationContext();
		TestPropertyValues.of("magic.api./order/create.options.avoid_repeat_submit_enable=false").applyTo(context);
		context.refresh();
		new SpringUtils().setApplicationContext(context);

		ApiInfo info = api("/order/create", Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue(), "true");

		assertThat(ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue())).isEqualTo("false");
		assertThat(ParamConfig.isEnabled(info, Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue())).isFalse();
	}

	@Test
	void interfaceOptionIsUsedWhenExternalConfigIsMissing() {
		context = new GenericApplicationContext();
		context.refresh();
		new SpringUtils().setApplicationContext(context);

		ApiInfo info = api("order/create", Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue(), "2500");

		assertThat(ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue())).isEqualTo("2500");
		assertThat(ParamConfig.getPath(info)).isEqualTo("/order/create");
	}

	private ApiInfo api(String path, String optionName, String optionValue) {
		ApiInfo info = new ApiInfo();
		info.setPath(path);
		info.setOption(Collections.singletonList(new Option(optionName, optionValue)));
		return info;
	}
}
```

- [x] **Step 2: Write failing optionList metadata test**

Append to `MagicAPIControllerJdbcOptionsTest`:

```java
	@Test
	void optionListKeepsRepeatSubmitOptionsAndMillisecondTimeoutLabel() {
		MagicAPIController controller = new MagicAPIController(new MagicConfiguration(), mock(MagicJdbcClassOptionProvider.class));

		JsonBean<List<List<String>>> result = controller.options();

		assertThat(result.getData()).contains(
				Arrays.asList("avoid_repeat_submit_enable", "启动避免重复提交", "true"),
				Arrays.asList("avoid_repeat_submit_timeout", "避免重复提交超时时间(毫秒)", "1000"),
				Arrays.asList("avoid_repeat_submit_key", "避免重复提交的 Key（支持 SpEL 表达式）", "#{magicapi:repeat:submit}"),
				Arrays.asList("avoid_repeat_submit_key_subfix_spring_bean", "避免重复提交的 Key 后缀（Spring Bean 名称）", "springbean"),
				Arrays.asList("avoid_repeat_submit_msg", "避免重复提交提示信息", "重复提交，请稍后再试。"));
	}
```

Also add imports:

```java
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
```

- [x] **Step 3: Run tests to verify failure**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='ParamConfigTest,MagicAPIControllerJdbcOptionsTest' test
```

Expected: FAIL because `Options.AVOID_REPEAT_SUBMIT_TIMEOUT` still says `秒` and `ParamConfig` comment has not been corrected.

- [x] **Step 4: Update comments and timeout label**

Change `ParamConfig` comment to:

```java
	/** magic.api./{path}.options.{key} > API options */
```

Change `Options.AVOID_REPEAT_SUBMIT_TIMEOUT` name from:

```java
AVOID_REPEAT_SUBMIT_TIMEOUT("避免重复提交超时时间(秒)", "avoid_repeat_submit_timeout", "1000") ,
```

to:

```java
AVOID_REPEAT_SUBMIT_TIMEOUT("避免重复提交超时时间(毫秒)", "avoid_repeat_submit_timeout", "1000") ,
```

- [x] **Step 5: Run tests to verify pass**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='ParamConfigTest,MagicAPIControllerJdbcOptionsTest' test
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/ParamConfig.java \
  fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/enums/Options.java \
  fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/ParamConfigTest.java \
  fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIControllerJdbcOptionsTest.java
git commit -m "fw-plus-boot: 校验重复提交配置元数据"
```

### Task 2: Repeat key 后缀接口和 key resolver

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitKeySuffixProvider.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/DefaultRepeatSubmitKeySuffixProvider.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitKeyResolver.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitKeyResolverTest.java`

**Interfaces:**
- Consumes: `ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_KEY.getValue())`, `ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue())`.
- Produces: `String RepeatSubmitKeyResolver.resolve(ApiInfo info, RequestEntity requestEntity)`.

- [x] **Step 1: Write failing resolver tests**

Create `RepeatSubmitKeyResolverTest` with these test names and assertions:

```java
class RepeatSubmitKeyResolverTest {

	private final StaticApplicationContext applicationContext = new StaticApplicationContext();
	private final RepeatSubmitKeySuffixProvider defaultSuffixProvider = requestEntity -> "default-suffix";
	private final RepeatSubmitKeyResolver resolver = new RepeatSubmitKeyResolver(applicationContext, defaultSuffixProvider);

	@Test
	void defaultLiteralKeyDoesNotInvokeSpelAndBuildsStableKey() {
		RequestEntity entity = entity(api("api-1", "/orders", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY.getValue(), "#{magicapi:repeat:submit}"),
				params("orderNo", "A001"), Collections.singletonMap("channel", "web"), body("amount", 100));

		String first = resolver.resolve(entity.getApiInfo(), entity);
		String second = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(first).isEqualTo(second);
		assertThat(first).startsWith("magicapi:repeat:submit:");
		assertThat(first).contains("api-1");
		assertThat(first).contains("default-suffix");
	}

	@Test
	void spelExpressionCanUseHeadersParamsPathVariablesBodyAndApi() {
		RequestEntity entity = entity(api("api-2", "/pay/{id}", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY.getValue(),
				"#{#headers['X-Request-ID'] + ':' + #params['tenant'] + ':' + #pathVariables['id'] + ':' + #body['bizNo'] + ':' + #api.method}"),
				params("tenant", "t1"), Collections.singletonMap("X-Request-ID", "rid-1"), body("bizNo", "B001"));
		entity.pathVariables(Collections.singletonMap("id", "42"));

		String key = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(key).contains("rid-1:t1:42:B001:POST");
	}

	@Test
	void emptyKeyFallsBackToRequestFingerprint() {
		RequestEntity entity = entity(api("api-3", "/pay", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY.getValue(), ""),
				params("tenant", "t1"), Collections.emptyMap(), body("bizNo", "B001"));

		String key = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(key).startsWith("magicapi:repeat:submit:");
		assertThat(key).contains("api-3");
		assertThat(key).contains("sha256:");
	}

	@Test
	void differentRequestContextsGenerateDifferentKeys() {
		RequestEntity left = entity(api("api-4", "/pay", "POST", Options.AVOID_REPEAT_SUBMIT_KEY.getValue(), ""),
				params("tenant", "t1"), Collections.emptyMap(), body("bizNo", "B001"));
		RequestEntity right = entity(api("api-4", "/pay", "POST", Options.AVOID_REPEAT_SUBMIT_KEY.getValue(), ""),
				params("tenant", "t2"), Collections.emptyMap(), body("bizNo", "B002"));

		assertThat(resolver.resolve(left.getApiInfo(), left)).isNotEqualTo(resolver.resolve(right.getApiInfo(), right));
	}

	@Test
	void configuredSuffixBeanIsUsedWhenItImplementsProviderInterface() {
		applicationContext.registerBean("tenantSuffix", RepeatSubmitKeySuffixProvider.class, () -> requestEntity -> "tenant-9527");
		applicationContext.refresh();
		RequestEntity entity = entity(api("api-5", "/orders", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue(), "tenantSuffix"),
				Collections.emptyMap(), Collections.emptyMap(), null);

		String key = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(key).contains("tenant-9527");
	}

	@Test
	void missingOrWrongSuffixBeanFallsBackToDefaultProvider() {
		applicationContext.registerSingleton("wrongSuffix", String.class);
		applicationContext.refresh();
		RequestEntity entity = entity(api("api-6", "/orders", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue(), "wrongSuffix"),
				Collections.emptyMap(), Collections.emptyMap(), null);

		String key = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(key).contains("default-suffix");
	}
}
```

Add helper methods in the same test class:

```java
private ApiInfo api(String id, String path, String method, String optionName, String optionValue) {
	ApiInfo info = new ApiInfo();
	info.setId(id);
	info.setPath(path);
	info.setMethod(method);
	info.setOption(Collections.singletonList(new Option(optionName, optionValue)));
	return info;
}

private RequestEntity entity(ApiInfo info, Map<String, Object> params, Map<String, Object> headers, Object body) {
	return RequestEntity.create()
			.info(info)
			.parameters(params)
			.pathVariables(Collections.emptyMap())
			.setHeaders(headers)
			.setRequestBody(body);
}

private Map<String, Object> params(String key, Object value) {
	Map<String, Object> map = new LinkedHashMap<>();
	map.put(key, value);
	return map;
}

private Map<String, Object> body(String key, Object value) {
	Map<String, Object> map = new LinkedHashMap<>();
	map.put(key, value);
	return map;
}
```

- [x] **Step 2: Run resolver tests to verify failure**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=RepeatSubmitKeyResolverTest test
```

Expected: FAIL because provider and resolver classes do not exist.

- [x] **Step 3: Add suffix provider interface**

Create `RepeatSubmitKeySuffixProvider.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat;

import org.ssssssss.magicapi.core.context.RequestEntity;

public interface RepeatSubmitKeySuffixProvider {

	String getSuffix(RequestEntity requestEntity);
}
```

- [x] **Step 4: Add default suffix provider**

Create `DefaultRepeatSubmitKeySuffixProvider.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpSession;

import java.security.Principal;

public class DefaultRepeatSubmitKeySuffixProvider implements RepeatSubmitKeySuffixProvider {

	@Override
	public String getSuffix(RequestEntity requestEntity) {
		MagicHttpServletRequest request = requestEntity == null ? null : requestEntity.getRequest();
		if (request == null) {
			return "";
		}
		Principal principal = request.getUserPrincipal();
		if (principal != null && StringUtils.isNotBlank(principal.getName())) {
			return principal.getName();
		}
		String userId = attribute(request, "userId");
		if (StringUtils.isNotBlank(userId)) {
			return userId;
		}
		MagicHttpSession session = request.getSession();
		if (session != null && session.getSession() != null) {
			return String.valueOf(session.getSession().hashCode());
		}
		String ip = firstNonBlank(
				firstIp(request.getHeader("X-Forwarded-For")),
				request.getHeader("X-Real-IP"),
				request.getHeader("Proxy-Client-IP"),
				request.getHeader("WL-Proxy-Client-IP"),
				request.getRemoteAddr());
		return StringUtils.defaultString(ip);
	}

	private String attribute(MagicHttpServletRequest request, String name) {
		Object value = request.getAttribute(name);
		return value == null ? null : String.valueOf(value);
	}

	private String firstIp(String value) {
		if (StringUtils.isBlank(value)) {
			return value;
		}
		int index = value.indexOf(',');
		return index < 0 ? value.trim() : value.substring(0, index).trim();
	}

	private String firstNonBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (StringUtils.isNotBlank(value)) {
				return value;
			}
		}
		return null;
	}
}
```

- [x] **Step 5: Add resolver implementation**

Create `RepeatSubmitKeyResolver.java` with these public methods:

```java
public class RepeatSubmitKeyResolver {

	public RepeatSubmitKeyResolver(ApplicationContext applicationContext,
								   RepeatSubmitKeySuffixProvider defaultSuffixProvider) {
		this.applicationContext = applicationContext;
		this.defaultSuffixProvider = defaultSuffixProvider;
		this.expressionParser = new SpelExpressionParser();
	}

	public String resolve(ApiInfo info, RequestEntity requestEntity) {
		String main = resolveMainKey(info, requestEntity);
		String suffix = resolveSuffix(info, requestEntity);
		List<String> parts = new ArrayList<>();
		parts.add(PREFIX);
		parts.add(interfaceKey(info));
		parts.add(normalizeSegment(StringUtils.defaultIfBlank(main, fallbackFingerprint(info, requestEntity))));
		if (StringUtils.isNotBlank(suffix)) {
			parts.add(normalizeSegment(suffix));
		}
		return StringUtils.join(parts, ":");
	}
}
```

Use these private rules:

```java
private static final String PREFIX = "magicapi:repeat:submit";
private static final int MAX_SEGMENT_LENGTH = 128;

private String resolveMainKey(ApiInfo info, RequestEntity requestEntity) {
	String configured = ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_KEY.getValue());
	if (StringUtils.isBlank(configured)) {
		return null;
	}
	String trimmed = configured.trim();
	if (isSpel(trimmed)) {
		return String.valueOf(evaluateSpel(trimmed, info, requestEntity));
	}
	return trimmed;
}

private boolean isSpel(String value) {
	return value.startsWith("#{#") && value.endsWith("}");
}
```

For SpEL, use `StandardEvaluationContext` and set only these variables: `request`, `response`, `headers`, `params`, `pathVariables`, `body`, `api`, `requestEntity`. Do not configure a BeanResolver.

For stable serialization and digest, use Java 8 APIs:

```java
private String digest(Object value) {
	try {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] bytes = JSON.toJSONString(value, JSONWriter.Feature.MapSortField).getBytes(StandardCharsets.UTF_8);
		byte[] encoded = digest.digest(bytes);
		StringBuilder builder = new StringBuilder();
		for (byte b : encoded) {
			builder.append(String.format("%02x", b));
		}
		return "sha256:" + builder.toString();
	} catch (NoSuchAlgorithmException e) {
		throw new IllegalStateException("SHA-256 algorithm is not available", e);
	}
}
```

For invalid SpEL, throw `IllegalArgumentException("重复提交 Key SpEL 解析失败: " + configured, e)` so Task 3 can convert it to Magic-API failure response.

- [x] **Step 6: Run resolver tests to verify pass**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=RepeatSubmitKeyResolverTest test
```

Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat \
  fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitKeyResolverTest.java
git commit -m "fw-plus-boot: 添加 MagicAPI 重复提交 key 解析"
```

### Task 3: RepeatSubmitRequestInterceptor 锁生命周期

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitRequestInterceptor.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitRequestInterceptorTest.java`

**Interfaces:**
- Consumes: `RepeatSubmitKeyResolver.resolve(ApiInfo, RequestEntity)`, `Lock.lock(String,long,long)`, `Lock.release(Object)`, `ResultProvider.buildResult(RequestEntity,int,String,Object)`.
- Produces: Magic-API `RequestInterceptor` implementation registered in Task 4.

- [x] **Step 1: Write failing interceptor tests**

Create `RepeatSubmitRequestInterceptorTest` with an in-memory lock stub:

```java
static class StubLock implements Lock {
	private final Set<String> locked = new HashSet<>();
	private int lockCalls;
	private int releaseCalls;

	@Override
	public Object lock(String key) {
		return lock(key, 0, 0);
	}

	@Override
	public Object lock(String key, long expire, long acquireTimeout) {
		lockCalls++;
		if (!locked.add(key)) {
			throw new RuntimeException("获取本地锁超时");
		}
		return key;
	}

	@Override
	public boolean release(Object lockInfo) {
		releaseCalls++;
		return locked.remove(String.valueOf(lockInfo));
	}
}
```

Cover these test cases:

```java
@Test
void disabledOptionDoesNotCallLockAndAllowsScript() throws Exception

@Test
void firstRequestObtainsLockAndStoresLockInfoInMagicScriptContext() throws Exception

@Test
void duplicateRequestReturnsFailureJsonBeanAndDoesNotReleaseOtherLock() throws Exception

@Test
void differentKeysDoNotBlockEachOther() throws Exception

@Test
void successfulScriptReleasesCurrentRequestLock()

@Test
void failedScriptReleasesCurrentRequestLock()

@Test
void releaseFailureDoesNotThrow()
```

Build request entities with:

```java
private RequestEntity entity(String key, String enabled) {
	ApiInfo info = new ApiInfo();
	info.setId("api-" + key);
	info.setPath("/submit");
	info.setMethod("POST");
	info.setOption(Arrays.asList(
			new Option(Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue(), enabled),
			new Option(Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue(), "1000"),
			new Option(Options.AVOID_REPEAT_SUBMIT_MSG.getValue(), "重复提交，请稍后再试。")));
	return RequestEntity.create()
			.info(info)
			.parameters(Collections.singletonMap("key", key))
			.pathVariables(Collections.emptyMap())
			.setHeaders(Collections.emptyMap())
			.setMagicScriptContext(new MagicScriptContext());
}
```

Use a resolver stub:

```java
RepeatSubmitKeyResolver resolver = mock(RepeatSubmitKeyResolver.class);
when(resolver.resolve(any(ApiInfo.class), any(RequestEntity.class))).thenAnswer(invocation -> {
	RequestEntity entity = invocation.getArgument(1);
	return "magicapi:repeat:submit:" + entity.getParameters().get("key");
});
```

Assert duplicate response:

```java
Object duplicate = interceptor.preHandle(second);
assertThat(duplicate).isInstanceOf(JsonBean.class);
JsonBean<?> jsonBean = (JsonBean<?>) duplicate;
assertThat(jsonBean.getMessage()).isEqualTo("重复提交，请稍后再试。");
assertThat(jsonBean.isSuccess()).isFalse();
assertThat(lock.releaseCalls).isZero();
```

- [x] **Step 2: Run interceptor tests to verify failure**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=RepeatSubmitRequestInterceptorTest test
```

Expected: FAIL because `RepeatSubmitRequestInterceptor` does not exist.

- [x] **Step 3: Implement interceptor**

Create `RepeatSubmitRequestInterceptor.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.ParamConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.script.MagicScriptContext;

@Slf4j
public class RepeatSubmitRequestInterceptor implements RequestInterceptor {

	static final String LOCK_CONTEXT_KEY = "__fw_plus_repeat_submit_lock_info";

	private final Lock lock;
	private final ResultProvider resultProvider;
	private final RepeatSubmitKeyResolver keyResolver;

	public RepeatSubmitRequestInterceptor(Lock lock, ResultProvider resultProvider, RepeatSubmitKeyResolver keyResolver) {
		this.lock = lock;
		this.resultProvider = resultProvider;
		this.keyResolver = keyResolver;
	}

	@Override
	public Object preHandle(RequestEntity requestEntity) {
		if (!ParamConfig.isEnabled(requestEntity.getApiInfo(), Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue())) {
			return null;
		}
		long timeout = parseTimeout(requestEntity);
		String key;
		try {
			key = keyResolver.resolve(requestEntity.getApiInfo(), requestEntity);
		} catch (Exception e) {
			return failure(requestEntity, e.getMessage());
		}
		try {
			Object lockInfo = lock.lock(key, timeout, timeout);
			requestEntity.getMagicScriptContext().set(LOCK_CONTEXT_KEY, lockInfo);
			return null;
		} catch (Exception e) {
			return failure(requestEntity, repeatMessage(requestEntity));
		}
	}

	@Override
	public void afterCompletion(RequestEntity requestEntity, Object returnValue, Throwable throwable) {
		MagicScriptContext context = requestEntity.getMagicScriptContext();
		Object lockInfo = context == null ? null : context.get(LOCK_CONTEXT_KEY);
		if (lockInfo == null) {
			return;
		}
		try {
			lock.release(lockInfo);
		} catch (Exception e) {
			log.warn("release magic-api repeat submit lock failed", e);
		}
	}

	private long parseTimeout(RequestEntity requestEntity) {
		String value = ParamConfig.get(requestEntity.getApiInfo(), Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue());
		try {
			return Long.parseLong(StringUtils.defaultIfBlank(value, Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getDefaultValue()));
		} catch (Exception e) {
			log.warn("invalid avoid_repeat_submit_timeout={}, use default={}", value, Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getDefaultValue());
			return Long.parseLong(Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getDefaultValue());
		}
	}

	private String repeatMessage(RequestEntity requestEntity) {
		return StringUtils.defaultIfBlank(
				ParamConfig.get(requestEntity.getApiInfo(), Options.AVOID_REPEAT_SUBMIT_MSG.getValue()),
				Options.AVOID_REPEAT_SUBMIT_MSG.getDefaultValue());
	}

	private Object failure(RequestEntity requestEntity, String message) {
		Object result = resultProvider.buildResult(requestEntity, Constants.RESPONSE_CODE_INVALID, message, null);
		if (result instanceof JsonBean) {
			((JsonBean<?>) result).setSuccess(false);
		}
		return result;
	}
}
```

- [x] **Step 4: Run interceptor tests to verify pass**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=RepeatSubmitRequestInterceptorTest test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitRequestInterceptor.java \
  fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitRequestInterceptorTest.java
git commit -m "fw-plus-boot: 添加 MagicAPI 重复提交拦截器"
```

### Task 4: 自动配置接入

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfigurationRepeatSubmitTest.java`

**Interfaces:**
- Consumes: Task 2 and Task 3 classes.
- Produces: Spring Beans for `DefaultRepeatSubmitKeySuffixProvider`, `RepeatSubmitKeyResolver`, `RepeatSubmitRequestInterceptor`.

- [x] **Step 1: Write failing auto-configuration tests**

Create `MagicAPIBaseAutoConfigurationRepeatSubmitTest`:

```java
class MagicAPIBaseAutoConfigurationRepeatSubmitTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MagicAPIBaseAutoConfiguration.class, LockModuleConfiguration.class)
			.withBean(MagicAPIProperties.class, MagicAPIProperties::new)
			.withBean(MagicConfiguration.class, MagicConfiguration::new);

	@Test
	void repeatSubmitBeansAreRegisteredWhenMagicApiIsEnabled() {
		contextRunner.withPropertyValues("fw.plus.magicapi.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(DefaultRepeatSubmitKeySuffixProvider.class);
					assertThat(context).hasSingleBean(RepeatSubmitKeyResolver.class);
					assertThat(context).hasSingleBean(RepeatSubmitRequestInterceptor.class);
				});
	}

	@Test
	void localLockModuleIsUsedWhenLockTemplateIsMissing() {
		new ApplicationContextRunner()
				.withUserConfiguration(LockModuleConfiguration.class)
				.run(context -> {
					assertThat(context).hasSingleBean(LocalLockModule.class);
					assertThat(context).hasSingleBean(Lock.class);
				});
	}
}
```

- [x] **Step 2: Run auto-configuration tests to verify failure**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIBaseAutoConfigurationRepeatSubmitTest test
```

Expected: FAIL because repeat submit Beans are not registered.

- [x] **Step 3: Register repeat submit Beans**

Modify `MagicAPIBaseAutoConfiguration` imports:

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.DefaultRepeatSubmitKeySuffixProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.RepeatSubmitKeyResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.RepeatSubmitKeySuffixProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.RepeatSubmitRequestInterceptor;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;
```

Add Bean methods near existing provider Beans:

```java
	@Bean
	@ConditionalOnMissingBean(RepeatSubmitKeySuffixProvider.class)
	public DefaultRepeatSubmitKeySuffixProvider repeatSubmitKeySuffixProvider() {
		return new DefaultRepeatSubmitKeySuffixProvider();
	}

	@Bean
	public RepeatSubmitKeyResolver repeatSubmitKeyResolver(RepeatSubmitKeySuffixProvider repeatSubmitKeySuffixProvider) {
		return new RepeatSubmitKeyResolver(applicationContext, repeatSubmitKeySuffixProvider);
	}

	@Bean
	public RepeatSubmitRequestInterceptor repeatSubmitRequestInterceptor(Lock lock,
																		 ResultProvider resultProvider,
																		 RepeatSubmitKeyResolver repeatSubmitKeyResolver) {
		return new RepeatSubmitRequestInterceptor(lock, resultProvider, repeatSubmitKeyResolver);
	}
```

Use fully qualified name for local `ResultProvider` if import conflicts with `com.xiongx9527...provider.ResultProvider`.

- [x] **Step 4: Run auto-configuration tests to verify pass**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIBaseAutoConfigurationRepeatSubmitTest test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java \
  fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfigurationRepeatSubmitTest.java
git commit -m "fw-plus-boot: 接入 MagicAPI 重复提交自动配置"
```

### Task 5: 集成验证和 OpenSpec 覆盖核对

**Files:**
- Verify only: all files changed by Tasks 1-4.

**Interfaces:**
- Consumes: completed Tasks 1-4.
- Produces: verification evidence for OpenSpec `5.1`-`5.3`.

- [x] **Step 1: Run focused repeat-submit test suite**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='ParamConfigTest,MagicAPIControllerJdbcOptionsTest,RepeatSubmitKeyResolverTest,RepeatSubmitRequestInterceptorTest,MagicAPIBaseAutoConfigurationRepeatSubmitTest' test
```

Expected: PASS; confirms config precedence, key generation, duplicate short-circuit, different-key isolation, release behavior, optionList, and auto-configuration.

- [x] **Step 2: Compile target module with tests skipped**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests clean install
```

Expected: BUILD SUCCESS; confirms Java 8 compatibility and Magic-API 2.2.2 compile compatibility.

- [x] **Step 3: Verify no Java 9+ APIs were introduced**

Run:

```bash
rg 'List\\.of\\(|Map\\.of\\(|Set\\.of\\(|Optional\\.orElseThrow\\(|Map\\.copyOf\\(' fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi
```

Expected: no output.

- [x] **Step 4: Check OpenSpec coverage manually**

Verify each requirement in `openspec/changes/add-magicapi-repeat-submit-lock/specs/magicapi-repeat-submit-lock/spec.md` has test evidence:

```text
Repeat submit validation disabled -> RepeatSubmitRequestInterceptorTest.disabledOptionDoesNotCallLockAndAllowsScript
Repeat submit validation enabled -> RepeatSubmitRequestInterceptorTest.firstRequestObtainsLockAndStoresLockInfoInMagicScriptContext
External config overrides option -> ParamConfigTest.externalMagicApiPathOptionOverridesInterfaceOption
SpEL uses request data -> RepeatSubmitKeyResolverTest.spelExpressionCanUseHeadersParamsPathVariablesBodyAndApi
Different keys do not block -> RepeatSubmitRequestInterceptorTest.differentKeysDoNotBlockEachOther
Empty key fallback -> RepeatSubmitKeyResolverTest.emptyKeyFallsBackToRequestFingerprint
Configured suffix bean -> RepeatSubmitKeyResolverTest.configuredSuffixBeanIsUsedWhenItImplementsProviderInterface
Default suffix provider fallback -> RepeatSubmitKeyResolverTest.missingOrWrongSuffixBeanFallsBackToDefaultProvider
Duplicate request fails -> RepeatSubmitRequestInterceptorTest.duplicateRequestReturnsFailureJsonBeanAndDoesNotReleaseOtherLock
Acquire timeout honored -> RepeatSubmitRequestInterceptorTest first lock call asserts expire/acquireTimeout = 1000
Successful script releases -> RepeatSubmitRequestInterceptorTest.successfulScriptReleasesCurrentRequestLock
Failed script releases -> RepeatSubmitRequestInterceptorTest.failedScriptReleasesCurrentRequestLock
Duplicate does not release other lock -> RepeatSubmitRequestInterceptorTest.duplicateRequestReturnsFailureJsonBeanAndDoesNotReleaseOtherLock
```

- [x] **Step 5: Commit verification-only cleanup if needed**

If Step 1-4 reveal only minor test/import cleanup, commit after fixes:

```bash
git add fw-plus-boot-project/fw-plus-boot-autoconfigure
git commit -m "fw-plus-boot: 完成重复提交锁验证"
```

If no files changed during verification, do not create an empty commit.
