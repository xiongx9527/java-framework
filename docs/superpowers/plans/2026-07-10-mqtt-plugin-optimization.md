# Magic-API MQTT Plugin Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 默认关闭 Magic-API 内置 MQTT 服务端插件，并系统优化 `mqttclient` 客户端插件的配置解析、订阅 clientId、资源释放和错误提示。

**Architecture:** 将 `magicapi/plugin/mqtt` 降级为显式启用的高级能力；将 `magicapi/plugin/mqttclient` 作为主路径。新增一个小型 MQTT Client 配置工具类集中处理端口、QoS、订阅 clientId 和 `other` 配置，避免 producer/consumer/dynamic datasource 三处重复实现。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Magic-API、Mica MQTT、JUnit 5、AssertJ、Mockito、Maven Wrapper。

## Global Constraints

- 全程使用中文沟通；代码实现新增或调整的关键逻辑需要加中文注释。
- 不删除 `magicapi/plugin/mqtt` 服务端插件源码。
- 不实现完整标准 MQTT Broker。
- 不重命名 Magic-API 资源目录。
- 不引入新的重型依赖。
- 不大规模改造前端控制台插件。
- 不破坏已有 `mqttclient` 的 `key`、`ip`、`port`、`clientId`、`username`、`password`、`other` 配置格式。
- 使用 Java 8 兼容写法，不使用 Java 9+ API。
- 每个任务验收后独立提交，不积攒提交。

---

## File Structure

### 修改文件

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfiguration.java`
  - 将内置 MQTT 服务端插件改为显式启用。
  - 修正服务端数据源注释和 `magicDynamicWeaviateDataSource` 误导性变量名。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/module/MqttModule.java`
  - 修正 `magicDynamicWeaviateDataSource` 字段名和相关中文注释。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/common/MqttClientConfigSupport.java`
  - 新建工具类，集中处理 MQTT Client 端口解析、QoS 解析、订阅 clientId 派生和 `other` 配置应用。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/producer/MqttClientProducer.java`
  - 使用 `MqttClientConfigSupport` 统一配置解析。
  - 明确 SSL 当前不作为完整能力启用。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/consumer/MqttClientConsumer.java`
  - 使用 `MqttClientConfigSupport` 统一配置解析。
  - 修正订阅端配置中心覆盖后的 `clientId` 未实际使用问题。
  - 为每个 topic 派生独立订阅 clientId，避免多 topic 共用同一 clientId 互踢。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/model/MagicDynamicMqttClientDataSource.java`
  - 使用 `MqttClientConfigSupport` 统一配置解析。
  - 删除数据源时同时关闭 producer 与 consumer。
  - 默认 producer 列表获取失败时记录调试日志，不再完全吞异常。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/module/MqttClientModule.java`
  - 增加统一的 producer 可用性校验，错误信息包含数据源 key。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientDataSourceMagicDynamicRegistry.java`
  - 数据源取消注册时同时关闭 producer 和 consumer。
  - 将原本 private 的取消注册辅助方法改成包可见，便于单元测试覆盖。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientSubscribeMagicDynamicRegistry.java`
  - 按项目既有 MQ 插件规则，将 subscribe 业务资源注册调整为 `super.register(entity)` 成功后再创建业务订阅。
  - 异常日志不再完全吞掉。

### 新增测试文件

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfigurationTest.java`
  - 验证服务端插件默认不加载，显式启用时加载。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/common/MqttClientConfigSupportTest.java`
  - 验证端口、QoS、订阅 clientId 派生。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/module/MqttClientModuleTest.java`
  - 验证缺失数据源时错误信息带 key。

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientDataSourceMagicDynamicRegistryTest.java`
  - 验证删除数据源时同时删除 producer 和 consumer。

---

### Task 1: 默认关闭 Magic-API MQTT 服务端插件

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfiguration.java:28-84`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfigurationTest.java`

**Interfaces:**
- Consumes: Spring Boot `@ConditionalOnProperty` 条件。
- Produces: `MagicAPIMqttConfiguration` 仅在 `magic-api.plugin.mqtt.enabled=true` 时注册 `MqttModule` 和 `BaseMagicDynamicMqttDataSource`。

- [ ] **Step 1: Write the failing test**

Create `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfigurationTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.BaseMagicDynamicMqttDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.module.MqttModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIMqttConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MagicAPIMqttConfiguration.class))
            .withUserConfiguration(MagicApiSupportConfiguration.class);

    @Test
    void mqttServerPluginBacksOffWhenPropertyIsMissing() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(MqttModule.class)
                .doesNotHaveBean(BaseMagicDynamicMqttDataSource.class));
    }

    @Test
    void mqttServerPluginLoadsWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("magic-api.plugin.mqtt.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(MqttModule.class)
                        .hasSingleBean(BaseMagicDynamicMqttDataSource.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class MagicApiSupportConfiguration {

        @Bean
        MagicAPIProperties magicAPIProperties() {
            return new MagicAPIProperties();
        }

        @Bean
        MagicResourceService magicResourceService() {
            return mock(MagicResourceService.class);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run from repo root:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIMqttConfigurationTest test
```

Expected: `mqttServerPluginBacksOffWhenPropertyIsMissing` fails because `MagicAPIMqttConfiguration` currently uses `matchIfMissing = true`.

- [ ] **Step 3: Write minimal implementation**

Modify `MagicAPIMqttConfiguration.java` annotation and nearby comments:

```java
@Slf4j
// 内置 MQTT 服务端会监听端口并承担 Broker 职责，属于高级可选能力；普通外连 Broker 场景请使用 mqttclient 插件。
@ConditionalOnProperty(prefix = "magic-api.plugin.mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnClass(name = {"net.dreamlu.iot.mqtt.spring.server.MqttServerTemplate"})
@EnableConfigurationProperties(MagicAPIMqttConfig.class)
@Configuration
public class MagicAPIMqttConfiguration implements MagicPluginConfiguration {
```

Also fix misleading parameter names in the same file:

```java
/**
 * 创建并初始化 MqttModule 模块。
 * 该模块面向内置 MQTT 服务端，依赖动态 MQTT 服务端数据源，并可选注入默认 MqttServer 实例。
 *
 * @param magicDynamicMqttDataSource MQTT 服务端动态数据源实例。
 * @param mqttServer                可选的 MQTT 服务端实例。
 * @return 返回初始化后的 MqttModule 实例。
 */
@Bean
@DependsOn(value = {"magicDynamicMqttDataSource"})
public MqttModule mqttModule(BaseMagicDynamicMqttDataSource magicDynamicMqttDataSource,
                             @Autowired(required = false) MqttServer mqttServer) {
    return new MqttModule(magicDynamicMqttDataSource, mqttServer);
}
```

Fix standalone/cluster comments:

```java
/**
 * 创建单机模式的内置 MQTT 服务端数据源。
 * 当未启用 Magic-API Redis 插件时，服务端消息分发使用本机内存组件。
 */
```

```java
/**
 * 创建集群模式的内置 MQTT 服务端数据源。
 * 当启用 Magic-API Redis 插件时，服务端消息分发使用 Redis 组件。
 */
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIMqttConfigurationTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfiguration.java \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfigurationTest.java
git commit -m "fix: disable magic api mqtt server plugin by default"
```

---

### Task 2: 清理服务端模块的误导性命名

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/module/MqttModule.java:18-78`

**Interfaces:**
- Consumes: `BaseMagicDynamicMqttDataSource#getServer(String)`。
- Produces: `MqttModule#getDynamicModule(MagicScriptContext)` 与 `getDynamicAttribute(String)` 行为保持不变，仅命名和注释更准确。

- [ ] **Step 1: Write the failing static check**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
p = Path('fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/module/MqttModule.java')
text = p.read_text(encoding='utf-8')
assert 'Weaviate' not in text and 'weaviate' not in text
PY
```

Expected: FAIL because `magicDynamicWeaviateDataSource` exists.

- [ ] **Step 2: Rename field and comments**

Replace the relevant parts of `MqttModule.java` with:

```java
@Slf4j
@MagicModule("mqttServer")
public class MqttModule implements DynamicAttribute<MqttModule, MqttModule>, DynamicModule<MqttModule> {

    /**
     * 内置 MQTT 服务端动态数据源，用于按 Magic-API 资源配置管理多个 MqttServer。
     */
    private final BaseMagicDynamicMqttDataSource magicDynamicMqttDataSource;

    /**
     * 当前模块绑定的 MQTT 服务端实例；默认模块可能为空，按 key 切换后绑定具体服务端。
     */
    private final MqttServer mqttServer;

    public MqttModule(BaseMagicDynamicMqttDataSource magicDynamicMqttDataSource, MqttServer mqttServer) {
        this.magicDynamicMqttDataSource = magicDynamicMqttDataSource;
        this.mqttServer = mqttServer;
    }

    /**
     * 获取当前绑定服务端的模板。
     * 调用方需要确保当前模块已通过默认服务端或动态 key 绑定到 MqttServer。
     */
    public MqttServerTemplate getMqttServerTemplate() {
        return new MqttServerTemplate(this.mqttServer);
    }

    @Override
    public MqttModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        return new MqttModule(this.magicDynamicMqttDataSource, this.magicDynamicMqttDataSource.getServer(dataSourceKey).getMqttServer());
    }

    @Override
    public MqttModule getDynamicAttribute(String key) {
        return new MqttModule(this.magicDynamicMqttDataSource, this.magicDynamicMqttDataSource.getServer(key).getMqttServer());
    }
}
```

- [ ] **Step 3: Re-run static check**

```bash
python3 - <<'PY'
from pathlib import Path
p = Path('fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/module/MqttModule.java')
text = p.read_text(encoding='utf-8')
assert 'Weaviate' not in text and 'weaviate' not in text
PY
```

Expected: PASS.

- [ ] **Step 4: Run compile for touched module**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/module/MqttModule.java
git commit -m "refactor: clarify magic api mqtt server module naming"
```

---

### Task 3: 新增 MQTT Client 配置工具类

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/common/MqttClientConfigSupport.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/common/MqttClientConfigSupportTest.java`

**Interfaces:**
- Produces: `MqttClientConfigSupport.parsePort(String): int`
- Produces: `MqttClientConfigSupport.parseQos(String): MqttQoS`
- Produces: `MqttClientConfigSupport.buildSubscribeClientId(String, String, String): String`
- Produces: `MqttClientConfigSupport.applyCommonOptions(MqttClientCreator, String): void`

- [ ] **Step 1: Write the failing test**

Create `MqttClientConfigSupportTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.common;

import net.dreamlu.iot.mqtt.codec.MqttQoS;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttClientConfigSupportTest {

    @Test
    void parsePortFallsBackToDefaultMqttPort() {
        assertThat(MqttClientConfigSupport.parsePort("1884")).isEqualTo(1884);
        assertThat(MqttClientConfigSupport.parsePort("bad")).isEqualTo(1883);
        assertThat(MqttClientConfigSupport.parsePort(null)).isEqualTo(1883);
    }

    @Test
    void parseQosSupportsZeroOneTwoAndFallsBackToZero() {
        assertThat(MqttClientConfigSupport.parseQos("0")).isEqualTo(MqttQoS.QOS0);
        assertThat(MqttClientConfigSupport.parseQos("1")).isEqualTo(MqttQoS.QOS1);
        assertThat(MqttClientConfigSupport.parseQos("2")).isEqualTo(MqttQoS.QOS2);
        assertThat(MqttClientConfigSupport.parseQos("bad")).isEqualTo(MqttQoS.QOS0);
        assertThat(MqttClientConfigSupport.parseQos(null)).isEqualTo(MqttQoS.QOS0);
    }

    @Test
    void buildSubscribeClientIdKeepsBaseReadableAndAddsTopicSuffix() {
        String clientId = MqttClientConfigSupport.buildSubscribeClientId("device-client", "iot", "device/+/status");

        assertThat(clientId).startsWith("device-client-sub-");
        assertThat(clientId).contains("device___status");
        assertThat(clientId.length()).isLessThanOrEqualTo(64);
    }

    @Test
    void buildSubscribeClientIdFallsBackToDataSourceKeyWhenBaseIsBlank() {
        String clientId = MqttClientConfigSupport.buildSubscribeClientId("", "iot", "device/a/status");

        assertThat(clientId).startsWith("iot-sub-");
        assertThat(clientId.length()).isLessThanOrEqualTo(64);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttClientConfigSupportTest test
```

Expected: FAIL because `MqttClientConfigSupport` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `MqttClientConfigSupport.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.client.MqttClientCreator;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public final class MqttClientConfigSupport {

    private static final int DEFAULT_MQTT_PORT = 1883;
    private static final int MAX_CLIENT_ID_LENGTH = 64;

    private MqttClientConfigSupport() {
    }

    public static int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (Exception e) {
            return DEFAULT_MQTT_PORT;
        }
    }

    public static MqttQoS parseQos(String qos) {
        try {
            int qosInt = Integer.parseInt(qos);
            if (qosInt == 1) {
                return MqttQoS.QOS1;
            }
            if (qosInt == 2) {
                return MqttQoS.QOS2;
            }
        } catch (Exception ignored) {
        }
        return MqttQoS.QOS0;
    }

    public static String buildSubscribeClientId(String clientId, String dataSourceKey, String topic) {
        String base = StringUtils.defaultIfBlank(clientId, dataSourceKey);
        base = StringUtils.defaultIfBlank(base, "mqttclient");
        String topicPart = StringUtils.defaultIfBlank(topic, "topic")
                .replaceAll("[^a-zA-Z0-9_-]", "_");
        String value = base + "-sub-" + topicPart;
        if (value.length() <= MAX_CLIENT_ID_LENGTH) {
            return value;
        }
        String hash = Integer.toHexString(value.hashCode());
        int keepLength = Math.max(1, MAX_CLIENT_ID_LENGTH - hash.length() - 1);
        return value.substring(0, keepLength) + "-" + hash;
    }

    public static void applyCommonOptions(MqttClientCreator mqttClientCreator, String other) {
        if (StringUtils.isBlank(other)) {
            return;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(other);
            if (jsonObject.containsKey("keepAliveSec")) {
                mqttClientCreator.keepAliveSecs(jsonObject.getIntValue("keepAliveSec"));
            }
            if (jsonObject.containsKey("reconnect")) {
                mqttClientCreator.reconnect(jsonObject.getBooleanValue("reconnect"));
            }
            if (jsonObject.containsKey("reIntervalSec")) {
                mqttClientCreator.reInterval(jsonObject.getIntValue("reIntervalSec"));
            }
            if (jsonObject.containsKey("readBufferSize")) {
                mqttClientCreator.readBufferSize(jsonObject.getIntValue("readBufferSize"));
            }
            if (jsonObject.containsKey("maxBytesInMessage")) {
                mqttClientCreator.maxBytesInMessage(jsonObject.getIntValue("maxBytesInMessage"));
            }
            if (jsonObject.containsKey("cleanSession")) {
                mqttClientCreator.cleanSession(jsonObject.getBooleanValue("cleanSession"));
            }
            if (jsonObject.containsKey("ssl")) {
                // 当前 Mica MQTT Client 侧 SSL 需要 SslConfig 等完整参数；这里不把布尔 ssl 误当成完整支持。
                log.warn("MQTT Client other.ssl 已配置，但当前插件尚未根据该字段创建完整 SSL 配置");
            }
        } catch (Exception e) {
            log.warn("解析 MQTT Client other 配置失败: {}", e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttClientConfigSupportTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/common/MqttClientConfigSupport.java \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/common/MqttClientConfigSupportTest.java
git commit -m "feat: add mqtt client config support"
```

---

### Task 4: 复用工具类优化 MQTT Client producer/consumer/data source

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/producer/MqttClientProducer.java:4-148`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/consumer/MqttClientConsumer.java:4-175`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/model/MagicDynamicMqttClientDataSource.java:4-147`

**Interfaces:**
- Consumes: `MqttClientConfigSupport.parsePort(String)`、`parseQos(String)`、`buildSubscribeClientId(String, String, String)`、`applyCommonOptions(MqttClientCreator, String)`。
- Produces: Producer、Consumer、DynamicDataSource 的外部方法签名不变。

- [ ] **Step 1: Run existing helper tests as safety baseline**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttClientConfigSupportTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Refactor MqttClientProducer**

In `MqttClientProducer.java`, remove direct `JSON` / `JSONObject` option parsing imports and add:

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.common.MqttClientConfigSupport;
```

Replace `createMqttClientTemplate()` body with:

```java
@Transient
private MqttClientTemplate createMqttClientTemplate() {
    String ip = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "ip", dataSourceInfo.getIp());
    String port = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "port", dataSourceInfo.getPort());
    String clientId = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "clientId", dataSourceInfo.getClientId());
    String username = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "username", dataSourceInfo.getUsername());
    String password = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "password", dataSourceInfo.getPassword());

    MqttClientCreator mqttClientCreator = MqttClient.create()
            .ip(ip)
            .port(MqttClientConfigSupport.parsePort(port))
            .clientId(clientId)
            .username(username)
            .password(password)
            .bufferAllocator(ByteBufferAllocator.HEAP);

    String other = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "other", dataSourceInfo.getOther());
    MqttClientConfigSupport.applyCommonOptions(mqttClientCreator, other);
    return new MqttClientTemplate(mqttClientCreator);
}
```

- [ ] **Step 3: Refactor MagicDynamicMqttClientDataSource**

Add import:

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.common.MqttClientConfigSupport;
```

Replace `createMqttClientTemplate(MqttClientDataSourceInfo dataSourceInfo)` body with:

```java
public MqttClientTemplate createMqttClientTemplate(MqttClientDataSourceInfo dataSourceInfo) throws Exception {
    String ip = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "ip", dataSourceInfo.getIp());
    String port = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "port", dataSourceInfo.getPort());
    String clientId = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "clientId", dataSourceInfo.getClientId());
    String username = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "username", dataSourceInfo.getUsername());
    String password = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "password", dataSourceInfo.getPassword());

    MqttClientCreator mqttClientCreator = MqttClient.create()
            .ip(ip)
            .port(MqttClientConfigSupport.parsePort(port))
            .clientId(clientId)
            .username(username)
            .password(password);

    String other = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "other", dataSourceInfo.getOther());
    MqttClientConfigSupport.applyCommonOptions(mqttClientCreator, other);

    MqttClientTemplate template = new MqttClientTemplate(mqttClientCreator);
    template.setApplicationContext(SpringUtils.getContext());
    template.afterPropertiesSet();
    return template;
}
```

Update `deleteProducer` to guard blank keys:

```java
public void deleteProducer(String datasourceKey) {
    if (StringUtils.isBlank(datasourceKey)) {
        return;
    }
    MqttClientProducer producer = this.producerMap.remove(datasourceKey);
    if (producer != null) {
        producer.close();
    }
}
```

Update `deleteConsumer` similarly:

```java
public void deleteConsumer(String datasourceKey) {
    if (StringUtils.isBlank(datasourceKey)) {
        return;
    }
    MqttClientConsumer consumer = this.consumerMap.remove(datasourceKey);
    if (consumer != null) {
        consumer.close();
    }
}
```

Update swallowed exception in `producerNodes()`:

```java
} catch (Exception e) {
    log.debug("未找到默认 MQTT ClientTemplate，跳过默认 MQTT Client 数据源暴露", e);
}
```

- [ ] **Step 4: Refactor MqttClientConsumer**

Add import:

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.common.MqttClientConfigSupport;
```

In `createConsumer`, replace port and creator setup with:

```java
String ip = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "ip", dataSourceInfo.getIp());
String port = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "port", dataSourceInfo.getPort());
String clientId = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "clientId", dataSourceInfo.getClientId());
String username = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "username", dataSourceInfo.getUsername());
String password = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "password", dataSourceInfo.getPassword());

// 每个订阅 topic 使用独立 clientId，避免同一数据源多个订阅连接互相踢下线。
String subscribeClientId = MqttClientConfigSupport.buildSubscribeClientId(clientId, dataSourceInfo.getKey(), topicName);
MqttClientCreator mqttClientCreator = MqttClient.create()
        .ip(ip)
        .port(MqttClientConfigSupport.parsePort(port))
        .clientId(subscribeClientId)
        .username(username)
        .password(password);
```

Replace `other` parsing with:

```java
String other = MagicAPIConfigUtils.getMqttClientSubscribeConfig(dataSourceInfo.getKey(), topicName, "other", subscribeScriptInfo.getOther());
MqttClientConfigSupport.applyCommonOptions(mqttClientCreator, other);
```

Replace QoS parsing with:

```java
String qosStr = MagicAPIConfigUtils.getMqttClientSubscribeConfig(dataSourceInfo.getKey(), topicName, "qos", subscribeScriptInfo.getQos());
MqttQoS qos = MqttClientConfigSupport.parseQos(qosStr);
```

- [ ] **Step 5: Run focused tests and compile**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttClientConfigSupportTest test
```

Expected: `BUILD SUCCESS`.

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/producer/MqttClientProducer.java \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/consumer/MqttClientConsumer.java \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/model/MagicDynamicMqttClientDataSource.java
git commit -m "refactor: share mqtt client config parsing"
```

---

### Task 5: 优化 MQTT Client 模块错误信息

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/module/MqttClientModule.java:43-92`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/module/MqttClientModuleTest.java`

**Interfaces:**
- Produces: `MqttClientModule#publish(String, Object)`、`publish(String, Object, MqttQoS)`、`publish(String, Object, int)` 签名不变。
- Produces: 缺失数据源时 `IllegalStateException` 消息包含 key。

- [ ] **Step 1: Write the failing test**

Create `MqttClientModuleTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer.MqttClientConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MagicDynamicMqttClientDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MqttClientModuleTest {

    @Test
    void publishWithMissingDynamicDataSourceIncludesKeyInErrorMessage() {
        MagicDynamicMqttClientDataSource dataSource = new MagicDynamicMqttClientDataSource(mock(MqttClientConsumerService.class));
        MqttClientModule module = new MqttClientModule(dataSource).getDynamicAttribute("missing");

        assertThatThrownBy(() -> module.publish("topic/a", "payload"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttClientModuleTest test
```

Expected: FAIL because current message is `未找到可用的MQTT Client数据源` and does not include `missing`.

- [ ] **Step 3: Write minimal implementation**

Modify `MqttClientModule.java`:

```java
private MqttClientProducer requireProducer() {
    if (this.mqttClientProducer != null) {
        return this.mqttClientProducer;
    }
    String dataSourceKey = StringUtils.defaultIfBlank(this.key, "default");
    throw new IllegalStateException("未找到可用的 MQTT Client 数据源: " + dataSourceKey);
}

public boolean publish(String topic, Object payload) {
    return requireProducer().publish(topic, payload);
}

public boolean publish(String topic, Object payload, MqttQoS qos) {
    return requireProducer().publish(topic, payload, qos);
}

public boolean publish(String topic, Object payload, int qos) {
    MqttQoS mqttQoS;
    switch (qos) {
        case 1:
            mqttQoS = MqttQoS.QOS1;
            break;
        case 2:
            mqttQoS = MqttQoS.QOS2;
            break;
        default:
            mqttQoS = MqttQoS.QOS0;
            break;
    }
    return publish(topic, payload, mqttQoS);
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttClientModuleTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/module/MqttClientModule.java \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/module/MqttClientModuleTest.java
git commit -m "fix: include mqtt client datasource key in errors"
```

---

### Task 6: 修正 MQTT Client 数据源注销生命周期

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientDataSourceMagicDynamicRegistry.java:44-46`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientDataSourceMagicDynamicRegistryTest.java`

**Interfaces:**
- Produces: package-private `void unregisterByEntity(MqttClientDataSourceInfo info)` for same-package tests。
- Produces: 数据源注销时调用 `deleteProducer(key)` 和 `deleteConsumer(key)`。

- [ ] **Step 1: Write the failing test**

Create `MqttClientDataSourceMagicDynamicRegistryTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer.MqttClientConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MagicDynamicMqttClientDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientDataSourceInfo;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MqttClientDataSourceMagicDynamicRegistryTest {

    @Test
    void unregisterDataSourceDeletesProducerAndConsumer() {
        TrackingMqttClientDataSource dataSource = new TrackingMqttClientDataSource();
        MqttClientDataSourceMagicDynamicRegistry registry = new MqttClientDataSourceMagicDynamicRegistry(
                mock(MagicResourceStorage.class), dataSource);
        MqttClientDataSourceInfo info = new MqttClientDataSourceInfo();
        info.setKey("iot");

        registry.unregisterByEntity(info);

        assertThat(dataSource.deletedProducerKey).isEqualTo("iot");
        assertThat(dataSource.deletedConsumerKey).isEqualTo("iot");
    }

    static class TrackingMqttClientDataSource extends MagicDynamicMqttClientDataSource {

        private String deletedProducerKey;
        private String deletedConsumerKey;

        TrackingMqttClientDataSource() {
            super(mock(MqttClientConsumerService.class));
        }

        @Override
        public void deleteProducer(String datasourceKey) {
            this.deletedProducerKey = datasourceKey;
        }

        @Override
        public void deleteConsumer(String datasourceKey) {
            this.deletedConsumerKey = datasourceKey;
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttClientDataSourceMagicDynamicRegistryTest test
```

Expected: compile fails because `unregisterByEntity` is private, or assertion fails because consumer is not deleted.

- [ ] **Step 3: Write minimal implementation**

Modify `MqttClientDataSourceMagicDynamicRegistry.java`:

```java
void unregisterByEntity(MqttClientDataSourceInfo info) {
    // 数据源删除时生产者和消费者都必须释放，否则订阅连接会继续占用外部 Broker 资源。
    this.magicDynamicMqttClientDataSource.deleteProducer(info.getKey());
    this.magicDynamicMqttClientDataSource.deleteConsumer(info.getKey());
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MqttClientDataSourceMagicDynamicRegistryTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientDataSourceMagicDynamicRegistry.java \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientDataSourceMagicDynamicRegistryTest.java
git commit -m "fix: release mqtt client consumers on datasource removal"
```

---

### Task 7: 调整 MQTT Client 订阅注册顺序和异常日志

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientSubscribeMagicDynamicRegistry.java:38-130`

**Interfaces:**
- Produces: `register(MqttClientSubscribeInfo): boolean` 仍返回注册结果。
- Produces: 只有 `super.register(entity)` 成功后才创建业务订阅。

- [ ] **Step 1: Run a source check that currently fails**

```bash
python3 - <<'PY'
from pathlib import Path
p = Path('fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientSubscribeMagicDynamicRegistry.java')
text = p.read_text(encoding='utf-8')
needle = 'boolean result = super.register(entity);\n            if (result) {\n                registerByEntity(entity);\n            }'
assert needle in text
PY
```

Expected: FAIL because current code calls `registerByEntity(entity)` before `super.register(entity)`.

- [ ] **Step 2: Update event logging and register order**

Modify `MqttClientSubscribeMagicDynamicRegistry.java` event handlers:

```java
@EventListener(condition = "#event.type == 'mqttclient-subscribe'")
public void onFileEvent(FileEvent event) {
    try {
        processEvent(event);
    } catch (Exception e) {
        log.warn("处理 MQTT Client 订阅文件事件失败", e);
    }
}

@EventListener(condition = "#event.type == 'mqttclient-subscribe'")
public void onGroupEvent(GroupEvent event) {
    try {
        processEvent(event);
    } catch (Exception e) {
        log.warn("处理 MQTT Client 订阅分组事件失败", e);
    }
}
```

Modify `register(MqttClientSubscribeInfo entity)`:

```java
@Override
public boolean register(MqttClientSubscribeInfo entity) {
    try {
        boolean result = super.register(entity);
        if (result) {
            // 订阅属于业务资源，必须等 Magic-API 资源保存成功后再创建外部 MQTT 订阅。
            registerByEntity(entity);
        }
        return result;
    } catch (Exception e) {
        log.error("MQTT Client 订阅:[{}]注册失败", entity.getTopic(), e);
        return false;
    }
}
```

Modify `register(MappingNode<MqttClientSubscribeInfo> mappingNode)` catch:

```java
@Override
protected boolean register(MappingNode<MqttClientSubscribeInfo> mappingNode) {
    try {
        return super.register(mappingNode);
    } catch (Exception e) {
        log.error("MQTT Client 订阅:[{}]映射注册失败", mappingNode.getEntity().getTopic(), e);
        return false;
    }
}
```

Modify `unregister` catch:

```java
@Override
protected void unregister(MappingNode<MqttClientSubscribeInfo> mappingNode) {
    unregisterByEntity(mappingNode.getEntity());
    try {
        super.unregister(mappingNode);
    } catch (Exception e) {
        log.warn("MQTT Client 订阅:[{}]映射取消注册失败", mappingNode.getEntity().getTopic(), e);
    }
}
```

- [ ] **Step 3: Re-run source check**

```bash
python3 - <<'PY'
from pathlib import Path
p = Path('fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientSubscribeMagicDynamicRegistry.java')
text = p.read_text(encoding='utf-8')
needle = 'boolean result = super.register(entity);\n        if (result) {\n            // 订阅属于业务资源，必须等 Magic-API 资源保存成功后再创建外部 MQTT 订阅。\n            registerByEntity(entity);\n        }'
assert needle in text
assert 'catch (Exception ignored)' not in text
PY
```

Expected: PASS.

- [ ] **Step 4: Run compile**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientSubscribeMagicDynamicRegistry.java
git commit -m "fix: register mqtt client subscriptions after persistence"
```

---

### Task 8: Run focused verification

**Files:**
- No source changes unless verification reveals failures.

**Interfaces:**
- Consumes: All previous task outputs.
- Produces: Verified compile/test status for the MQTT plugin optimization.

- [ ] **Step 1: Run focused tests**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure \
  -Dtest=MagicAPIMqttConfigurationTest,MqttClientConfigSupportTest,MqttClientModuleTest,MqttClientDataSourceMagicDynamicRegistryTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run related existing MQTT tests**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure \
  -Dtest=MqttAutoConfigurationTest,MqttSecurityDefaultsTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run compile for autoconfigure module**

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Inspect working tree**

```bash
git status --short
```

Expected: only intentional files from this feature are modified or untracked.

- [ ] **Step 5: Commit verification note if any test-only adjustments were needed**

If Step 1-3 required no additional changes, do not create an empty commit. If test-only adjustments were needed, commit them:

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient
git commit -m "test: verify magic api mqtt plugin optimization"
```

---

## Self-Review

### Spec coverage

- 服务端插件默认关闭：Task 1。
- 服务端命名和注释清理：Task 1、Task 2。
- `mqttclient` 配置解析统一：Task 3、Task 4。
- 订阅端 clientId 互踢风险：Task 3、Task 4。
- 缺失数据源错误信息：Task 5。
- 数据源删除释放 producer/consumer：Task 6。
- subscribe 注册顺序和异常日志：Task 7。
- 编译和测试验证：Task 8。

### Placeholder scan

本计划不包含 `TBD`、`TODO`、`implement later` 或未定义的占位步骤。每个代码修改步骤都给出具体路径、代码片段、命令和预期结果。

### Type consistency

- `MqttClientConfigSupport.parsePort(String)` 在 Task 3 定义，Task 4 使用。
- `MqttClientConfigSupport.parseQos(String)` 在 Task 3 定义，Task 4 使用。
- `MqttClientConfigSupport.buildSubscribeClientId(String, String, String)` 在 Task 3 定义，Task 4 使用。
- `MqttClientConfigSupport.applyCommonOptions(MqttClientCreator, String)` 在 Task 3 定义，Task 4 使用。
- `MqttClientDataSourceMagicDynamicRegistry.unregisterByEntity(MqttClientDataSourceInfo)` 在 Task 6 改为包可见并由同包测试调用。
