# Tinyflow Dify External 请求限额与审计实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变现有 Dify External HTTP 契约的前提下，于 MVC 解析前限制请求体，并让 HTTP 与 direct Service 调用共享不可绕过的语义限额和恰好一次的脱敏审计。

**Architecture:** 使用 `OncePerRequestFilter` 包装目标 `/retrieval` 请求的输入流并按原始字节数限制 body；`TinyflowDifyExternalKnowledgeService` 作为 query 与 metadata 语义限额的唯一边界；`TinyflowDifyExternalAuditCoordinator` 统一审计事件构造和持久化失败策略，由 Filter、Controller 与 Service 按委托边界调用。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Spring MVC/Servlet API、JUnit 4、Mockito、MockMvc、Maven 3.6.3、Magic-API 2.2.2。

## Global Constraints

- Java 源码必须兼容 Java 8，不使用 `record`、`var`、模式匹配或 Java 9+ API。
- 保持 `POST ${fw.plus.ai.tinyflow.dify.external.base-path}/retrieval`、`IKnowledgeService` 和现有 Dify 错误码不变。
- HTTP 错误优先级保持为：Bearer header → Access Key → scope → 完整 JSON/业务校验。
- Filter 按原始字节限制 body，不按 `String.length()` 计数，也不预读或缓存完整 body。
- Service 是 query 长度、metadata 条件数量和 metadata value 深度的唯一语义校验边界。
- 审计事件不得接收或保存 Authorization、Access Key、query、metadata value、完整正文或异常消息。
- 成功业务的审计失败必须 fail-closed；已有业务失败的审计失败不得覆盖原异常，只能作为 suppressed 附加。
- 不迁移 Core `TinyflowKnowledgeAuditSink`，不引入异步审计，不修改真实 Dify/浏览器/最终部署 E2E 范围。
- 当前位于 Comet Native Build；只修改当前需求范围，完成后通过 Runtime Builder handoff 进入 Verify。

## File Structure

### 新建文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAuditCoordinator.java`
  - 固定 `external:retrieval` action；创建脱敏审计事件；统一审计失败策略。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalRequestBodyFilter.java`
  - 只匹配 configured retrieval POST；检查 `Content-Length`；用有界 `ServletInputStream` 包装请求；超限时审计并返回 400/1003。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAuditCoordinatorTest.java`
  - 审计事件字段和审计持久化失败策略。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalRequestBodyFilterTest.java`
  - body 字节限制、路径匹配、稳定响应和单次审计。

### 修改文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalProperties.java`
  - 增加四个正数限额属性。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalKnowledgeService.java`
  - 注入 Coordinator；增加 direct-call 认证审计、全业务路径审计和 Service 唯一语义限额。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalController.java`
  - 注入 Coordinator；记录委托前 HTTP 失败；保留现有 `@RequestBody String` 和错误顺序。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAutoConfiguration.java`
  - 条件装配 Coordinator、Service、Controller 和 Filter registration。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalKnowledgeServiceTest.java`
  - direct-call 限额、审计和 sink 故障回归。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalControllerTest.java`
  - HTTP 前置失败、委托后去重和脱敏审计回归。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAutoConfigurationTest.java`
  - Bean 条件、覆盖和自定义 basePath Filter 映射。

---

### Task 1: 外部请求限额配置

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalProperties.java:13-44`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAutoConfigurationTest.java`

**Interfaces:**
- Produces:
  - `int getMaxRequestBodyBytes()` / `void setMaxRequestBodyBytes(int)`，默认 `65536`；
  - `int getMaxQueryLength()` / `void setMaxQueryLength(int)`，默认 `4096`；
  - `int getMaxMetadataConditions()` / `void setMaxMetadataConditions(int)`，默认 `20`；
  - `int getMaxMetadataDepth()` / `void setMaxMetadataDepth(int)`，默认 `8`。

- [ ] **Step 1: 写默认值和非法值失败测试**

在 `TinyflowDifyExternalAutoConfigurationTest` 增加：

```java
@Test
public void shouldExposeSafeExternalRequestLimitDefaults() throws Exception {
    TinyflowDifyExternalProperties properties = new TinyflowDifyExternalProperties();
    assertEquals(65536, properties.getMaxRequestBodyBytes());
    assertEquals(4096, properties.getMaxQueryLength());
    assertEquals(20, properties.getMaxMetadataConditions());
    assertEquals(8, properties.getMaxMetadataDepth());
    properties.afterPropertiesSet();
}

@Test
public void shouldRejectNonPositiveExternalRequestLimits() {
    TinyflowDifyExternalProperties properties = new TinyflowDifyExternalProperties();
    properties.setMaxRequestBodyBytes(0);
    assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet);

    properties = new TinyflowDifyExternalProperties();
    properties.setMaxQueryLength(0);
    assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet);

    properties = new TinyflowDifyExternalProperties();
    properties.setMaxMetadataConditions(0);
    assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet);

    properties = new TinyflowDifyExternalProperties();
    properties.setMaxMetadataDepth(0);
    assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet);
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```bash
mvn -f fw-plus-ai/pom.xml -o \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -Dtest=TinyflowDifyExternalAutoConfigurationTest \
  -DfailIfNoTests=false test
```

Expected: test compile 失败，提示四组 getter/setter 不存在。

- [ ] **Step 3: 增加属性和启动校验**

在 `TinyflowDifyExternalProperties` 增加字段、getter/setter，并在 `afterPropertiesSet()` 增加：

```java
if (maxRequestBodyBytes <= 0 || maxQueryLength <= 0
        || maxMetadataConditions <= 0 || maxMetadataDepth <= 0) {
    throw new IllegalArgumentException("Tinyflow Dify external request limits must be positive");
}
```

- [ ] **Step 4: 重新运行测试确认 GREEN**

Run 同 Step 2。

Expected: `TinyflowDifyExternalAutoConfigurationTest` PASS。

- [ ] **Step 5: 评审 Task 1 差异**

检查默认值、正数校验和 Java 8 兼容性；不要提交，除非用户另行明确要求。

---

### Task 2: 审计协调器

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAuditCoordinator.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAuditCoordinatorTest.java`

**Interfaces:**
- Consumes: `MagicTinyflowKnowledgeAuditSink`。
- Produces:

```java
public final class TinyflowDifyExternalAuditCoordinator {
    public TinyflowDifyExternalAuditCoordinator(MagicTinyflowKnowledgeAuditSink auditSink);
    public void success(TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey, String knowledgeId);
    public void failure(TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey,
            String knowledgeId, RuntimeException error);
}
```

`failure(...)` 保留原业务异常；审计失败通过 `error.addSuppressed(auditFailure)` 附加。`success(...)` 审计失败抛出包级可识别的 `AuditPersistenceException`，供 Service 避免二次审计。

- [ ] **Step 1: 写事件字段和脱敏失败测试**

```java
@Test
public void shouldRecordOnlySafeExternalRetrievalFields() {
    List<MagicTinyflowKnowledgeAuditSink.AuditEvent> events = new ArrayList<>();
    TinyflowDifyExternalAuditCoordinator coordinator =
            new TinyflowDifyExternalAuditCoordinator(events::add);

    coordinator.success(accessKey("tenant-a", "kb-1"), "kb-1");
    RuntimeException failure = new IllegalStateException("query=secret");
    coordinator.failure(accessKey("tenant-a", "kb-1"), "kb-1", failure);

    assertEquals(2, events.size());
    assertEquals("external:retrieval", events.get(0).getAction());
    assertEquals("tenant-a", events.get(0).getTenantId());
    assertEquals("kb-1", events.get(0).getResourceId());
    assertTrue(events.get(0).isSuccess());
    assertFalse(events.get(1).isSuccess());
    assertEquals(IllegalStateException.class.getName(), events.get(1).getErrorType());
    assertFalse(events.get(1).toString().contains("query=secret"));
}
```

- [ ] **Step 2: 写审计持久化失败策略测试**

```java
@Test
public void shouldFailClosedForSuccessfulBusinessWhenAuditPersistenceFails() {
    TinyflowDifyExternalAuditCoordinator coordinator =
            new TinyflowDifyExternalAuditCoordinator(event -> {
                throw new IllegalStateException("audit unavailable");
            });

    assertThrows(TinyflowDifyExternalAuditCoordinator.AuditPersistenceException.class,
            () -> coordinator.success(accessKey("tenant-a", "kb-1"), "kb-1"));
}

@Test
public void shouldKeepBusinessFailureAndAttachAuditFailure() {
    TinyflowDifyExternalAuditCoordinator coordinator =
            new TinyflowDifyExternalAuditCoordinator(event -> {
                throw new IllegalStateException("audit unavailable");
            });
    RuntimeException business = new IllegalArgumentException("invalid request");

    coordinator.failure(accessKey("tenant-a", "kb-1"), "kb-1", business);

    assertEquals(1, business.getSuppressed().length);
    assertEquals("audit unavailable", business.getSuppressed()[0].getMessage());
}
```

- [ ] **Step 3: 运行测试确认 RED**

Run:

```bash
mvn -f fw-plus-ai/pom.xml -o \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -Dtest=TinyflowDifyExternalAuditCoordinatorTest \
  -DfailIfNoTests=false test
```

Expected: test compile 失败，Coordinator 类型不存在。

- [ ] **Step 4: 实现最小 Coordinator**

核心实现必须等价于：

```java
public void success(AccessKey accessKey, String knowledgeId) {
    record(accessKey, knowledgeId, true, null);
}

public void failure(AccessKey accessKey, String knowledgeId, RuntimeException error) {
    if (error == null) throw new IllegalArgumentException("External retrieval error must not be null");
    try {
        record(accessKey, knowledgeId, false, error);
    } catch (RuntimeException auditFailure) {
        if (auditFailure != error) error.addSuppressed(auditFailure);
    }
}

private void record(AccessKey accessKey, String knowledgeId,
        boolean success, RuntimeException error) {
    try {
        auditSink.record(new AuditEvent(accessKey == null ? null : accessKey.getTenantId(),
                "external:retrieval", knowledgeId, success,
                error == null ? null : error.getClass().getName()));
    } catch (RuntimeException auditFailure) {
        if (error == null) throw new AuditPersistenceException(auditFailure);
        throw auditFailure;
    }
}
```

构造器拒绝 null sink。`AuditPersistenceException` 不携带请求数据，只保留 cause。

- [ ] **Step 5: 运行 Coordinator 测试确认 GREEN**

Run 同 Step 3。

Expected: 3 tests PASS。

- [ ] **Step 6: 评审 Task 2 差异**

确认 Coordinator 不接受 token、header、query 或 body 参数；不要提交，除非用户明确要求。

---

### Task 3: MVC 前原始 body 限额 Filter

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalRequestBodyFilter.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalRequestBodyFilterTest.java`

**Interfaces:**
- Consumes:
  - `TinyflowDifyExternalProperties#getBasePath()`；
  - `TinyflowDifyExternalProperties#getMaxRequestBodyBytes()`；
  - `TinyflowDifyExternalAuditCoordinator#failure(...)`。
- Produces:

```java
public final class TinyflowDifyExternalRequestBodyFilter extends OncePerRequestFilter {
    public TinyflowDifyExternalRequestBodyFilter(
            TinyflowDifyExternalProperties properties,
            TinyflowDifyExternalAuditCoordinator audit);
}
```

- [ ] **Step 1: 写已知 Content-Length 超限和路径透传失败测试**

使用 `MockHttpServletRequest`、`MockHttpServletResponse` 和记录型 `FilterChain`：

```java
@Test
public void shouldRejectKnownOversizedRetrievalBeforeFilterChain() throws Exception {
    properties.setMaxRequestBodyBytes(8);
    MockHttpServletRequest request = request("POST", "/external-knowledge/retrieval", "123456789");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicInteger calls = new AtomicInteger();

    filter.doFilter(request, response, (req, res) -> calls.incrementAndGet());

    assertEquals(400, response.getStatus());
    assertEquals(0, calls.get());
    assertTrue(response.getContentAsString().contains("\"error_code\":1003"));
    assertEquals(1, audits.size());
}

@Test
public void shouldIgnoreNonTargetRequest() throws Exception {
    MockHttpServletRequest request = request("POST", "/other", repeat("x", 100));
    AtomicInteger calls = new AtomicInteger();
    filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> calls.incrementAndGet());
    assertEquals(1, calls.get());
    assertEquals(0, audits.size());
}
```

- [ ] **Step 2: 写未知长度流、边界和 UTF-8 字节失败测试**

创建覆盖 `getContentLengthLong()` 为 `-1` 的 `HttpServletRequestWrapper`：

```java
@Test
public void shouldStopUnknownLengthBodyAtLimitPlusOneByte() throws Exception {
    properties.setMaxRequestBodyBytes(8);
    HttpServletRequest request = unknownLength(request(
            "POST", "/external-knowledge/retrieval", "123456789"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> drain(req.getInputStream()));

    assertEquals(400, response.getStatus());
    assertEquals(1, audits.size());
}

@Test
public void shouldAllowBodyExactlyAtByteLimit() throws Exception {
    properties.setMaxRequestBodyBytes(8);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicInteger calls = new AtomicInteger();

    filter.doFilter(request("POST", "/external-knowledge/retrieval", "12345678"), response,
            (req, res) -> { drain(req.getInputStream()); calls.incrementAndGet(); });

    assertEquals(1, calls.get());
    assertEquals(0, audits.size());
}

@Test
public void shouldCountUtf8BytesInsteadOfCharacters() throws Exception {
    properties.setMaxRequestBodyBytes(5);
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request("POST", "/external-knowledge/retrieval", "中文"), response,
            (req, res) -> drain(req.getInputStream()));
    assertEquals(400, response.getStatus());
}
```

- [ ] **Step 3: 运行 Filter 测试确认 RED**

Run:

```bash
mvn -f fw-plus-ai/pom.xml -o \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -Dtest=TinyflowDifyExternalRequestBodyFilterTest \
  -DfailIfNoTests=false test
```

Expected: test compile 失败，Filter 类型不存在。

- [ ] **Step 4: 实现路径匹配和已知长度短路**

`shouldNotFilter` 只在以下条件全部满足时返回 false：

```java
"POST".equalsIgnoreCase(request.getMethod())
        && (request.getContextPath() + properties.getBasePath() + "/retrieval")
                .equals(request.getRequestURI())
```

`doFilterInternal` 在 `contentLength > maxBytes` 时调用统一 `reject(...)`，不进入 chain。

- [ ] **Step 5: 实现有界 ServletInputStream 包装器**

使用 `HttpServletRequestWrapper` 和代理 `ServletInputStream`。每次 `read()`/`read(byte[],off,len)` 累加实际返回字节数；达到 `maxBytes + 1` 时抛包级 `RequestBodyLimitException`。代理必须转发 `isFinished()`、`isReady()` 和 `setReadListener()`。

Filter 包围 `chain.doFilter(wrapped, response)`，捕获 `RequestBodyLimitException` 后调用 `reject(...)`。

`reject(...)`：

```java
RuntimeException error = new RequestBodyLimitException();
audit.failure(null, null, error);
response.setStatus(HttpStatus.BAD_REQUEST.value());
response.setContentType(MediaType.APPLICATION_JSON_VALUE);
response.setCharacterEncoding(StandardCharsets.UTF_8.name());
response.getWriter().write("{\"error_code\":1003,\"error_msg\":\"Invalid request\"}");
```

确保响应已提交时不二次写入。

- [ ] **Step 6: 运行 Filter 测试确认 GREEN**

Run 同 Step 3。

Expected: Filter tests 全部 PASS。

- [ ] **Step 7: 评审 Task 3 差异**

确认 Filter 不缓存 body、按字节计数、非目标请求不受影响、超限审计一次；不要提交。

---

### Task 4: Service 唯一语义限额与 direct-call 审计

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalKnowledgeService.java:42-318`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalKnowledgeServiceTest.java`

**Interfaces:**
- Consumes:
  - `TinyflowDifyExternalAuditCoordinator#success(...)`；
  - `TinyflowDifyExternalAuditCoordinator#failure(...)`；
  - Task 1 的 Properties 限额。
- Produces:
  - 兼容原四参数 public 构造器；
  - 新 public 构造器增加 Coordinator；
  - 包级测试构造器增加 Coordinator、executor、rateWindowCapacity 和 nanoTime；
  - `retrieval(String, KnowledgeArgs)` 和 `retrieval(AccessKey, KnowledgeArgs)` 均完整审计。

- [ ] **Step 1: 写 direct Service 限额 RED 测试**

增加：

```java
@Test
public void shouldRejectDirectCallsThatExceedQueryAndMetadataLimits() {
    properties.setMaxQueryLength(8);
    properties.setMaxMetadataConditions(1);
    properties.setMaxMetadataDepth(2);
    TinyflowDifyExternalKnowledgeService service = serviceWithAudit(provider, repository, properties);

    KnowledgeArgs longQuery = request();
    longQuery.setQuery("123456789");
    assertInvalidRequest(service, longQuery);

    KnowledgeArgs tooMany = requestWithConditions(
            condition("is", "first"), condition("is", "second"));
    assertInvalidRequest(service, tooMany);

    KnowledgeArgs tooDeep = requestWithConditions(condition("in",
            Collections.singletonList(Collections.singletonList(
                    Collections.singletonList("value")))));
    assertInvalidRequest(service, tooDeep);

    assertEquals(0, provider.calls);
}
```

另写边界合法测试：query 长度恰为 8、一个条件、深度恰为 2 时 Provider 调用一次。

- [ ] **Step 2: 写 direct Service 审计 RED 测试**

覆盖：

```java
@Test
public void shouldAuditInvalidKeyNullRequestSuccessAndBusinessFailureExactlyOnce() {
    // invalid api key: one failure event, null tenant
    // authenticated args == null: one failure event, null resourceId
    // valid retrieval: one success event
    // wrong scope: one failure event
    // 每次调用前清空 events，并断言 size == 1
}
```

增加 sink 故障测试：

```java
@Test
public void shouldNotAuditTwiceOrReplaceBusinessFailureWhenSinkFails() {
    // Coordinator 使用抛异常 sink
    // wrong scope 应仍抛 ExternalKnowledgeException 401/1002
    // 原异常带一个 suppressed audit failure
}
```

- [ ] **Step 3: 运行 Service 测试确认 RED**

Run:

```bash
mvn -f fw-plus-ai/pom.xml -o \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -Dtest=TinyflowDifyExternalKnowledgeServiceTest \
  -DfailIfNoTests=false test
```

Expected: 限额测试未抛异常，且新构造器/审计断言失败。

- [ ] **Step 4: 注入 Coordinator 并包围 direct authentication**

保留旧 public 构造器，通过默认 no-op 不可用；根据本设计，自动配置只创建带 Coordinator 的构造器。为源码兼容，旧构造器可创建一个明确的 `TinyflowDifyExternalAuditCoordinator`，其 sink 为 no-op，仅供非 HTTP 手工构造场景；自动配置不得使用旧构造器。

`retrieval(String apiKey, KnowledgeArgs args)`：

```java
TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey;
try {
    accessKey = authenticateAccessKey(apiKey);
} catch (RuntimeException error) {
    audit.failure(null, knowledgeId(args), error);
    throw error;
}
return retrieval(accessKey, args);
```

- [ ] **Step 5: 包围完整业务 retrieval 且避免二次审计**

```java
try {
    KnowledgeResult result = retrieveInternal(accessKey, args);
    audit.success(accessKey, knowledgeId(args));
    return result;
} catch (RuntimeException error) {
    if (!(error instanceof TinyflowDifyExternalAuditCoordinator.AuditPersistenceException)) {
        audit.failure(accessKey, knowledgeId(args), error);
    }
    throw error;
}
```

把现有方法体抽到 `retrieveInternal(...)`，业务顺序不变。

- [ ] **Step 6: 在 `validate(...)` 增加唯一语义限额**

在 Provider 查找前执行：

```java
if (query == null || query.length() > properties.getMaxQueryLength() || ...) {
    throw error(400, 1003, "Invalid request");
}
if (metadata.getConditions() != null
        && metadata.getConditions().size() > properties.getMaxMetadataConditions()) {
    throw error(400, 1003, "Invalid request");
}
```

`validateMetadataValue(...)` 第一行：

```java
if (metadataDepth(value, 0) > properties.getMaxMetadataDepth()) {
    throw error(400, 1003, "Invalid request");
}
```

实现 `metadataDepth` 对 `Map.values()`、`Collection` 和数组递归，超过限制立即返回；非容器返回当前深度。

- [ ] **Step 7: 运行 Service 测试确认 GREEN**

Run 同 Step 3。

Expected: 全部 Service tests PASS，既有并发、超时、关闭和 metadata 运算符测试不回归。

- [ ] **Step 8: 评审 Task 4 差异**

确认 semantic rules 只在 Service；认证失败和 `args == null` 仍审计；成功审计失败不进入失败审计；不要提交。

---

### Task 5: Controller 委托前审计和去重

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalController.java:27-190`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalControllerTest.java`

**Interfaces:**
- Consumes: `TinyflowDifyExternalAuditCoordinator#failure(...)`。
- Produces:

```java
public TinyflowDifyExternalController(
        TinyflowDifyExternalKnowledgeService service,
        TinyflowDifyExternalAuditCoordinator audit);
```

保留旧单参数构造器仅作源码兼容和测试迁移，内部使用 no-op Coordinator；自动配置必须使用双参数构造器。

- [ ] **Step 1: 写 HTTP 前置失败和委托后去重 RED 测试**

在测试 setup 使用 `List<AuditEvent>` + real Coordinator。

覆盖并逐次清空 events：

```java
@Test
public void shouldAuditHttpPreflightFailuresAndDelegatedResultsExactlyOnce() throws Exception {
    assertError(null, 403, 1001);
    assertSingleFailure(null, null);

    assertError("Bearer invalid", 401, 1002);
    assertSingleFailure(null, null);

    String wrongScope = accessKeyService.create("tenant-a", "kb-other").getToken();
    assertError("Bearer " + wrongScope, 401, 1002);
    assertSingleFailure("tenant-a", "kb-1");

    mockMvc.perform(validPost()).andExpect(status().isOk());
    assertSingleSuccess("tenant-a", "kb-1");

    properties.setRequestTimeout(Duration.ofMillis(25));
    provider.block = true;
    rebuildService();
    assertBodyError(validRequest(), 504, 3002);
    assertEquals(1, events.size());
}
```

另断言所有事件 `toString()` 不含 token、`Bearer`、`question` 和 `sensitive cause`。

- [ ] **Step 2: 运行 Controller 测试确认 RED**

Run:

```bash
mvn -f fw-plus-ai/pom.xml -o \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -Dtest=TinyflowDifyExternalControllerTest \
  -DfailIfNoTests=false test
```

Expected: 双参数构造器不存在或事件数量为 0。

- [ ] **Step 3: 实现 Controller 审计状态边界**

`retrieval` 局部状态：

```java
AccessKey accessKey = null;
String knowledgeId = null;
boolean delegated = false;
```

流程：

1. `bearerToken`；
2. `authenticateAccessKey`；
3. `extractKnowledgeId`；
4. 若有 ID，`authorizeScope`；
5. `KnowledgeArgs args = parse(body)`；
6. `delegated = true`；
7. `service.retrieval(accessKey, args)`。

catch 行为：

```java
catch (HeaderException error) {
    audit.failure(accessKey, knowledgeId, error);
    return error(403, 1001, error.getMessage());
} catch (ExternalKnowledgeException error) {
    if (!delegated) audit.failure(accessKey, knowledgeId, error);
    return error(error.getStatus(), error.getCode(), error.getMessage());
} catch (RuntimeException error) {
    if (!delegated) audit.failure(accessKey, knowledgeId, error);
    return error(500, 5000, "Internal server error");
}
```

Controller 不增加 body/query/metadata limit 校验。

- [ ] **Step 4: 运行 Controller 测试确认 GREEN**

Run 同 Step 2。

Expected: Controller tests PASS，现有认证/scope/malformed 顺序测试保持通过。

- [ ] **Step 5: 评审 Task 5 差异**

确认 delegated 只在实际调用 Service 前变为 true；前置失败一次、委托后结果由 Service 一次；不要提交。

---

### Task 6: 自动配置装配和 Filter 注册

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAutoConfiguration.java:1-65`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAutoConfigurationTest.java`

**Interfaces:**
- Consumes: Tasks 2-5 的 Coordinator、Filter、Service 和 Controller 构造器。
- Produces: 默认 Bean 图和 `FilterRegistrationBean<TinyflowDifyExternalRequestBodyFilter>`。

- [ ] **Step 1: 写 Bean 条件和覆盖 RED 测试**

用 `ApplicationContextRunner` 覆盖：

```java
@Test
public void shouldCreateAuditedExternalEndpointAndFilterWhenAllDependenciesExist() {
    contextRunner.withBean(MagicTinyflowKnowledgeAuditSink.class, () -> event -> {})
        .withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true")
        .run(context -> {
            assertThat(context).hasSingleBean(TinyflowDifyExternalAuditCoordinator.class);
            assertThat(context).hasSingleBean(TinyflowDifyExternalKnowledgeService.class);
            assertThat(context).hasSingleBean(TinyflowDifyExternalController.class);
            assertThat(context).hasSingleBean(TinyflowDifyExternalRequestBodyFilter.class);
        });
}

@Test
public void shouldNotPublishExternalEndpointWithoutAuditSink() {
    contextRunner.withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true")
        .run(context -> {
            assertThat(context).doesNotHaveBean(TinyflowDifyExternalKnowledgeService.class);
            assertThat(context).doesNotHaveBean(TinyflowDifyExternalController.class);
            assertThat(context).doesNotHaveBean(TinyflowDifyExternalRequestBodyFilter.class);
        });
}
```

增加自定义 Coordinator/Service/Filter 覆盖测试，以及 `base-path=/custom-external` 时 registration URL 为 `/custom-external/retrieval`。

- [ ] **Step 2: 运行自动配置测试确认 RED**

Run:

```bash
mvn -f fw-plus-ai/pom.xml -o \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -Dtest=TinyflowDifyExternalAutoConfigurationTest \
  -DfailIfNoTests=false test
```

Expected: Coordinator/Filter Bean 不存在，或无 sink 时 Service 仍被错误创建。

- [ ] **Step 3: 装配 Coordinator、Service 和 Controller**

增加：

```java
@Bean
@ConditionalOnMissingBean
@ConditionalOnBean(MagicTinyflowKnowledgeAuditSink.class)
public TinyflowDifyExternalAuditCoordinator tinyflowDifyExternalAuditCoordinator(
        MagicTinyflowKnowledgeAuditSink auditSink) {
    return new TinyflowDifyExternalAuditCoordinator(auditSink);
}
```

Service Bean 加 `@ConditionalOnBean({FwPlusLocalKnowledgeProvider.class, TinyflowDifyExternalAuditCoordinator.class})`，并使用带 Coordinator 构造器。Controller 使用双参数构造器。

- [ ] **Step 4: 注册 Filter**

推荐同时创建 Filter 本体与 registration：

```java
@Bean
@ConditionalOnMissingBean
public TinyflowDifyExternalRequestBodyFilter tinyflowDifyExternalRequestBodyFilter(
        TinyflowDifyExternalProperties properties,
        TinyflowDifyExternalAuditCoordinator audit) {
    return new TinyflowDifyExternalRequestBodyFilter(properties, audit);
}

@Bean
@ConditionalOnMissingBean(name = "tinyflowDifyExternalRequestBodyFilterRegistration")
public FilterRegistrationBean<TinyflowDifyExternalRequestBodyFilter>
        tinyflowDifyExternalRequestBodyFilterRegistration(
                TinyflowDifyExternalRequestBodyFilter filter,
                TinyflowDifyExternalProperties properties) {
    FilterRegistrationBean<TinyflowDifyExternalRequestBodyFilter> registration =
            new FilterRegistrationBean<TinyflowDifyExternalRequestBodyFilter>();
    registration.setFilter(filter);
    registration.addUrlPatterns(properties.getBasePath() + "/retrieval");
    registration.setName("tinyflowDifyExternalRequestBodyFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
}
```

- [ ] **Step 5: 运行自动配置测试确认 GREEN**

Run 同 Step 2。

Expected: 自动配置 tests PASS。

- [ ] **Step 6: 运行外部 Dify 全套测试**

```bash
mvn -f fw-plus-ai/pom.xml -o \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -Dtest=TinyflowDifyExternalAuditCoordinatorTest,TinyflowDifyExternalRequestBodyFilterTest,TinyflowDifyExternalKnowledgeServiceTest,TinyflowDifyExternalControllerTest,TinyflowDifyExternalAutoConfigurationTest \
  -DfailIfNoTests=false \
  -Djava.awt.headless=true -DargLine=-Djava.awt.headless=true test
```

Expected: 全部 PASS。

- [ ] **Step 7: 评审 Task 6 差异**

确认缺少审计 sink 时不发布 external endpoint；自定义 Bean 可覆盖；Filter 路径跟随 basePath；不要提交。

---

### Task 7: 全量定向回归、代码评审和 Comet Builder handoff

**Files:**
- Verify all files listed in File Structure。
- Do not modify formal brief/spec unless实现发现用户可见需求变化；若变化，必须先返回 Shape。

**Interfaces:**
- Consumes: Tasks 1-6 全部实现和测试。
- Produces: 可由 Native Runtime 验收的 Builder candidate。

- [ ] **Step 1: 运行完整 AI Boot 定向回归**

```bash
mvn -f fw-plus-ai/pom.xml -o \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -Dtest=MagicTinyflowControllerTest,TinyflowMagicResourceStorageTest,TinyflowResourceDynamicRegistryTest,TinyflowAutoConfigurationTest,TinyflowDifyExternalAuditCoordinatorTest,TinyflowDifyExternalRequestBodyFilterTest,TinyflowDifyExternalControllerTest,TinyflowDifyExternalKnowledgeServiceTest,TinyflowDifyExternalAutoConfigurationTest \
  -DfailIfNoTests=false \
  -Djava.awt.headless=true -DargLine=-Djava.awt.headless=true test
```

Expected: BUILD SUCCESS，0 failures，0 errors。

- [ ] **Step 2: 运行差异检查**

```bash
git diff --check
```

Expected: 无输出，exit 0。

- [ ] **Step 3: 核对 A37/A38 场景矩阵**

逐项确认：

- body 已知长度超限；
- body 未知长度超限；
- UTF-8 字节边界；
- query 超长；
- metadata 条件数超限；
- metadata 深度超限；
- direct Service 不可绕过；
- HTTP success/header/key/scope/malformed/body-limit；
- Service null request/not-found/timeout/internal；
- 每项恰好一次审计；
- 事件不含凭证、query、body 或异常消息；
- audit sink 故障不重复审计、不覆盖已有业务失败。

任一缺口都回到对应 Task 的 RED-GREEN 循环，不提交 handoff。

- [ ] **Step 4: 请求独立代码评审**

调用 `requesting-code-review`，重点检查：

- Filter 是否真正在 MVC 前限制实际字节；
- Filter 异常是否能稳定返回 400/1003；
- Controller/Service 是否重复或遗漏审计；
- direct Service 是否绕过限额；
- audit sink 故障语义；
- Java 8/Spring Boot 2.7 兼容性。

修复所有 Critical/Important 后重新执行 Step 1-3。

- [ ] **Step 5: 提交 Native Builder handoff**

从最新 `comet native status tinyflow-magicapi-knowledge-base --details --json` 读取 continuation。创建临时 JSON：

```json
{
  "kind": "builder-handoff",
  "summary": "A37/A38 重构完成：MVC 前 Filter 按原始字节限制 external retrieval body；Service 统一 query/metadata 语义限额；Coordinator 统一 HTTP/direct Service 单次脱敏审计及审计失败策略。",
  "addressed_acceptance_ids": ["A37", "A38"],
  "checks": [
    {
      "name": "Tinyflow AI 自动配置定向回归",
      "result": "passed",
      "note": "填写实际测试数和结果"
    },
    {
      "name": "Git diff 空白检查",
      "result": "passed",
      "note": "git diff --check exit 0"
    }
  ],
  "known_limits": [
    "真实 Dify、真实浏览器和最终部署 JAR E2E 仍按已确认范围作为后续独立任务。"
  ]
}
```

按 Runtime `continuation.commandArgs` 执行一次，并删除临时文件。

- [ ] **Step 6: 进入 Verify 并仅执行最新 continuation**

Runtime 要求 `dispatch-verifier` 时，将最新定向回归和 `git diff --check` 填入 check plan。Runtime 检查通过后启动新的只读 Verifier，逐项覆盖当前 85 项验收，重点独立验收 A37/A38。不得复用此前已失效候选的结论。

- [ ] **Step 7: 不自动确认 Archive**

若 Runtime 返回 `await-user` 的 `confirm-skill-coordinated-pass`，向用户报告实际通过数、检查结果和剩余风险，等待用户明确确认后才执行 `--confirmed`。

## Plan Self-Review

- Spec coverage：Filter、Service 语义限额、Coordinator、Controller/Service 审计分工、自动配置、错误契约、测试矩阵和 Native handoff 均有对应 Task。
- Placeholder scan：计划无 `TBD`、`TODO` 或未定义类型；handoff 中“填写实际测试数”是执行时必须替换的运行结果字段，不是实现占位符。
- Type consistency：Coordinator、Filter、Properties getter、Service/Controller 构造器和自动配置 Bean 名称在各 Task 中一致。
- Scope：不包含 Core 审计迁移、异步审计或外部 E2E，符合已确认设计。
