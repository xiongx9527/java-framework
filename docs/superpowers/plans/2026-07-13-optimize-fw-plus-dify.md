---
change: optimize-fw-plus-dify
design-doc: docs/superpowers/specs/2026-07-13-optimize-fw-plus-dify-design.md
base-ref: 8e5124fb778f7599e24800bad04f7b646afd73c1
---

# optimize-fw-plus-dify Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保持现有 Java public API 与 Magic-API `dify` 模块入口兼容的前提下，规范并优化 `fw-plus-dify` 的客户端构建、URL 拼接、异常诊断和基础响应解析行为。

**Architecture:** 采用“兼容 API + 内部基础设施 helper”方案：新增内部工具类集中处理 URL 规范化、参数校验和异常信息构建，再把这些 helper 接入现有 `AppClientBuilder`、`DatasetBuilder`、`WebClientBuilder` 与 `SimpleHttpClient`。不新增顶层 facade，不重写 SDK，不改变 `DifyModule` 现有方法名、参数顺序和返回类型语义。

**Tech Stack:** Java 1.8、Maven Wrapper、Spring Boot 2.7.18 依赖体系、OkHttp、Fastjson2、Lombok、JUnit 单元测试。

## Global Constraints

- 产物语言使用 `zh-CN`。
- 主要代码范围限定在 `fw-plus-core/fw-plus-project/fw-plus-dify/`。
- Boot 层 `DifyModule` 只允许兼容性审查；除非新增兼容重载，否则不修改现有入口。
- 不修改 Product 层业务代码或真实 Magic-API 业务脚本。
- 不自动追加 `/v1` 到 server；调用方已有基础路径必须保留。
- 不泄露完整 `apiKey` 或 `password` 到异常消息、测试断言或文档示例。
- timeout、连接池、retry 本次采用最小处理：明确默认值和边界，不强制完整打通所有配置链路。
- 每完成一个 Comet task 验收后，定向勾选 `openspec/changes/optimize-fw-plus-dify/tasks.md` 中对应任务并提交。

---

## File Structure

### 新增文件

- `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtils.java`
  - 内部 URL helper。负责 server 规范化、server + route 拼接、query 参数 UTF-8 编码。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DifyValidationUtils.java`
  - 内部参数校验 helper。负责校验 server、apiKey、userName、password，并生成带上下文的 `DifyClientException`。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtilsTest.java`
  - URL 规范化和 query 编码测试。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyValidationUtilsTest.java`
  - 必填参数校验测试。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyBuilderValidationTest.java`
  - app、dataset、web console 构建器校验测试。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/types/DifyExceptionTest.java`
  - Dify JSON 错误、非 JSON 错误、空响应体的诊断信息测试。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/types/DifyPageTest.java`
  - 分页响应字段映射和未知字段容忍测试。

### 修改文件

- `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/SimpleHttpClient.java`
  - 用 `DifyUrlUtils.buildUrl(server, url, query)` 替换私有 `buildUrl` 内部逻辑。
  - 网络异常包装为带上下文的 `DifyClientException`，保留 cause。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/AppClientBuilder.java`
  - 使用 `DifyValidationUtils` 校验 `baseUrl` 和 `apiKey`。
  - 构建失败时抛出 `DifyClientException` 并保留 cause。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DatasetBuilder.java`
  - 使用 `DifyValidationUtils` 校验 `baseUrl` 和 `apiKey`。
  - `of(...)` 与 `ofDocument(...)` 也使用规范化 server 构建 `ApiConfig`。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/WebClientBuilder.java`
  - 使用 `DifyValidationUtils` 校验 `server`、`userName` 和 `password`。
  - 保留 `DifyClientException | DifyException` 透传，其他异常包装为 `DifyClientException`。
- `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/types/DifyException.java`
  - 增加 HTTP status 与 Dify status 的可访问字段或 getter。
  - 空响应体、非 JSON 响应体均给出稳定 message。
- `openspec/changes/optimize-fw-plus-dify/tasks.md`
  - 每个 Comet task 完成验收后勾选对应条目。

### 审查但默认不改

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dify/DifyModule.java`
  - 确认以下入口保持兼容：`web`、`dataset`、`baseApp`、`chatApp`、`flowApp`、`completionApp`。

---

### Task 1: 梳理兼容基线并新增 URL helper

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtils.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtilsTest.java`
- Modify: `openspec/changes/optimize-fw-plus-dify/tasks.md:3-5`

**Interfaces:**
- Consumes: 现有 `SimpleHttpClient` 对 server、route、query 的拼接需求。
- Produces:
  - `public final class DifyUrlUtils`
  - `public static String normalizeServer(String server)`
  - `public static String buildUrl(String server, String route, Map<String, Object> query)`

- [x] **Step 1: 写 URL helper 的失败测试**

创建 `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtilsTest.java`：

```java
package com.xiongx9527.fw.plus.dify.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DifyUrlUtilsTest {

    @Test
    void buildUrlShouldKeepSingleSlashBetweenServerAndRoute() {
        assertEquals(
                "https://api.dify.ai/v1/datasets",
                DifyUrlUtils.buildUrl("https://api.dify.ai", "/v1/datasets", null)
        );
        assertEquals(
                "https://api.dify.ai/v1/datasets",
                DifyUrlUtils.buildUrl("https://api.dify.ai/", "v1/datasets", null)
        );
    }

    @Test
    void buildUrlShouldKeepExistingBasePath() {
        assertEquals(
                "https://proxy.example.com/dify/v1/chat-messages",
                DifyUrlUtils.buildUrl("https://proxy.example.com/dify/", "/v1/chat-messages", null)
        );
    }

    @Test
    void buildUrlShouldEncodeQueryWithUtf8() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("keyword", "中文 空格");
        query.put("tag", "a&b");
        query.put("ignored", null);

        assertEquals(
                "https://api.dify.ai/v1/datasets?keyword=%E4%B8%AD%E6%96%87+%E7%A9%BA%E6%A0%BC&tag=a%26b",
                DifyUrlUtils.buildUrl("https://api.dify.ai", "/v1/datasets", query)
        );
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyUrlUtilsTest test
```

Expected: FAIL，错误包含 `cannot find symbol` 或 `DifyUrlUtils` 未定义。

- [x] **Step 3: 实现 URL helper**

创建 `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtils.java`：

```java
package com.xiongx9527.fw.plus.dify.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Dify URL 工具。
 */
public final class DifyUrlUtils {

    private static final String UTF_8 = "UTF-8";

    private DifyUrlUtils() {
    }

    public static String normalizeServer(String server) {
        if (server == null) {
            return null;
        }
        String normalized = server.trim();
        while (normalized.endsWith("/") && normalized.length() > "https://".length()) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static String buildUrl(String server, String route, Map<String, Object> query) {
        String normalizedServer = normalizeServer(server);
        String normalizedRoute = route == null ? "" : route.trim();
        StringBuilder sb = new StringBuilder();
        sb.append(normalizedServer);

        if (normalizedRoute.length() > 0) {
            if (!normalizedRoute.startsWith("/")) {
                sb.append('/');
            }
            sb.append(normalizedRoute);
        }

        appendQuery(sb, query);
        return sb.toString();
    }

    private static void appendQuery(StringBuilder sb, Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return;
        }

        boolean first = true;
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            sb.append(first ? '?' : '&');
            sb.append(encode(entry.getKey()));
            sb.append('=');
            sb.append(encode(String.valueOf(entry.getValue())));
            first = false;
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported", e);
        }
    }
}
```

- [x] **Step 4: 运行 URL helper 测试确认通过**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyUrlUtilsTest test
```

Expected: PASS。

- [x] **Step 5: 定向勾选 OpenSpec 任务 1.1、1.2、1.3 的兼容基线部分**

只在本任务完成代码审查或自检后，把 `openspec/changes/optimize-fw-plus-dify/tasks.md` 中以下三行勾选：

```markdown
- [x] 1.1 梳理 `fw-plus-dify` 的 app、dataset、web console 客户端构建链路和现有 public API。
- [x] 1.2 梳理 Boot 层 Magic-API `DifyModule` 入口，确认需要保持兼容的方法名、参数顺序和返回类型语义。
- [x] 1.3 明确本次实现不覆盖的 Dify API 范围，避免扩展为全量 SDK 重写。
```

- [x] **Step 6: 提交 Task 1**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtils.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtilsTest.java \
  openspec/changes/optimize-fw-plus-dify/tasks.md
git commit -m "feat(dify): add URL normalization helper"
```

---

### Task 2: 新增参数校验 helper 并接入构建器

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DifyValidationUtils.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyValidationUtilsTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyBuilderValidationTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/AppClientBuilder.java:1-139`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DatasetBuilder.java:1-109`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/WebClientBuilder.java:1-103`
- Modify: `openspec/changes/optimize-fw-plus-dify/tasks.md:9`

**Interfaces:**
- Consumes: `DifyClientException`。
- Produces:
  - `public final class DifyValidationUtils`
  - `public static String requireText(String value, String fieldName, String clientType, String phase)`
  - `public static String normalizeServer(String server, String clientType, String phase)`

- [x] **Step 1: 写参数校验 helper 的失败测试**

创建 `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyValidationUtilsTest.java`：

```java
package com.xiongx9527.fw.plus.dify.utils;

import com.xiongx9527.fw.plus.dify.types.DifyClientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifyValidationUtilsTest {

    @Test
    void requireTextShouldRejectBlankValueWithContext() {
        DifyClientException exception = assertThrows(DifyClientException.class, () ->
                DifyValidationUtils.requireText("  ", "apiKey", "app", "build")
        );

        assertTrue(exception.getMessage().contains("app"));
        assertTrue(exception.getMessage().contains("build"));
        assertTrue(exception.getMessage().contains("apiKey"));
    }

    @Test
    void requireTextShouldReturnTrimmedValue() {
        assertEquals("https://api.dify.ai", DifyValidationUtils.requireText(
                " https://api.dify.ai ", "server", "dataset", "build"
        ));
    }
}
```

- [x] **Step 2: 写构建器参数校验失败测试**

创建 `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyBuilderValidationTest.java`：

```java
package com.xiongx9527.fw.plus.dify.utils;

import com.xiongx9527.fw.plus.dify.types.DifyClientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifyBuilderValidationTest {

    @Test
    void appBuilderShouldRejectBlankServer() {
        DifyClientException exception = assertThrows(DifyClientException.class, () ->
                AppClientBuilder.builder(" ", "app-key").chat().build()
        );

        assertTrue(exception.getMessage().contains("app"));
        assertTrue(exception.getMessage().contains("server"));
    }

    @Test
    void datasetBuilderShouldRejectBlankApiKey() {
        DifyClientException exception = assertThrows(DifyClientException.class, () ->
                DatasetBuilder.builder("https://api.dify.ai", " ").build()
        );

        assertTrue(exception.getMessage().contains("dataset"));
        assertTrue(exception.getMessage().contains("apiKey"));
    }

    @Test
    void webBuilderShouldRejectBlankPassword() {
        DifyClientException exception = assertThrows(DifyClientException.class, () ->
                WebClientBuilder.builder("https://api.dify.ai", "user@example.com", " ").connect()
        );

        assertTrue(exception.getMessage().contains("web"));
        assertTrue(exception.getMessage().contains("password"));
    }
}
```

- [x] **Step 3: 运行测试确认失败**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyValidationUtilsTest,DifyBuilderValidationTest test
```

Expected: FAIL，错误包含 `DifyValidationUtils` 未定义或构建器仍抛出泛化 `RuntimeException`。

- [x] **Step 4: 实现参数校验 helper**

创建 `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DifyValidationUtils.java`：

```java
package com.xiongx9527.fw.plus.dify.utils;

import com.xiongx9527.fw.plus.dify.types.DifyClientException;

/**
 * Dify 参数校验工具。
 */
public final class DifyValidationUtils {

    private DifyValidationUtils() {
    }

    public static String requireText(String value, String fieldName, String clientType, String phase) {
        if (value == null || value.trim().length() == 0) {
            throw new DifyClientException("Dify " + clientType + " client " + phase
                    + " error: required field '" + fieldName + "' is blank");
        }
        return value.trim();
    }

    public static String normalizeServer(String server, String clientType, String phase) {
        String required = requireText(server, "server", clientType, phase);
        return DifyUrlUtils.normalizeServer(required);
    }
}
```

- [x] **Step 5: 接入 `AppClientBuilder`**

在 `AppClientBuilder.Builder.build()` 中替换参数校验和异常包装。目标方法内容：

```java
public R build() {
    String normalizedBaseUrl = DifyValidationUtils.normalizeServer(baseUrl, "app", "build");
    String requiredApiKey = DifyValidationUtils.requireText(apiKey, "apiKey", "app", "build");

    try {
        return type.getConstructor(String.class, String.class).newInstance(normalizedBaseUrl, requiredApiKey);
    } catch (Exception e) {
        throw new DifyClientException("Dify app client build error: failed to create " + type.getSimpleName(), e);
    }
}
```

同时在 imports 中加入：

```java
import com.xiongx9527.fw.plus.dify.types.DifyClientException;
```

- [x] **Step 6: 接入 `DatasetBuilder`**

在 `DatasetBuilder.build()` 中替换为：

```java
public IDatasetClient build() {
    String normalizedBaseUrl = DifyValidationUtils.normalizeServer(baseUrl, "dataset", "build");
    String requiredApiKey = DifyValidationUtils.requireText(apiKey, "apiKey", "dataset", "build");

    try {
        return DatasetClientImpl.build(normalizedBaseUrl, requiredApiKey);
    } catch (Exception e) {
        throw new DifyClientException("Dify dataset client build error: failed to create DatasetClientImpl", e);
    }
}
```

同时在 imports 中加入：

```java
import com.xiongx9527.fw.plus.dify.types.DifyClientException;
```

在 `of(String datasetId)` 中把 `server(baseUrl)` 改为：

```java
.server(DifyValidationUtils.normalizeServer(baseUrl, "dataset", "build"))
```

在 `ofDocument(String datasetId, String documentId)` 中做相同替换。

- [x] **Step 7: 接入 `WebClientBuilder`**

在 `WebClientBuilder.connect()` 中替换为：

```java
public IWebConsoleClient connect() {
    String normalizedServer = DifyValidationUtils.normalizeServer(server, "web", "connect");
    String requiredUserName = DifyValidationUtils.requireText(userName, "userName", "web", "connect");
    String requiredPassword = DifyValidationUtils.requireText(password, "password", "web", "connect");

    try {
        WebConfig config = WebConfig.builder()
                .server(normalizedServer)
                .userName(requiredUserName)
                .password(requiredPassword)
                .build();
        TokenList tokenList = WebAuthService.login(config);
        return new WebClientImpl(normalizedServer, tokenList);
    } catch (DifyClientException | DifyException e) {
        throw e;
    } catch (Exception e) {
        throw new DifyClientException("Dify web client connect error", e);
    }
}
```

- [x] **Step 8: 运行构建器校验测试确认通过**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyValidationUtilsTest,DifyBuilderValidationTest test
```

Expected: PASS。

- [x] **Step 9: 勾选 OpenSpec 任务 2.1**

把 `openspec/changes/optimize-fw-plus-dify/tasks.md` 中以下行勾选：

```markdown
- [x] 2.1 优化 app、dataset、web console 客户端构建时的必填参数校验和错误信息。
```

- [x] **Step 10: 提交 Task 2**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/AppClientBuilder.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DatasetBuilder.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/WebClientBuilder.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DifyValidationUtils.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyValidationUtilsTest.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyBuilderValidationTest.java \
  openspec/changes/optimize-fw-plus-dify/tasks.md
git commit -m "feat(dify): validate client builder parameters"
```

---

### Task 3: 将 URL helper 接入 HTTP 请求链路

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/SimpleHttpClient.java:592-620`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtilsTest.java`
- Modify: `openspec/changes/optimize-fw-plus-dify/tasks.md:10-11`

**Interfaces:**
- Consumes: `DifyUrlUtils.buildUrl(String server, String route, Map<String, Object> query)`。
- Produces: `SimpleHttpClient` 所有 JSON、file、SSE、multipart 请求统一使用同一 URL 拼接规则。

- [x] **Step 1: 补充 query 空值边界测试**

在 `DifyUrlUtilsTest` 增加：

```java
@Test
void buildUrlShouldNotAppendQuestionMarkWhenAllQueryValuesAreNull() {
    Map<String, Object> query = new LinkedHashMap<>();
    query.put("keyword", null);

    assertEquals(
            "https://api.dify.ai/v1/datasets",
            DifyUrlUtils.buildUrl("https://api.dify.ai", "/v1/datasets", query)
    );
}
```

- [x] **Step 2: 运行测试确认当前实现失败或暴露多余问号问题**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyUrlUtilsTest test
```

Expected: 如果 Task 1 helper 已正确实现，应 PASS；如果出现 `...?` 尾随问号，应 FAIL 并在下一步修复。

- [x] **Step 3: 调整 helper 的 `appendQuery` 只在首个有效参数前追加 `?`**

确认 `DifyUrlUtils.appendQuery(...)` 使用以下逻辑：

```java
private static void appendQuery(StringBuilder sb, Map<String, Object> query) {
    if (query == null || query.isEmpty()) {
        return;
    }

    boolean first = true;
    for (Map.Entry<String, Object> entry : query.entrySet()) {
        if (entry.getKey() == null || entry.getValue() == null) {
            continue;
        }
        sb.append(first ? '?' : '&');
        sb.append(encode(entry.getKey()));
        sb.append('=');
        sb.append(encode(String.valueOf(entry.getValue())));
        first = false;
    }
}
```

- [x] **Step 4: 替换 `SimpleHttpClient.buildUrl` 内部逻辑**

在 `SimpleHttpClient` imports 中加入：

```java
import com.xiongx9527.fw.plus.dify.utils.DifyUrlUtils;
```

将私有方法改为：

```java
private String buildUrl(String url, Map<String, Object> query) {
    return DifyUrlUtils.buildUrl(this.server, url, query);
}
```

- [x] **Step 5: 运行 URL 测试和模块编译**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyUrlUtilsTest test
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -DskipTests compile
```

Expected: 两条命令均 PASS。

- [x] **Step 6: 勾选 OpenSpec 任务 2.2，并记录 2.3 最小处理边界**

把 `tasks.md` 中 2.2 勾选：

```markdown
- [x] 2.2 明确并实现 server 地址规范化规则，覆盖末尾斜杠、已有基础路径和 route 拼接边界。
```

如果没有完整打通所有 `ApiConfig` timeout / 连接池 / retry 链路，不要伪造勾选。只有确认当前默认值和已有传递链路已经审查完成后，再把 2.3 勾选。若本任务仅完成审查但未修改代码，在提交信息中说明“最小处理边界已确认”。

- [x] **Step 7: 提交 Task 3**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/SimpleHttpClient.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtils.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/utils/DifyUrlUtilsTest.java \
  openspec/changes/optimize-fw-plus-dify/tasks.md
git commit -m "feat(dify): normalize request URLs"
```

---

### Task 4: 增强 Dify 异常诊断信息

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/types/DifyException.java:1-85`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/SimpleHttpClient.java:279-328,373-429`
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/types/DifyExceptionTest.java`
- Modify: `openspec/changes/optimize-fw-plus-dify/tasks.md:15-16`

**Interfaces:**
- Consumes: HTTP status code 与 response body。
- Produces:
  - `public Integer getStatus()` 返回兼容 status。
  - `public Integer getHttpStatus()` 返回 HTTP status。
  - `public Integer getDifyStatus()` 返回 Dify body 中的 status。
  - `getOriginal()`、`getCode()`、`getParams()` 继续可用。

- [ ] **Step 1: 写 DifyException 失败测试**

创建 `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/types/DifyExceptionTest.java`：

```java
package com.xiongx9527.fw.plus.dify.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifyExceptionTest {

    @Test
    void shouldParseJsonErrorAndKeepHttpStatus() {
        DifyException exception = new DifyException(
                "{\"status\":400,\"code\":\"invalid_param\",\"message\":\"bad request\",\"params\":\"name\"}",
                422
        );

        assertEquals(422, exception.getHttpStatus());
        assertEquals(400, exception.getDifyStatus());
        assertEquals(400, exception.getStatus());
        assertEquals("invalid_param", exception.getCode());
        assertEquals("bad request", exception.getMessage());
        assertEquals("name", exception.getParams());
        assertTrue(exception.getOriginal().contains("invalid_param"));
    }

    @Test
    void shouldKeepNonJsonErrorBody() {
        DifyException exception = new DifyException("Bad Gateway", 502);

        assertEquals(502, exception.getHttpStatus());
        assertEquals(502, exception.getStatus());
        assertEquals("Bad Gateway", exception.getMessage());
        assertEquals("Bad Gateway", exception.getOriginal());
    }

    @Test
    void shouldUseStableMessageForEmptyErrorBody() {
        DifyException exception = new DifyException(null, 500);

        assertEquals(500, exception.getHttpStatus());
        assertEquals(500, exception.getStatus());
        assertEquals("Dify request failed with HTTP status 500", exception.getMessage());
    }
}
```

- [ ] **Step 2: 运行异常测试确认失败**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyExceptionTest test
```

Expected: FAIL，错误包含 `getHttpStatus()` 或 `getDifyStatus()` 未定义。

- [ ] **Step 3: 修改 `DifyException`**

将 `DifyException` 调整为以下结构，保留原 package 与 imports：

```java
package com.xiongx9527.fw.plus.dify.types;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Dify exception type
 */
public class DifyException extends RuntimeException {
    @Getter
    private String original;

    @Getter
    private Integer status;

    @Getter
    private Integer httpStatus;

    @Getter
    private Integer difyStatus;

    @Getter
    private String code;

    private String message;

    @Getter
    private String params;

    public DifyException() {
        super();
    }

    public DifyException(String response, Integer status) {
        this.httpStatus = status;
        this.status = status;
        this.original = response;

        if (response == null || response.trim().length() == 0) {
            this.message = "Dify request failed with HTTP status " + status;
            return;
        }

        try {
            JSONObject object = JSON.parseObject(response);
            this.difyStatus = object.getInteger("status");
            this.status = this.difyStatus != null ? this.difyStatus : status;
            this.code = object.getString("code");
            this.message = object.getString("message");
            this.params = object.getString("params");
            if (this.message == null || this.message.trim().length() == 0) {
                this.message = response;
            }
        } catch (Exception e) {
            this.message = response;
        }
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("httpStatus", httpStatus);
        map.put("difyStatus", difyStatus);
        map.put("code", code);
        map.put("message", message);
        map.put("params", params);
        return JSON.toJSONString(map);
    }
}
```

- [ ] **Step 4: 改善 `SimpleHttpClient` 客户端异常上下文**

在 `requestJson` 的最后一个 catch 中替换为：

```java
} catch (Exception e) {
    throw new DifyClientException("Dify request error: " + method + " " + url, e);
}
```

在 `requestFile` 的最后一个 catch 中替换为：

```java
} catch (Exception e) {
    throw new DifyClientException("Dify file request error: " + method + " " + url, e);
}
```

不要把 `apiKey` 拼入异常消息。

- [ ] **Step 5: 运行异常测试与编译**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyExceptionTest test
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -DskipTests compile
```

Expected: 两条命令均 PASS。

- [ ] **Step 6: 勾选 OpenSpec 任务 3.1、3.2**

把 `tasks.md` 中以下两行勾选：

```markdown
- [x] 3.1 优化 Dify HTTP 错误响应处理，保留 HTTP 状态、Dify code、message、params 和原始响应体。
- [x] 3.2 优化非 JSON 错误响应、空响应体和网络异常的诊断信息，保留原始异常链。
```

- [ ] **Step 7: 提交 Task 4**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/types/DifyException.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/SimpleHttpClient.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/types/DifyExceptionTest.java \
  openspec/changes/optimize-fw-plus-dify/tasks.md
git commit -m "feat(dify): improve error diagnostics"
```

---

### Task 5: 验证分页和动态 JSON 响应模型

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/types/DifyPageTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/types/DifyPage.java` if tests reveal missing mapping
- Modify: `openspec/changes/optimize-fw-plus-dify/tasks.md:17`

**Interfaces:**
- Consumes: `DifyPage` 当前字段与 Fastjson2 解析行为。
- Produces: 分页字段 `page`、`limit`、`total`、`total_pages`、`has_more`、`data` 的可验证映射。

- [ ] **Step 1: 写分页解析测试**

创建 `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/types/DifyPageTest.java`：

```java
package com.xiongx9527.fw.plus.dify.types;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DifyPageTest {

    @Test
    void shouldParsePageFieldsWithSnakeCaseNames() {
        String json = "{"
                + "\"page\":1,"
                + "\"limit\":20,"
                + "\"total\":2,"
                + "\"total_pages\":1,"
                + "\"has_more\":false,"
                + "\"data\":[{\"id\":\"dataset-1\",\"name\":\"知识库\",\"unknown\":\"value\"}]"
                + "}";

        DifyPage page = JSON.parseObject(json, DifyPage.class);

        assertEquals(1, page.getPage());
        assertEquals(20, page.getLimit());
        assertEquals(2, page.getTotal());
        assertEquals(1, page.getTotalPages());
        assertFalse(page.isHasMore());
        assertNotNull(page.getData());
        assertEquals(1, page.getData().size());
    }

    @Test
    void dynamicJsonShouldKeepUnknownFieldsWhenParsedAsJSONObject() {
        JSONObject object = JSON.parseObject("{\"id\":\"dataset-1\",\"future_field\":{\"enabled\":true}}");

        assertEquals("dataset-1", object.getString("id"));
        assertEquals(true, object.getJSONObject("future_field").getBoolean("enabled"));
    }
}
```

- [ ] **Step 2: 运行分页测试**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyPageTest test
```

Expected: PASS。如果 FAIL，错误应指向 `DifyPage` 字段命名或 getter 命名不匹配。

- [ ] **Step 3: 如分页字段映射失败，修正 `DifyPage`**

如果 `total_pages` 或 `has_more` 映射失败，在 `DifyPage` 对应字段上使用 Fastjson2 注解。目标代码片段：

```java
@JSONField(name = "total_pages")
private Integer totalPages;

@JSONField(name = "has_more")
private boolean hasMore;
```

并加入 import：

```java
import com.alibaba.fastjson2.annotation.JSONField;
```

如果测试已通过，不修改 `DifyPage`。

- [ ] **Step 4: 再次运行分页测试**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyPageTest test
```

Expected: PASS。

- [ ] **Step 5: 勾选 OpenSpec 任务 3.3**

把 `tasks.md` 中以下行勾选：

```markdown
- [x] 3.3 验证分页响应、动态 JSON 响应和布尔操作响应的解析行为，必要时调整模型或解析逻辑。
```

- [ ] **Step 6: 提交 Task 5**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/types/DifyPageTest.java \
  fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/types/DifyPage.java \
  openspec/changes/optimize-fw-plus-dify/tasks.md
git commit -m "test(dify): verify response model parsing"
```

---

### Task 6: 确认 Magic-API 兼容性并补充使用说明

**Files:**
- Modify: `openspec/changes/optimize-fw-plus-dify/tasks.md:21-23`
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/DifyClientBuilderCompatibilityTest.java`
- Optional Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dify/DifyModule.java` only if a compatibility compile error appears

**Interfaces:**
- Consumes: `DifyClientBuilder` 和 `DifyModule` 现有入口。
- Produces: Java 层兼容性测试，证明最小入口仍存在。

- [ ] **Step 1: 写客户端入口兼容性测试**

创建 `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/DifyClientBuilderCompatibilityTest.java`：

```java
package com.xiongx9527.fw.plus.dify;

import com.xiongx9527.fw.plus.dify.app.IAppBaseClient;
import com.xiongx9527.fw.plus.dify.app.IAppChatClient;
import com.xiongx9527.fw.plus.dify.app.IAppCompletion;
import com.xiongx9527.fw.plus.dify.app.IAppFlowClient;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DifyClientBuilderCompatibilityTest {

    @Test
    void shouldKeepDatasetEntryCompatible() {
        IDatasetClient client = DifyClientBuilder.dataset("https://api.dify.ai/", "dataset-key").build();
        assertNotNull(client);
    }

    @Test
    void shouldKeepAppEntriesCompatible() {
        IAppBaseClient base = DifyClientBuilder.app("https://api.dify.ai/", "app-key").base().build();
        IAppChatClient chat = DifyClientBuilder.app("https://api.dify.ai/", "app-key").chat().build();
        IAppFlowClient flow = DifyClientBuilder.app("https://api.dify.ai/", "app-key").flow().build();
        IAppCompletion completion = DifyClientBuilder.app("https://api.dify.ai/", "app-key").completion().build();

        assertNotNull(base);
        assertNotNull(chat);
        assertNotNull(flow);
        assertNotNull(completion);
    }
}
```

- [ ] **Step 2: 运行兼容性测试**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -Dtest=DifyClientBuilderCompatibilityTest test
```

Expected: PASS。

- [ ] **Step 3: 编译 Boot 层 DifyModule 所在模块**

只有当本次修改影响 Boot 层编译时执行；如果 Core 层 public API 未变，这一步仍可作为兼容证据执行：

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests compile
```

Expected: PASS。若因本地 Maven 依赖未安装失败，先不要修改源码；记录失败输出，按 systematic-debugging 流程定位是否为环境依赖问题。

- [ ] **Step 4: 如需补充使用说明，在 OpenSpec tasks 中记录说明位置**

如果项目已有 Dify README 或示例目录，补充以下说明；如果没有合适文档文件，不新建大篇文档，只在本计划执行证据中保留兼容性测试和异常行为说明：

```markdown
Magic-API `dify` 模块兼容入口保持不变：

- `dify.dataset(server, apiKey)` 创建知识库客户端。
- `dify.baseApp(server, apiKey)` 创建基础应用客户端。
- `dify.chatApp(server, apiKey)` 创建聊天应用客户端。
- `dify.flowApp(server, apiKey)` 创建工作流应用客户端。
- `dify.completionApp(server, apiKey)` 创建文本补全应用客户端。
- `dify.web(server, userName, password)` 创建 Web 控制台客户端并执行登录。

`server` 可以带或不带末尾 `/`；认证失败、Dify 错误响应和网络失败会抛出包含上下文的异常。
```

- [ ] **Step 5: 勾选 OpenSpec 任务 4.1、4.2、4.3**

把 `tasks.md` 中以下三行勾选：

```markdown
- [x] 4.1 确认 `dify.web(server, userName, password)`、`dify.dataset(server, apiKey)` 和各 app 入口保持兼容。
- [x] 4.2 如需新增配置能力，优先通过新增重载或新增方法实现，不改变现有入口语义。
- [x] 4.3 补充或更新 Magic-API 使用说明/示例，说明 Dify 客户端创建与错误处理行为。
```

如果没有新增文档文件，4.3 的验收证据为本任务中的兼容性测试、计划说明和最终验证报告；提交说明中写明“no public Magic-API entry changed”。

- [ ] **Step 6: 提交 Task 6**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/DifyClientBuilderCompatibilityTest.java \
  openspec/changes/optimize-fw-plus-dify/tasks.md
git commit -m "test(dify): keep client entry compatibility"
```

---

### Task 7: 完成核心行为测试、编译验证和验收映射

**Files:**
- Modify: `openspec/changes/optimize-fw-plus-dify/tasks.md:27-30`
- No source file changes expected unless previous task revealed a defect

**Interfaces:**
- Consumes: Tasks 1-6 的实现和测试。
- Produces: 可审查的构建、测试和 OpenSpec 验收证据。

- [ ] **Step 1: 运行 `fw-plus-dify` 全部单元测试**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify test
```

Expected: PASS。

- [ ] **Step 2: 运行 `fw-plus-dify` 编译验证**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -DskipTests compile
```

Expected: PASS。

- [ ] **Step 3: 如 Boot 层入口受影响，运行 Boot 编译验证**

如果 Task 6 修改过 Boot 层或 Core public API 变化可能影响 Boot，运行：

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests compile
```

Expected: PASS。

如果 Boot 层未受影响，在最终验证记录中写明：`Boot 层 DifyModule 未修改，兼容性由 Core 入口测试覆盖。`

- [ ] **Step 4: 对照 OpenSpec 规范逐项确认**

在执行记录或提交说明中确认：

```markdown
- Dify 客户端构建配置行为明确：参数校验测试、构建器兼容性测试覆盖。
- Dify 调用异常可诊断：DifyExceptionTest 覆盖 JSON、非 JSON、空响应和 cause 保留。
- Dify 响应模型解析稳定：DifyPageTest 覆盖分页和动态 JSON。
- Magic-API Dify 模块保持兼容：现有入口未改，Core builder 兼容性测试覆盖。
- Dify 优化具备可验证交付物：fw-plus-dify test 与 compile 通过。
```

- [ ] **Step 5: 勾选 OpenSpec 任务 5.1、5.2、5.3、5.4**

根据实际验证结果勾选：

```markdown
- [x] 5.1 为客户端构建、server 规范化、异常处理或分页解析补充测试或可审查示例。
- [x] 5.2 在 `fw-plus-core` 中编译验证 `fw-plus-dify` 相关模块。
- [x] 5.3 如 Boot 层入口受影响，在 `fw-plus-boot` 中编译验证相关自动配置模块。
- [x] 5.4 对照 `dify-client-optimization` 规范逐项确认验收场景已覆盖。
```

如果 Boot 层入口未受影响，将 5.3 勾选的依据写入提交说明：`Boot entry unchanged; Core compatibility test covers existing DifyModule call targets.`

- [ ] **Step 6: 记录 Comet build evidence**

如果 `comet guard optimize-fw-plus-dify build --apply` 无法自动识别 Maven 构建证据，记录实际运行过的命令：

```bash
comet state record-check optimize-fw-plus-dify build --command "cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify test" --exit-code 0
comet state record-check optimize-fw-plus-dify build --command "cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -DskipTests compile" --exit-code 0
```

- [ ] **Step 7: 提交 Task 7**

```bash
git add openspec/changes/optimize-fw-plus-dify/tasks.md
git commit -m "test(dify): verify optimization acceptance"
```

---

## Review and Verification Gates

- `review_mode: off`：在持久产物中记录跳过自动审查原因。
- `review_mode: standard`：所有计划任务完成后、`comet guard optimize-fw-plus-dify build --apply` 前，加载 `requesting-code-review` 做一次轻量审查。
- `review_mode: thorough`：总任务数大于 3，因此每 3 个任务做一次分段审查，最终再做一次完整审查。
- CRITICAL review 发现必须先修复，不能带入 verify。
- 非 CRITICAL review 发现如接受不修，必须在提交信息、tasks.md 注释或验证报告草稿中记录理由。

## Final Build Commands

实现全部完成后运行：

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify test
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify -DskipTests compile
```

如 Boot 层受影响，额外运行：

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests compile
```

## Self-Review

- Spec coverage: 已覆盖构建配置、URL 规范化、异常诊断、分页/动态 JSON、Magic-API 兼容、验证交付物。
- Placeholder scan: 本计划不包含 TBD、TODO、implement later 或“类似 Task N”的占位步骤。
- Type consistency: helper 名称统一为 `DifyUrlUtils` 与 `DifyValidationUtils`；异常 getter 统一为 `getStatus()`、`getHttpStatus()`、`getDifyStatus()`；测试中引用的方法均由对应任务定义。
