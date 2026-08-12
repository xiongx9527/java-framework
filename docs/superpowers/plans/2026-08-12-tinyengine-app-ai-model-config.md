# TinyEngine 应用级 AI 模型配置实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 TinyEngine 模型配置按应用持久化到 MySQL，使用 `fw-plus-mybatis-encrypt` 加密 API Key，并让 Owner/Admin 管理、Dev 仅使用。

**Architecture:** TinyEngine Core 提供两表持久化、角色校验、安全 DTO 和内部凭据解析；Boot Facade 统一承载 MVC/WebFlux API；AI 自动配置层只接受应用/服务/模型标识并从 Core 获取调用凭据；机器人前端改用应用共享 API，并提供一次性本地配置迁移。

**Tech Stack:** Java 8、Spring Boot 2.7.18、MyBatis-Plus、`fw-plus-mybatis-encrypt`、MySQL/H2、Vue 3、TypeScript、Vitest、pnpm。

## Global Constraints

- Java 实现必须兼容 Java 8，不使用 Java 9 及以上 API。
- API Key 只在服务端解密，不得返回明文、密文或可逆令牌给浏览器。
- 实体 `apiKey` 使用公共 `@FieldEncrypt`；业务代码不得手动加解密或二次加密。
- `app_owner`、`app_admin` 可管理；`app_dev` 只能使用已启用模型。
- 所有数据库访问同时约束 `tenant_id` 和 `app_id`，资源 ID 不能绕过应用隔离。
- API Key 留空表示保留，非空表示替换，`clearApiKey=true` 表示清除；替换与清除不能同时出现。
- 删除应用必须在同一事务内删除对应服务和模型。
- 数据库已有配置时旧本地配置不得覆盖；迁移失败必须整体回滚并保留本地配置。
- 不修改 `t_model`；该表不是 AI 模型配置。
- 不记录真实 API Key、加密主密钥、认证头或完整上游错误响应。
- 保留工作区现有无关改动，任何提交只包含本任务文件。

---

## 文件结构

Core 新增 `AiModelServiceConfig` 与 `AiModelConfig` 实体、两个 Mapper、`AppAiModelConfigService` 及实现；请求 DTO 与安全响应 DTO 放入现有 `model/dto`，内部调用凭据使用不可序列化的 `AiModelInvocationConfig`。Boot 在现有 `TinyEngineApiFacade`、`TinyEngineAppMvcController`、`TinyEngineAppWebFluxController` 上追加同构路由。AI 层修改 `TinyEngineChatRequest` 和默认 Chat Service，使其消费 Core 的解析接口。前端新增单一 `modelConfigApi`，`useConfig` 只管理当前应用的远端状态与一次性迁移。

### Task 1: 建表迁移与加密依赖接线

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/main/resources/META-INF/tiny-engine/sql/mysql/12_create_ai_model_config_ddl.sql`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/main/resources/META-INF/tiny-engine/sql/h2/14_create_ai_model_config_ddl.sql`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/main/resources/META-INF/tiny-engine/sql/manifest.properties`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/tinyengine/support/TinyEngineH2ManifestTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/tinyengine/support/TinyEngineAiModelMigrationTest.java`

**Interfaces:**
- Consumes: `fw-plus-mybatis-encrypt`、`fw-plus-mp-encrypt` 和现有 SQL manifest runner。
- Produces: `t_ai_model_service`、`t_ai_model` 表及 Core 可编译的加密注解/拦截器依赖。

- [ ] **Step 1: 写迁移失败测试**

在 `TinyEngineAiModelMigrationTest` 中用 H2 连续执行两次迁移，并断言表、唯一约束和 `api_key VARCHAR(2048)` 存在；在 manifest 测试中追加 `h2.014`。核心断言：

```java
execute(jdbc, read("META-INF/tiny-engine/sql/h2/14_create_ai_model_config_ddl.sql"));
execute(jdbc, read("META-INF/tiny-engine/sql/h2/14_create_ai_model_config_ddl.sql"));
assertEquals(2, jdbc.queryForObject(
        "select count(*) from information_schema.tables where table_name in ('T_AI_MODEL_SERVICE','T_AI_MODEL')",
        Integer.class).intValue());
assertThrows(DataIntegrityViolationException.class, () -> insertDuplicateService(jdbc));
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/pom.xml test -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter -Dtest=TinyEngineAiModelMigrationTest,TinyEngineH2ManifestTest`

Expected: FAIL，缺少迁移资源或 manifest 项。

- [ ] **Step 3: 编写幂等 SQL 和依赖**

两种数据库均创建两表，至少包含设计中的字段及约束。Manifest 只能追加
`mysql.016=.../mysql/12_create_ai_model_config_ddl.sql` 和
`h2.014=.../h2/14_create_ai_model_config_ddl.sql`，不得重排或覆盖已有编号：

```sql
CREATE TABLE IF NOT EXISTS t_ai_model_service (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL,
  app_id INT NOT NULL,
  service_key VARCHAR(64) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  label VARCHAR(128) NOT NULL,
  base_url VARCHAR(1024) NOT NULL,
  api_key VARCHAR(2048),
  allow_empty_api_key BOOLEAN NOT NULL DEFAULT FALSE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  created_by VARCHAR(64), created_time TIMESTAMP,
  last_updated_by VARCHAR(64), last_updated_time TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (tenant_id, app_id, service_key)
);
```

`t_ai_model` 使用 `UNIQUE(tenant_id, app_id, service_id, name)`，并为 `(tenant_id, app_id)`、`service_id` 建索引。Core POM 加 `fw-plus-annotation` 和 `fw-plus-mybatis-encrypt`；Starter 加 `fw-plus-mp-encrypt`，保持版本继承和现有可选性模式。

- [ ] **Step 4: 运行迁移测试**

Run: 同 Step 2。

Expected: PASS；脚本重复执行不报错，约束生效。

- [ ] **Step 5: 提交**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/main/resources/META-INF/tiny-engine/sql \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/tinyengine/support
git commit -m "TinyEngine: 新增应用 AI 模型配置表"
```

### Task 2: Core 加密持久化与安全 DTO

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/entity/AiModelServiceConfig.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/entity/AiModelConfig.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/mapper/AiModelServiceConfigMapper.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/mapper/AiModelConfigMapper.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/mapper/tinyengine/AiModelServiceConfigMapper.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/mapper/tinyengine/AiModelConfigMapper.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/dto/AiModelServiceRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/dto/AiModelRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/dto/AppAiModelSettings.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/dto/AiModelServiceDto.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/dto/AiModelInvocationConfig.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/service/AiModelEncryptedPersistenceTest.java`

**Interfaces:**
- Consumes: Task 1 两表和 `@FieldEncrypt`。
- Produces: Mapper 方法 `findServices(tenantId, appId)`、`findService(tenantId, appId, serviceId)`、`findModels(...)`、`insert/update/delete`；安全 DTO 和内部凭据类型。

- [ ] **Step 1: 写字段加密端到端失败测试**

使用 H2、`FieldEncryptInterceptor`、`FieldDecryptInterceptor` 和固定测试密钥装配 MyBatis，插入占位密钥 `test-api-key`：

```java
mapper.insertService(service("tenant-a", 2, "test-api-key"));
String raw = jdbc.queryForObject("select api_key from t_ai_model_service where app_id = 2", String.class);
assertNotEquals("test-api-key", raw);
assertEquals("test-api-key", mapper.findService("tenant-a", 2, service.getId()).getApiKey());
assertFalse(toJson(AiModelServiceDto.from(service)).contains("test-api-key"));
assertFalse(toJson(AiModelServiceDto.from(service)).contains(raw));
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./fw-plus-core/mvnw -f fw-plus-core/pom.xml test -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=AiModelEncryptedPersistenceTest`

Expected: FAIL，实体/Mapper/DTO 尚不存在。

- [ ] **Step 3: 实现实体、Mapper 与 DTO**

实体关键字段：

```java
@TableName("t_ai_model_service")
public class AiModelServiceConfig extends BaseEntity {
    private Long id;
    private String tenantId;
    private Integer appId;
    private String serviceKey;
    private String provider;
    private String label;
    private String baseUrl;
    @FieldEncrypt
    private String apiKey;
    private Boolean allowEmptyApiKey;
    private Boolean enabled;
    private Integer sortOrder;
}
```

`AiModelServiceDto` 不定义 `apiKey`，只定义 `boolean hasApiKey`、`String apiKeyMask` 和模型列表；掩码固定为 `********`。`AiModelInvocationConfig` 定义 `appId/serviceId/provider/baseUrl/apiKey/modelName`，不加 Jackson 注解且不从 Controller 返回。

- [ ] **Step 4: 运行加密与 Mapper 测试**

Run: 同 Step 2。

Expected: PASS，原始列是密文、业务查询是明文、安全 DTO 无凭据。

- [ ] **Step 5: 提交**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
git commit -m "TinyEngine: 增加 AI 模型加密持久化"
```

### Task 3: Core 权限、事务、设置与应用删除

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/AppAiModelConfigService.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/AppAiModelConfigServiceImpl.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/dto/AiModelImportRequest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/AppServiceImpl.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/service/AppAiModelConfigServiceTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/service/AppLifecycleServiceTest.java`

**Interfaces:**
- Consumes: Task 2 Mapper/DTO、`AppRoleService.requirePermission`、`AppMapper` 的应用查询与 `extendConfig` 更新。
- Produces:

```java
List<AiModelServiceDto> listUsable(Integer appId);
List<AiModelServiceDto> listManageable(Integer appId);
AiModelServiceDto createService(Integer appId, AiModelServiceRequest request);
AiModelServiceDto updateService(Integer appId, Long serviceId, AiModelServiceRequest request);
void deleteService(Integer appId, Long serviceId);
AiModelServiceDto addModel(Integer appId, Long serviceId, AiModelRequest request);
AiModelServiceDto updateModel(Integer appId, Long serviceId, Long modelId, AiModelRequest request);
void deleteModel(Integer appId, Long serviceId, Long modelId);
List<AiModelServiceDto> importLocal(Integer appId, AiModelImportRequest request);
AiModelInvocationConfig resolveInvocation(Integer appId, Long serviceId, String modelName);
void deleteByApp(Integer appId, String tenantId);
```

- [ ] **Step 1: 写权限、密钥三态、迁移与删除失败测试**

参数化 Owner/Admin/Dev：Owner/Admin 管理成功，Dev 管理抛 `TinyEngineAccessDeniedException`，三者均可 `resolveInvocation`。覆盖跨租户/跨应用 ID、停用模型、重复导入、密钥三态和删除应用回滚：

```java
request.setApiKey(" ");
service.updateService(2, 10L, request);
verify(mapper).updateKeepingApiKey(...);

request.setApiKey("replacement");
request.setClearApiKey(Boolean.TRUE);
assertThrows(IllegalArgumentException.class, () -> service.updateService(2, 10L, request));
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./fw-plus-core/mvnw -f fw-plus-core/pom.xml test -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=AppAiModelConfigServiceTest,AppLifecycleServiceTest`

Expected: FAIL，服务不存在且应用删除未清理配置。

- [ ] **Step 3: 实现 Core 服务**

管理方法统一调用 `requirePermission(appId, AppPermission.MANAGE)`；使用/解析调用 `READ` 后再验证应用启用状态。所有 Mapper 条件显式携带当前 `tenantId` 与 `appId`。`App.extendConfig.aiModelSettings` 用复制后的 Map 更新，保留其他键。

事务规则：服务与模型写操作、导入、删除均加 `@Transactional(rollbackFor = Exception.class)`；导入先完整校验，再检查数据库为空，最后批量插入。`AppServiceImpl.deleteAppById` 在删除角色前调用：

```java
if (appAiModelConfigService != null) {
    appAiModelConfigService.deleteByApp(id, currentTenant);
}
```

- [ ] **Step 4: 运行 Core 服务测试**

Run: 同 Step 2。

Expected: PASS，权限矩阵、隔离、事务和级联均通过。

- [ ] **Step 5: 提交**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
git commit -m "TinyEngine: 实现应用 AI 模型配置服务"
```

### Task 4: MVC/WebFlux 共享配置 API

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineAppMvcController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineAppWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineAiModelMvcApiContractTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineAiModelWebFluxApiContractTest.java`

**Interfaces:**
- Consumes: Task 3 `AppAiModelConfigService` 全部公开方法。
- Produces: 设计文档列出的 `/apps/{appId}/ai-model-services` MVC/WebFlux 同构路由。

- [ ] **Step 1: 写双栈合同失败测试**

分别用 MockMvc/WebTestClient 验证列表、管理、新增、修改、删除、模型子资源和导入均委托同一 Facade；序列化结果不含 `apiKey`：

```java
mockMvc.perform(get("/app-center/api/apps/2/ai-model-services/manage"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$[0].hasApiKey").value(true))
    .andExpect(jsonPath("$[0].apiKey").doesNotExist());
```

- [ ] **Step 2: 运行合同测试确认失败**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/pom.xml test -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TinyEngineAiModelMvcApiContractTest,TinyEngineAiModelWebFluxApiContractTest`

Expected: FAIL，路由和 Facade 方法尚不存在。

- [ ] **Step 3: 增加 Facade 和控制器路由**

Facade 仅委托 Core。WebFlux 路由沿用现有 blocking scheduler 包装；MVC 直接返回同一 DTO。不要在 Controller 读取角色、实体或 API Key。

- [ ] **Step 4: 运行双栈合同和现有应用合同测试**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/pom.xml test -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TinyEngineAiModelMvcApiContractTest,TinyEngineAiModelWebFluxApiContractTest,TinyEngineAppPageMvcApiContractTest,TinyEngineWebFluxApiContractTest`

Expected: PASS，无已有应用/成员路由回归。

- [ ] **Step 5: 提交**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure
git commit -m "TinyEngine: 暴露应用 AI 模型配置接口"
```

### Task 5: AI 代理改为服务端解析凭据

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/chat/TinyEngineChatRequest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiController.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiWebFluxController.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiControllerTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiWebFluxAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `AppAiModelConfigService.resolveInvocation(Integer, Long, String)`。
- Produces: `TinyEngineChatRequest` 新字段 `Integer appId`、`Long serviceId`、`String modelName`；默认 Chat Service 使用解析后的内部凭据构造 provider 请求。

- [ ] **Step 1: 写服务端凭据解析失败测试**

测试请求只含三个标识，验证 resolver 被调用，provider 收到数据库配置；客户端即使提交旧 `baseUrl/apiKey` JSON 字段也不会影响调用。上游 401/403 转换为模型错误，不变成 TinyEngine 登录 401。

```java
request.setAppId(2);
request.setServiceId(10L);
request.setModelName("test-model");
when(configService.resolveInvocation(2, 10L, "test-model")).thenReturn(invocation("https://example.invalid/v1", "test-key"));
chatService.chat(request);
verify(provider).chat(argThat(it -> "test-model".equals(it.getModel())));
```

- [ ] **Step 2: 运行 AI 测试确认失败**

Run: `./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=TinyEngineAiControllerTest,TinyEngineAiWebFluxAutoConfigurationTest`

Expected: FAIL，新请求字段和配置服务依赖尚不存在。

- [ ] **Step 3: 修改请求与默认 Chat Service**

AI 模块增加 TinyEngine Core 依赖。默认实现构造时注入 `AppAiModelConfigService`，每次请求调用 resolver；缺少任一标识返回 400。删除正常链路对请求体模型凭据/EKEY 令牌的读取，保留旧加密端点仅供迁移兼容期使用。

- [ ] **Step 4: 运行 AI 全部 TinyEngine 测试**

Run: `./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='TinyEngineAi*Test'`

Expected: PASS，上游业务鉴权错误不触发登录跳转语义。

- [ ] **Step 5: 提交**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "TinyEngine: 从应用配置解析 AI 调用凭据"
```

### Task 6: 前端应用共享配置、权限与一次性迁移

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot/src/types/setting.types.ts`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot/src/services/api.ts`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot/src/composables/core/useConfig.ts`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot/src/components/header-extension/robot-setting/RobotSetting.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot/src/components/header-extension/robot-setting/ServiceEditDialog.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot/src/services/OpenAICompatibleProvider.ts`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot/test/composables/core/useConfig.remote.test.ts`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot/test/components/RobotSetting.permissions.test.ts`

**Interfaces:**
- Consumes: Task 4 配置 API、Task 5 聊天请求合同、当前路由应用 ID 和现有应用角色字段。
- Produces: `modelConfigApi`、远端 `useConfig`、迁移状态机及不含 API Key 的 `SelectedModelInfo`。

- [ ] **Step 1: 写远端配置和迁移失败测试**

Mock Meta HTTP：初始化先按 appId GET；Owner/Admin 在远端为空且 localStorage 有旧配置时调用 import；成功后只移除模型配置；409 不覆盖；500 保留本地数据；Dev 不导入。验证编辑密钥三态和 Dev 无管理入口。

```ts
expect(http.get).toHaveBeenCalledWith('app-center/api/apps/2/ai-model-services')
expect(chatBody).toMatchObject({ appId: 2, serviceId: 10, modelName: 'test-model' })
expect(chatBody).not.toHaveProperty('apiKey')
expect(chatBody).not.toHaveProperty('baseUrl')
```

- [ ] **Step 2: 运行前端测试确认失败**

Run: `cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console && pnpm --filter @opentiny/tiny-engine-plugin-robot test -- useConfig.remote.test.ts RobotSetting.permissions.test.ts`

Expected: FAIL，仍从 localStorage 读取并向聊天链路传凭据。

- [ ] **Step 3: 实现 API 客户端和远端状态**

`ModelService` 改为服务端 DTO：`id: number`、`serviceKey`、`hasApiKey`、`apiKeyMask`，不含 `apiKey`。`useConfig.init(appId, role)` 从 API 加载；Owner/Admin 才暴露 mutation；选中模型只生成：

```ts
{
  appId,
  serviceId: selectedService.id,
  modelName: selectedModel.name,
  chatMode: settings.chatMode,
  enableThinking: settings.enableThinking
}
```

迁移成功后重写 `tiny-engine-robot-settings`，删除 `services/defaultModel/quickModel/chatMode/enableThinking`，保留未知键；失败不写 localStorage。设置弹窗用显式“清除密钥”开关，空输入不覆盖。

- [ ] **Step 4: 运行机器人测试和构建**

Run: `cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console && pnpm --filter @opentiny/tiny-engine-plugin-robot test && pnpm --filter @opentiny/tiny-engine-plugin-robot build`

Expected: PASS；TypeScript 构建无凭据字段引用。

- [ ] **Step 5: 提交**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot
git commit -m "TinyEngine: 使用应用共享 AI 模型配置"
```

### Task 7: 全链路验证、部署和安全检查

**Files:**
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/TinyEngineAiSampleApplicationTest.java`
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/main/resources/application.yml`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/TinyEngineAppAiModelIT.java`

**Interfaces:**
- Consumes: Tasks 1-6 的完整功能。
- Produces: 可部署 Starter、样例安全配置模板和端到端验收证据。

- [ ] **Step 1: 写样例端到端失败测试**

使用占位密钥和测试加密参数启动样例，登录后以 Owner 创建配置、Dev 读取并发起模拟上游调用，直接 JDBC 断言密文；删除应用后断言两表为 0。配置只引用环境变量：

```yaml
fw:
  plus:
    mp-encrypt:
      private-key: ${TINY_ENGINE_FIELD_ENCRYPT_KEY:}
      iv: ${TINY_ENGINE_FIELD_ENCRYPT_IV:}
      salt: ${TINY_ENGINE_FIELD_ENCRYPT_SALT:}
```

- [ ] **Step 2: 运行样例测试确认失败**

Run: `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml test -pl fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample -Dtest=TinyEngineAppAiModelIT`

Expected: FAIL，样例尚未接入测试加密参数或新接口。

- [ ] **Step 3: 完成样例配置与测试夹具**

测试 profile 使用固定占位加密参数，主配置只读环境变量。Mock 上游只记录模型名和是否有 Authorization，不打印其值。补充无加密配置时拒绝写入模型密钥的启动/服务测试。

- [ ] **Step 4: 执行分层回归**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml test -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
./fw-plus-boot/mvnw -f fw-plus-boot/pom.xml test -pl fw-plus-boot-project/fw-plus-boot-autoconfigure,fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter
./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml test -pl fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console
pnpm --filter @opentiny/tiny-engine-plugin-robot test
pnpm run build
```

Expected: 全部 PASS/BUILD SUCCESS。

- [ ] **Step 5: 执行凭据泄漏扫描**

Run:

```bash
rg -n "apiKey|api_key|Authorization" \
  fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot/src
```

Expected: 每个命中均属于实体内部、写请求或上游调用；响应 DTO、前端模型状态和日志语句中无密钥值。另用占位字符串搜索构建产物，结果为 0。

- [ ] **Step 6: 安装、重启本机样例并进行角色验收**

按 Core → Boot → AI → Sample 顺序安装需要模块，保留本机 MySQL `tiny_engine` 数据并重启 19080。使用 Owner/Admin/Dev 验证：共享可见、管理权限、Dev 可聊天、密钥不可见、错误不跳登录、错误只弹一次。不要在命令行或日志中传入真实 API Key。

- [ ] **Step 7: 提交样例和最终验证调整**

```bash
git add fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample
git commit -m "TinyEngine: 验证应用共享 AI 模型配置"
```

## 完成标准

- MySQL 原始 `api_key` 列为密文，业务调用可正确解密，浏览器和 HTTP 响应无法取得凭据。
- 同一应用所有成员看到相同配置；不同租户、不同应用互相隔离。
- Owner/Admin 可管理，Dev 可使用但不可管理。
- 密钥三态、重复约束、事务回滚、旧配置迁移和应用删除清理均有自动化覆盖。
- MVC、WebFlux、AI、前端、Starter 和样例测试通过，19080 已重新部署供人工验证。
