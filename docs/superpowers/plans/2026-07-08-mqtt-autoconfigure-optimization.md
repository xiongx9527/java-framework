# MQTT 自动配置优化实施计划

> **给代理执行者：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务执行本计划。步骤使用复选框（`- [ ]`）跟踪状态。

**目标：** 标准化并严格优化 fw-plus MQTT 自动配置，补齐客户端配置、默认安全行为、单机/Redis broker 分支和回归测试。

**架构：** 保留 `com.xiongx9527.fw.plus.boot.autoconfigure.mqtt` 作为 fw-plus 增强层，不重复实现 Mica MQTT 已有能力。server 侧保留内存/Redis broker 扩展并补条件、覆盖点和安全默认值；client 侧显式导入 Mica client 标准配置以补齐空壳自动配置。

**技术栈：** Java 8、Spring Boot 2.7.18、Maven Wrapper、Mica MQTT 2.3.9、JUnit 5、AssertJ、Spring Boot `ApplicationContextRunner`。

## 全局约束

- 使用简体中文沟通和提交说明。
- 目标源码保持 Java 1.8 兼容，不使用 Java 9+ API。
- 不引入新的外部依赖版本。
- 不升级 Spring Boot、Mica MQTT、Redis 或 Magic-API 版本。
- 范围限定在 MQTT 自动配置包及其直接协作类。
- 不改 MagicAPI MQTT 插件的资源格式和脚本 API。
- 先写失败测试，再写生产代码。
- 使用各模块内置 Maven Wrapper：`cd fw-plus-boot && ./mvnw ...`。

---

## 文件结构

- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/client/MqttClientAutoConfiguration.java`
  - 作为 fw-plus client 自动配置入口，导入 Mica MQTT client 标准配置。
- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/MqttServerConfiguration.java`
  - 创建 server 默认组件、creator、server、launcher、template、单机内存实现；补缺省条件和安全配置注入。
- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/MqttServerBrokerConfiguration.java`
  - Redis broker 入口，增加 Redis 连接 bean 条件。
- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/config/MqttBrokerConfiguration.java`
  - Redis broker 默认 bean，补 `@ConditionalOnMissingBean`。
- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttAuthHandler.java`
  - 默认 MQTT 认证实现，按 Mica `MqttServerProperties.MqttAuth` 配置认证。
- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttPublishPermission.java`
  - 默认发布权限实现，拒绝明显非法发布请求。
- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttSubscribeValidator.java`
  - 默认订阅校验实现，拒绝明显非法订阅过滤器。
- 新建：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/properties/FwPlusMqttServerSecurityProperties.java`
  - fw-plus 轻量安全开关：发布/订阅授权开关、认证关闭时是否允许匿名。
- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/cluster/RedisMqttMessageDispatcher.java`
  - 初始化后校验 `RStreamTemplate` 存在，否则抛出清晰异常。
- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/cluster/RedisMqttMessageDownReceiver.java`
  - 初始化后校验 `RedisConnectionFactory` 存在，否则抛出清晰异常。
- 修改：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/cluster/RedisMqttMessageExchangeReceiver.java`
  - 与 down receiver 一致的初始化校验。
- 测试：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttSecurityDefaultsTest.java`
  - 覆盖默认认证、发布权限、订阅校验行为。
- 测试：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/MqttAutoConfigurationTest.java`
  - 覆盖 server/client 自动配置启停、用户 bean 覆盖、单机内存分支、client 标准配置导入。
- 测试：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/MqttBrokerConfigurationTest.java`
  - 覆盖 Redis broker 条件和可覆盖 bean。

---

### Task 1: 默认安全行为

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/properties/FwPlusMqttServerSecurityProperties.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttAuthHandler.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttPublishPermission.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttSubscribeValidator.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttSecurityDefaultsTest.java`

**Interfaces:**
- Produces: `FwPlusMqttServerSecurityProperties` with getters/setters:
  - `boolean publishAuthEnabled`
  - `boolean subscribeAuthEnabled`
  - `boolean allowAnonymousWhenAuthDisabled`
- Produces: `new MqttAuthHandler(MqttServerProperties, FwPlusMqttServerSecurityProperties)`
- Produces: `new MqttPublishPermission(FwPlusMqttServerSecurityProperties)`
- Produces: `new MqttSubscribeValidator(FwPlusMqttServerSecurityProperties)`

- [ ] **Step 1: Write the failing security tests**

Create `MqttSecurityDefaultsTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties.FwPlusMqttServerSecurityProperties;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.spring.server.config.MqttServerProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttSecurityDefaultsTest {

	@Test
	void authRejectsBlankClientId() {
		MqttAuthHandler handler = new MqttAuthHandler(new MqttServerProperties(),
				new FwPlusMqttServerSecurityProperties());

		boolean authenticated = handler.authenticate(null, null, "", null, null);

		assertThat(authenticated).isFalse();
	}

	@Test
	void authAllowsAnonymousOnlyWhenMicaAuthDisabledAndAnonymousAllowed() {
		MqttServerProperties properties = new MqttServerProperties();
		properties.getAuth().setEnable(false);
		FwPlusMqttServerSecurityProperties security = new FwPlusMqttServerSecurityProperties();
		security.setAllowAnonymousWhenAuthDisabled(true);
		MqttAuthHandler handler = new MqttAuthHandler(properties, security);

		boolean authenticated = handler.authenticate(null, null, "client-a", null, null);

		assertThat(authenticated).isTrue();
	}

	@Test
	void authRejectsWhenMicaAuthEnabledAndCredentialsDoNotMatch() {
		MqttServerProperties properties = new MqttServerProperties();
		properties.getAuth().setEnable(true);
		properties.getAuth().setUsername("user");
		properties.getAuth().setPassword("secret");
		MqttAuthHandler handler = new MqttAuthHandler(properties, new FwPlusMqttServerSecurityProperties());

		boolean authenticated = handler.authenticate(null, null, "client-a", "user", "bad");

		assertThat(authenticated).isFalse();
	}

	@Test
	void authAllowsWhenMicaAuthEnabledAndCredentialsMatch() {
		MqttServerProperties properties = new MqttServerProperties();
		properties.getAuth().setEnable(true);
		properties.getAuth().setUsername("user");
		properties.getAuth().setPassword("secret");
		MqttAuthHandler handler = new MqttAuthHandler(properties, new FwPlusMqttServerSecurityProperties());

		boolean authenticated = handler.authenticate(null, null, "client-a", "user", "secret");

		assertThat(authenticated).isTrue();
	}

	@Test
	void publishPermissionRejectsBlankClientIdBlankTopicAndWildcardTopic() {
		MqttPublishPermission permission = new MqttPublishPermission(new FwPlusMqttServerSecurityProperties());

		assertThat(permission.hasPermission(null, "", "sensor/temp", MqttQoS.QOS0, false)).isFalse();
		assertThat(permission.hasPermission(null, "client-a", "", MqttQoS.QOS0, false)).isFalse();
		assertThat(permission.hasPermission(null, "client-a", "sensor/+", MqttQoS.QOS0, false)).isFalse();
		assertThat(permission.hasPermission(null, "client-a", "sensor/#", MqttQoS.QOS0, false)).isFalse();
	}

	@Test
	void publishPermissionCanBeDisabled() {
		FwPlusMqttServerSecurityProperties security = new FwPlusMqttServerSecurityProperties();
		security.setPublishAuthEnabled(false);
		MqttPublishPermission permission = new MqttPublishPermission(security);

		assertThat(permission.hasPermission(null, "", "", MqttQoS.QOS0, false)).isTrue();
	}

	@Test
	void subscribeValidatorRejectsBlankClientIdBlankFilterAndInvalidWildcard() {
		MqttSubscribeValidator validator = new MqttSubscribeValidator(new FwPlusMqttServerSecurityProperties());

		assertThat(validator.isValid(null, "", "sensor/temp", MqttQoS.QOS0)).isFalse();
		assertThat(validator.isValid(null, "client-a", "", MqttQoS.QOS0)).isFalse();
		assertThat(validator.isValid(null, "client-a", "sensor/#/bad", MqttQoS.QOS0)).isFalse();
		assertThat(validator.isValid(null, "client-a", "sensor/te+mp", MqttQoS.QOS0)).isFalse();
		assertThat(validator.isValid(null, "client-a", "sensor/+", MqttQoS.QOS0)).isTrue();
	}

	@Test
	void subscribeValidatorCanBeDisabled() {
		FwPlusMqttServerSecurityProperties security = new FwPlusMqttServerSecurityProperties();
		security.setSubscribeAuthEnabled(false);
		MqttSubscribeValidator validator = new MqttSubscribeValidator(security);

		assertThat(validator.isValid(null, "", "", MqttQoS.QOS0)).isTrue();
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttSecurityDefaultsTest test
```

Expected: FAIL because `FwPlusMqttServerSecurityProperties` and constructor overloads do not exist.

- [ ] **Step 3: Implement the security properties**

Create `FwPlusMqttServerSecurityProperties.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fw-plus.mqtt.server.security")
public class FwPlusMqttServerSecurityProperties {

	private boolean publishAuthEnabled = true;

	private boolean subscribeAuthEnabled = true;

	private boolean allowAnonymousWhenAuthDisabled = true;
}
```

- [ ] **Step 4: Implement default auth handler**

Replace `MqttAuthHandler` body with:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties.FwPlusMqttServerSecurityProperties;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerAuthHandler;
import net.dreamlu.iot.mqtt.spring.server.config.MqttServerProperties;
import org.apache.commons.lang3.StringUtils;
import org.tio.core.ChannelContext;

import java.util.Objects;

public class MqttAuthHandler implements IMqttServerAuthHandler {

	private final MqttServerProperties properties;

	private final FwPlusMqttServerSecurityProperties securityProperties;

	public MqttAuthHandler() {
		this(new MqttServerProperties(), new FwPlusMqttServerSecurityProperties());
	}

	public MqttAuthHandler(MqttServerProperties properties, FwPlusMqttServerSecurityProperties securityProperties) {
		this.properties = Objects.requireNonNull(properties, "MqttServerProperties must not be null");
		this.securityProperties = Objects.requireNonNull(securityProperties, "FwPlusMqttServerSecurityProperties must not be null");
	}

	@Override
	public boolean authenticate(ChannelContext context, String uniqueId, String clientId, String userName, String password) {
		if (StringUtils.isBlank(clientId)) {
			return false;
		}
		MqttServerProperties.MqttAuth mqttAuth = this.properties.getAuth();
		if (mqttAuth == null || !mqttAuth.isEnable()) {
			return this.securityProperties.isAllowAnonymousWhenAuthDisabled();
		}
		return StringUtils.equals(mqttAuth.getUsername(), userName)
				&& StringUtils.equals(mqttAuth.getPassword(), password);
	}
}
```

- [ ] **Step 5: Implement default publish permission**

Replace `MqttPublishPermission` body with:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties.FwPlusMqttServerSecurityProperties;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerPublishPermission;
import org.apache.commons.lang3.StringUtils;
import org.tio.core.ChannelContext;

import java.util.Objects;

public class MqttPublishPermission implements IMqttServerPublishPermission {

	private final FwPlusMqttServerSecurityProperties securityProperties;

	public MqttPublishPermission() {
		this(new FwPlusMqttServerSecurityProperties());
	}

	public MqttPublishPermission(FwPlusMqttServerSecurityProperties securityProperties) {
		this.securityProperties = Objects.requireNonNull(securityProperties, "FwPlusMqttServerSecurityProperties must not be null");
	}

	@Override
	public boolean hasPermission(ChannelContext context, String clientId, String topic, MqttQoS qoS, boolean isRetain) {
		if (!this.securityProperties.isPublishAuthEnabled()) {
			return true;
		}
		return StringUtils.isNotBlank(clientId)
				&& StringUtils.isNotBlank(topic)
				&& !StringUtils.contains(topic, '+')
				&& !StringUtils.contains(topic, '#');
	}
}
```

- [ ] **Step 6: Implement default subscribe validator**

Replace `MqttSubscribeValidator` body with:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties.FwPlusMqttServerSecurityProperties;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import org.apache.commons.lang3.StringUtils;
import org.tio.core.ChannelContext;

import java.util.Objects;

public class MqttSubscribeValidator implements IMqttServerSubscribeValidator {

	private final FwPlusMqttServerSecurityProperties securityProperties;

	public MqttSubscribeValidator() {
		this(new FwPlusMqttServerSecurityProperties());
	}

	public MqttSubscribeValidator(FwPlusMqttServerSecurityProperties securityProperties) {
		this.securityProperties = Objects.requireNonNull(securityProperties, "FwPlusMqttServerSecurityProperties must not be null");
	}

	@Override
	public boolean isValid(ChannelContext context, String clientId, String topicFilter, MqttQoS qoS) {
		if (!this.securityProperties.isSubscribeAuthEnabled()) {
			return true;
		}
		return StringUtils.isNotBlank(clientId)
				&& StringUtils.isNotBlank(topicFilter)
				&& isValidTopicFilter(topicFilter);
	}

	private boolean isValidTopicFilter(String topicFilter) {
		String[] levels = topicFilter.split("/", -1);
		for (int i = 0; i < levels.length; i++) {
			String level = levels[i];
			if (StringUtils.contains(level, '#')) {
				if (!"#".equals(level) || i != levels.length - 1) {
					return false;
				}
			}
			if (StringUtils.contains(level, '+') && !"+".equals(level)) {
				return false;
			}
		}
		return true;
	}
}
```

- [ ] **Step 7: Run test to verify it passes**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttSecurityDefaultsTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttAuthHandler.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttPublishPermission.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttSubscribeValidator.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/properties/FwPlusMqttServerSecurityProperties.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/auth/MqttSecurityDefaultsTest.java
git commit -m "feat: harden mqtt default security"
```

---

### Task 2: 服务端自动配置条件和可覆盖 bean

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/MqttServerConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/MqttAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `FwPlusMqttServerSecurityProperties`
- Produces: default beans using `@ConditionalOnMissingBean` and new constructors from Task 1.

- [ ] **Step 1: Write failing server auto-configuration tests**

Create `MqttAutoConfigurationTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth.MqttAuthHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth.MqttPublishPermission;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth.MqttSubscribeValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties.FwPlusMqttServerSecurityProperties;
import net.dreamlu.iot.mqtt.core.server.MqttServerCreator;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerAuthHandler;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerPublishPermission;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import net.dreamlu.iot.mqtt.core.server.session.IMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.store.IMqttMessageStore;
import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import net.dreamlu.iot.mqtt.spring.client.config.MqttClientConfiguration;
import net.dreamlu.iot.mqtt.spring.server.MqttServerTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MqttAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MqttAutoConfiguration.class));

	@Test
	void serverBacksOffWhenDisabled() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=false", "mqtt.client.enabled=false")
				.run(context -> assertThat(context)
						.doesNotHaveBean(MqttServerCreator.class)
						.doesNotHaveBean(MqttServerTemplate.class)
						.doesNotHaveBean(MqttAuthHandler.class));
	}

	@Test
	void serverCreatesDefaultSecurityAndMemoryBeansWhenEnabled() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=true", "mqtt.client.enabled=false")
				.run(context -> assertThat(context)
						.hasSingleBean(FwPlusMqttServerSecurityProperties.class)
						.hasSingleBean(IMqttServerAuthHandler.class)
						.hasSingleBean(IMqttServerPublishPermission.class)
						.hasSingleBean(IMqttServerSubscribeValidator.class)
						.hasSingleBean(IMqttSessionManager.class)
						.hasSingleBean(IMqttMessageStore.class)
						.hasSingleBean(MqttServerCreator.class)
						.hasSingleBean(MqttServerTemplate.class));
	}

	@Test
	void serverBacksOffForUserSecurityBeans() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=true", "mqtt.client.enabled=false")
				.withUserConfiguration(UserSecurityConfiguration.class)
				.run(context -> {
					assertThat(context).hasSingleBean(IMqttServerAuthHandler.class);
					assertThat(context).hasSingleBean(IMqttServerPublishPermission.class);
					assertThat(context).hasSingleBean(IMqttServerSubscribeValidator.class);
					assertThat(context).doesNotHaveBean(MqttAuthHandler.class);
					assertThat(context).doesNotHaveBean(MqttPublishPermission.class);
					assertThat(context).doesNotHaveBean(MqttSubscribeValidator.class);
				});
	}

	@Test
	void clientImportsMicaClientConfigurationWhenEnabled() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=false", "mqtt.client.enabled=true")
				.run(context -> assertThat(context)
						.hasSingleBean(MqttClientConfiguration.class)
						.hasSingleBean(MqttClientTemplate.class));
	}

	@Test
	void clientBacksOffWhenDisabled() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=false", "mqtt.client.enabled=false")
				.run(context -> assertThat(context)
						.doesNotHaveBean(MqttClientConfiguration.class)
						.doesNotHaveBean(MqttClientTemplate.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class UserSecurityConfiguration {

		@Bean
		IMqttServerAuthHandler customAuthHandler() {
			return mock(IMqttServerAuthHandler.class);
		}

		@Bean
		IMqttServerPublishPermission customPublishPermission() {
			return mock(IMqttServerPublishPermission.class);
		}

		@Bean
		IMqttServerSubscribeValidator customSubscribeValidator() {
			return mock(IMqttServerSubscribeValidator.class);
		}
	}
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttAutoConfigurationTest test
```

Expected: FAIL because server configuration does not enable new security properties and client config is not imported.

- [ ] **Step 3: Update server configuration properties and default beans**

Modify `MqttServerConfiguration`:

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({MqttServerProperties.class, FwPlusMqttServerSecurityProperties.class})
public class MqttServerConfiguration {
```

Change default bean methods:

```java
@Bean
@ConditionalOnMissingBean
public IMqttServerSubscribeValidator mqttSubscribeValidator(FwPlusMqttServerSecurityProperties securityProperties) {
	return new MqttSubscribeValidator(securityProperties);
}

@Bean
@ConditionalOnMissingBean
public IMqttServerPublishPermission mqttPublishPermission(FwPlusMqttServerSecurityProperties securityProperties) {
	return new MqttPublishPermission(securityProperties);
}

@Bean
@ConditionalOnMissingBean
public IMqttServerAuthHandler mqttServerAuthHandler(MqttServerProperties properties,
		FwPlusMqttServerSecurityProperties securityProperties) {
	return new MqttAuthHandler(properties, securityProperties);
}
```

Add `@ConditionalOnMissingBean` to:

```java
@Bean
@ConditionalOnMissingBean
public MqttServerCreator mqttServerCreator(...)

@Bean
@ConditionalOnMissingBean
public MqttServer mqttServer(MqttServerCreator mqttServerCreator)

@Bean
@ConditionalOnMissingBean
public MqttServerLauncher mqttServerLauncher(MqttServer mqttServer)

@Bean
@ConditionalOnMissingBean
public MqttServerTemplate mqttServerTemplate(MqttServer mqttServer)

@Bean
@ConditionalOnMissingBean
@ConditionalOnMissingClass(value = {"net.dreamlu.mica.redis.cache.MicaRedisCache"})
public IMqttSessionManager mqttSessionManager()

@Bean
@ConditionalOnMissingBean
@ConditionalOnMissingClass(value = {"net.dreamlu.mica.redis.cache.MicaRedisCache"})
public IMqttMessageStore mqttMessageStore()

@Bean
@ConditionalOnMissingBean
@ConditionalOnMissingClass(value = {"net.dreamlu.mica.redis.cache.MicaRedisCache"})
public IMqttService mqttService(MqttServer mqttServer)
```

Remove the fallback `new DefaultMqttServerAuthHandler(...)` from `mqttServerCreator`; it should only use the already-created `IMqttServerAuthHandler` bean:

```java
authHandlerObjectProvider.ifAvailable(serverCreator::authHandler);
```

- [ ] **Step 4: Import Mica client configuration**

Modify `MqttClientAutoConfiguration.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.client;

import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import net.dreamlu.iot.mqtt.spring.client.config.MqttClientConfiguration;
import net.dreamlu.iot.mqtt.spring.client.config.MqttClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
		prefix = MqttClientProperties.PREFIX,
		name = "enabled",
		havingValue = "true",
		matchIfMissing = false
)
@ConditionalOnClass(MqttClientTemplate.class)
@Import(MqttClientConfiguration.class)
public class MqttClientAutoConfiguration {
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttAutoConfigurationTest,MqttSecurityDefaultsTest test
```

Expected: PASS. If `MqttClientTemplate` attempts a real connection during context startup, update the client tests to assert `MqttClientConfiguration` is imported with a user-provided mocked `MqttClientTemplate` bean named `mqttClientTemplate`, then verify auto-config backs off.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/client/MqttClientAutoConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/MqttServerConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/MqttAutoConfigurationTest.java
git commit -m "feat: standardize mqtt auto configuration"
```

---

### Task 3: Redis broker 条件、覆盖点和初始化失败信息

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/MqttServerBrokerConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/config/MqttBrokerConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/cluster/RedisMqttMessageDispatcher.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/cluster/RedisMqttMessageDownReceiver.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/cluster/RedisMqttMessageExchangeReceiver.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/MqttBrokerConfigurationTest.java`

**Interfaces:**
- Produces: Redis broker beans all annotated with `@ConditionalOnMissingBean`.
- Produces: clear `IllegalStateException` messages for missing Redis infrastructure.

- [ ] **Step 1: Write failing broker tests**

Create `MqttBrokerConfigurationTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.config.MqttBrokerConfiguration;
import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.serializer.IMessageSerializer;
import net.dreamlu.iot.mqtt.core.server.serializer.JsonMessageSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MqttBrokerConfigurationTest {

	@Test
	void brokerBeansBackOffForUserDispatcher() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MqttBrokerConfiguration.class))
				.withUserConfiguration(UserDispatcherConfiguration.class)
				.run(context -> {
					assertThat(context).hasSingleBean(IMqttMessageDispatcher.class);
					assertThat(context).doesNotHaveBean(RedisMqttMessageDispatcher.class);
				});
	}

	@Test
	void dispatcherFailsClearlyWhenNoRedisStreamTemplateCanBeResolved() {
		RedisMqttMessageDispatcher dispatcher =
				new RedisMqttMessageDispatcher(new JsonMessageSerializer(), "mqtt:exchange");

		org.assertj.core.api.Assertions.assertThatThrownBy(dispatcher::afterSingletonsInstantiated)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("RStreamTemplate");
	}

	@Configuration(proxyBeanMethods = false)
	static class UserDispatcherConfiguration {

		@Bean
		IMessageSerializer messageSerializer() {
			return new JsonMessageSerializer();
		}

		@Bean
		IMqttMessageDispatcher customDispatcher() {
			return mock(IMqttMessageDispatcher.class);
		}
	}
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttBrokerConfigurationTest test
```

Expected: FAIL because broker beans do not back off and dispatcher can NPE or silently keep null template.

- [ ] **Step 3: Add Redis bean condition to broker entry**

Modify `MqttServerBrokerConfiguration.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.config.MqttBrokerConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = {"net.dreamlu.mica.redis.cache.MicaRedisCache"})
@ConditionalOnBean(RedisConnectionFactory.class)
@Import(MqttBrokerConfiguration.class)
public class MqttServerBrokerConfiguration {
}
```

- [ ] **Step 4: Add missing bean conditions to broker defaults**

In `MqttBrokerConfiguration`, add `@ConditionalOnMissingBean` to every `@Bean` method that does not already have it:

```java
@Bean
@ConditionalOnMissingBean
public IMessageSerializer messageSerializer()

@Bean
@ConditionalOnMissingBean
public IMqttConnectStatusListener mqttBrokerConnectListener()

@Bean
@ConditionalOnMissingBean
public IMqttMessageStore mqttMessageStore(IMessageSerializer messageSerializer)

@Bean
@ConditionalOnMissingBean
public RedisMqttMessageExchangeReceiver mqttMessageUpReceiver(...)

@Bean
@ConditionalOnMissingBean
public RedisMqttMessageDownReceiver mqttMessageDownReceiver(...)

@Bean
@ConditionalOnMissingBean
public IMqttMessageDispatcher mqttMessageDispatcher(IMessageSerializer messageSerializer)

@Bean
@ConditionalOnMissingBean
public RedisMqttServerManage mqttServerManage(MqttServer mqttServer)

@Bean
@ConditionalOnMissingBean
public IMqttBrokerService mqttBrokerService()
```

- [ ] **Step 5: Add dispatcher failure check**

Modify `RedisMqttMessageDispatcher.afterSingletonsInstantiated()`:

```java
@Override
public void afterSingletonsInstantiated() {
	if (this.streamTemplate == null && this.context != null) {
		try {
			this.streamTemplate = this.context.getBean(RStreamTemplate.class);
		} catch (Exception e) {
			Map<String, RedisTemplate> map = MapUtils.emptyIfNull(this.context.getBeansOfType(RedisTemplate.class));
			for (RedisTemplate redisTemplate : map.values()) {
				if (!(redisTemplate instanceof StringRedisTemplate)) {
					this.streamTemplate = new DefaultRStreamTemplate(redisTemplate);
					break;
				}
			}
		}
	}
	if (this.streamTemplate == null) {
		throw new IllegalStateException("No RStreamTemplate or non-String RedisTemplate available for MQTT Redis dispatcher.");
	}
}
```

- [ ] **Step 6: Add receiver failure checks**

In both `RedisMqttMessageDownReceiver` and `RedisMqttMessageExchangeReceiver`, after resolving `RedisConnectionFactory`, add:

```java
if (this.redisConnectionFactory == null) {
	throw new IllegalStateException("No RedisConnectionFactory available for MQTT Redis receiver.");
}
```

If the field is named differently in `RedisMqttMessageExchangeReceiver`, use that class's existing field name and the same message.

- [ ] **Step 7: Run tests to verify they pass**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttBrokerConfigurationTest,MqttAutoConfigurationTest,MqttSecurityDefaultsTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/MqttServerBrokerConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/config/MqttBrokerConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/cluster/RedisMqttMessageDispatcher.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/cluster/RedisMqttMessageDownReceiver.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/cluster/RedisMqttMessageExchangeReceiver.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt/server/broker/MqttBrokerConfigurationTest.java
git commit -m "feat: harden mqtt redis broker configuration"
```

---

### Task 4: 最终验证和文档状态

**Files:**
- Modify only if tests reveal compile issues in previous touched files.

**Interfaces:**
- Consumes: all tasks above.
- Produces: passing targeted MQTT tests.

- [ ] **Step 1: Run targeted MQTT tests**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='Mqtt*Test,*Mqtt*Test' test
```

Expected: PASS.

- [ ] **Step 2: Run related autoconfigure tests**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='Mqtt*Test,*Mqtt*Test,MpAutoConfigurationTest,RedisConnectionConfigurationTest' test
```

Expected: PASS.

- [ ] **Step 3: Check git status**

Run:

```bash
git status --short
```

Expected: only known unrelated untracked files remain, or no changes if all commits were created.

- [ ] **Step 4: Commit any final compile/test adjustments**

If Step 1 or Step 2 required small fixes, commit only those touched files:

```bash
git add <changed mqtt files>
git commit -m "test: verify mqtt autoconfigure optimization"
```

Expected: final MQTT changes are committed without staging unrelated existing untracked files.

---

## Self-Review

- Spec coverage: Task 1 covers default security semantics; Task 2 covers server/client auto-configuration and user bean override; Task 3 covers Redis broker conditions, override points, and initialization failures; Task 4 covers verification.
- Placeholder scan: no `TBD`, `TODO`, or “similar to” steps are used.
- Type consistency: task interfaces use Mica MQTT 2.3.9 classes verified from local source jars: `MqttClientConfiguration`, `MqttClientTemplate`, `MqttClientCreator`, `MqttServerProperties`, `MqttServerLauncher`.
