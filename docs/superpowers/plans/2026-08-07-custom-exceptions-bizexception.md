# 自定义异常统一继承 BizException 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让仓库内除 `BizException` 自身外的 38 个项目自定义异常全部直接继承 `BizException`。

**Architecture:** `BizException` 改为直接继承 `RuntimeException`，并提供兼容现有异常构造 API 的通用构造器。其他顶层和嵌套异常全部直接继承 `BizException`，不保留 `Exception`、`SQLException`、`BaseRuntimeException`、`ServiceException` 或其他项目异常作为中间父类；用根目录审计脚本持续验证这一结构约束。

**Tech Stack:** Java 8、Maven Surefire POJO 测试约定、Maven、Bash、ripgrep。

## Global Constraints

- Core 源码保持 Java 1.8，不使用 Java 9+ API。
- 所有 Java 源码保持 UTF-8，并沿用周围文件的 Tab/空格风格。
- `BizException` 之外的项目自定义异常必须直接 `extends BizException`。
- 名称包含 `Error` 但不属于 `Throwable` 体系的 DTO、接口和协议模型不修改。
- 原受检异常统一变为非受检异常；接受原父类捕获关系失效。
- 保留现有具体异常的公开构造 API、消息、原因和自有字段。
- 为解决 Java 返回类型冲突，领域码访问器统一改为 `getServiceCode()`、`getDifyCode()`、`getDbTxCode()`；仓库内调用与测试同步迁移。
- 通用 `BizException` 构造器使用 `SERVER + UNKNOWN + 0`，即错误码 `50999000`。
- 不修改 Magic-API 2.2.2、内部 Nexus 或现有依赖版本。
- 保留工作区现有 Tinyflow、动态实体、README 等无关修改；不创建提交。

---

### Task 1: 建立全仓直接继承审计

**Files:**
- Create: `scripts/verify-biz-exception-inheritance.sh`

**Interfaces:**
- Consumes: `fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai`、`fw-plus-sample` 下的 `src/main/java/**/*.java`。
- Produces: 退出码 `0` 表示 `BizException extends RuntimeException` 且其他所有 `*Exception` 类直接 `extends BizException`；退出码 `1` 并打印违规声明表示失败。

- [ ] **Step 1: 写入继承审计脚本**

```bash
#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd -P)"
cd "$repo_root"

search_roots=(fw-plus-core fw-plus-boot fw-plus-cloud fw-plus-ai fw-plus-sample)
biz_exception="fw-plus-core/fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/exception/BizException.java"

if ! rg -q 'class[[:space:]]+BizException[[:space:]]+extends[[:space:]]+RuntimeException' "$biz_exception"; then
    echo "BizException 必须直接继承 RuntimeException" >&2
    exit 1
fi

if violations="$(rg -n --glob '*.java' --pcre2 'class\s+(?!BizException\b)[A-Za-z0-9_]*Exception\b[^\{]*extends\s+(?!BizException\b)[A-Za-z0-9_$.]+' "${search_roots[@]}")"; then
    echo "以下自定义异常未直接继承 BizException：" >&2
    echo "$violations" >&2
    exit 1
fi
```

- [ ] **Step 2: 运行审计并确认 RED**

Run: `bash scripts/verify-biz-exception-inheritance.sh`

Expected: FAIL，首先报告 `BizException 必须直接继承 RuntimeException`；这证明审计能拦截当前继承结构。

### Task 2: 建立 BizException 根类和通用构造契约

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-common/src/test/java/com/xiongx9527/fw/plus/common/exception/BizExceptionTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/exception/BizException.java`

**Interfaces:**
- Consumes: `ErrorSource.SERVER`、`ErrorCategory.UNKNOWN`、`RuntimeException` 构造 API。
- Produces: `BizException()`、`BizException(String)`、`BizException(String, Throwable)`、`BizException(String, Object...)`、`BizException(Throwable)`；通用实例的 `getCode()` 返回 `50999000`，`getData()` 返回 `null`。

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.common.exception;

public class BizExceptionTest {

    public void testShouldBeTheRuntimeExceptionRoot() {
        assertSame(RuntimeException.class, BizException.class.getSuperclass());
    }

    public void testShouldSupportGenericExceptionConstructors() {
        IllegalStateException cause = new IllegalStateException("cause");
        BizException empty = new BizException();
        BizException message = new BizException("message");
        BizException messageAndCause = new BizException("wrapped", cause);
        BizException formatted = new BizException("value=%s", "42");
        BizException causeOnly = new BizException(cause);

        assertNull(empty.getMessage());
        assertEquals("message", message.getMessage());
        assertEquals("wrapped", messageAndCause.getMessage());
        assertSame(cause, messageAndCause.getCause());
        assertEquals("value=42", formatted.getMessage());
        assertSame(cause, causeOnly.getCause());
        assertEquals(50999000, message.getCode());
        assertNull(message.getData());
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private void assertNull(Object actual) {
        if (actual != null) {
            throw new AssertionError("expected null but was:<" + actual + ">");
        }
    }

    private void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new AssertionError("expected same:<" + expected + "> but was:<" + actual + ">");
        }
    }
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run from `fw-plus-core`: `mvn -f fw-plus-project/pom.xml -pl fw-plus-common -Dtest=BizExceptionTest test`

Expected: FAIL，测试编译报告缺少通用构造器，且根类断言尚不满足。

- [ ] **Step 3: 实现 BizException 根类和通用构造器**

将声明改为：

```java
public class BizException extends RuntimeException {
```

增加默认错误码和通用构造器；所有构造器设置 `data = null`：

```java
private static final int DEFAULT_CODE = 50_999_000;

public BizException() {
    super();
    this.code = DEFAULT_CODE;
    this.data = null;
}

public BizException(String message) {
    super(message);
    this.code = DEFAULT_CODE;
    this.data = null;
}

public BizException(String message, Throwable cause) {
    super(message, cause);
    this.code = DEFAULT_CODE;
    this.data = null;
}

public BizException(String msgFormat, Object... args) {
    super(String.format(msgFormat, args));
    this.code = DEFAULT_CODE;
    this.data = null;
}

public BizException(Throwable cause) {
    super(cause);
    this.code = DEFAULT_CODE;
    this.data = null;
}
```

- [ ] **Step 4: 运行测试并确认 GREEN**

Run from `fw-plus-core`: `mvn -f fw-plus-project/pom.xml -pl fw-plus-common -Dtest=BizExceptionTest test`

Expected: PASS，2 个测试通过。

### Task 3: 迁移 Core 通用、Dify、Redis、Tiny Engine 和 Nebula JDBC 异常

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/exception/BaseRuntimeException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/uid/exception/UidGenerateException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/dataset/types/external/KnowledgeException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/types/DifyException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/types/DifyExceptionTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/types/DifyClientException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nosql/fw-plus-redis/src/main/java/com/xiongx9527/fw/plus/nosql/redis/exception/RedisUrlSyntaxException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/exception/ServiceException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/sql/TinyEngineSqlIdentifierException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineAccessDeniedException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineUnauthenticatedException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineUnsupportedCapabilityException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/common/TinyEngineCommonContractTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/service/TinyEngineServiceContractTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/mysql/replication/BinlogStreamException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/mysql/replication/ReplicationException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/sql/parser/error/NebulaSqlParseException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/sql/query/NebulaQueryConversionException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/sql/routine/RoutineException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/sql/write/NebulaWriteConversionException.java`

**Interfaces:**
- Consumes: Task 2 的通用 `BizException` 构造器。
- Produces: 上述 17 个异常均直接以 `BizException` 为父类，原公开构造器与字段不变。

- [ ] **Step 1: 确认 Core 范围仍为 RED**

Run: `rg -n --glob '*.java' --pcre2 'class\s+(?!BizException\b)[A-Za-z0-9_]*Exception\b[^\{]*extends\s+(?!BizException\b)[A-Za-z0-9_$.]+' fw-plus-core/fw-plus-project/fw-plus-common fw-plus-core/fw-plus-project/fw-plus-dify fw-plus-core/fw-plus-project/fw-plus-nosql fw-plus-core/fw-plus-project/fw-plus-tiny-engine fw-plus-core/fw-plus-project/fw-plus-graph`

Expected: 输出上述 Core 异常的现有父类声明。

- [ ] **Step 2: 逐文件替换为直接继承 BizException**

先把领域码契约测试改为调用 `getServiceCode()` 和 `getDifyCode()`，运行以下命令并确认因新方法不存在而失败：

Run from `fw-plus-core`: `mvn -f fw-plus-project/pom.xml -pl fw-plus-dify,fw-plus-tiny-engine/fw-plus-tiny-engine-core -am -Dtest=DifyExceptionTest,TinyEngineCommonContractTest,TinyEngineServiceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL，报告 `getDifyCode()` 或 `getServiceCode()` 不存在。

随后修改生产类：`ServiceException` 和 `TinyEngineSqlIdentifierException` 保留字符串领域码并提供 `getServiceCode()`；`DifyException` 保留 Dify 字符串码并提供 `getDifyCode()`。三者继承的 `getCode()` 由 `BizException` 提供统一整数状态码。

每个非同包文件增加精确导入，并将类声明改为：

```java
import com.xiongx9527.fw.plus.common.exception.BizException;

public class ConcreteException extends BizException {
```

嵌套类不在本任务；删除不再使用的 `java.sql.SQLException`、`BaseRuntimeException`、`ServiceException` 导入，保留构造方法主体和自有字段。

- [ ] **Step 3: 验证 Core 范围 GREEN**

Run: `test -z "$(rg -n --glob '*.java' --pcre2 'class\s+(?!BizException\b)[A-Za-z0-9_]*Exception\b[^\{]*extends\s+(?!BizException\b)[A-Za-z0-9_$.]+' fw-plus-core/fw-plus-project/fw-plus-common fw-plus-core/fw-plus-project/fw-plus-dify fw-plus-core/fw-plus-project/fw-plus-nosql fw-plus-core/fw-plus-project/fw-plus-tiny-engine fw-plus-core/fw-plus-project/fw-plus-graph)"`

Expected: PASS，退出码为 `0`。

### Task 4: 迁移 Core AI 顶层与嵌套异常

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiModelException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/AiEmbeddingStoreException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/TinyflowException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeRepository.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/DefaultTinyflowIngestionService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/adapter/LangChain4jModelException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/adapter/EmbeddingStoreException.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-neo4j/src/main/java/dev/langchain4j/store/graph/neo4j/Neo4jException.java`

**Interfaces:**
- Consumes: Task 2 的通用 `BizException` 构造器。
- Produces: 10 个 Core AI 异常声明直接继承 `BizException`，包括 `OptimisticLockException`、`VectorStoreOwnershipException`、`CompensationStageException` 和 `PartialIndexException`。

- [ ] **Step 1: 确认 Core AI 范围仍为 RED**

Run: `rg -n --glob '*.java' --pcre2 'class\s+(?!BizException\b)[A-Za-z0-9_]*Exception\b[^\{]*extends\s+(?!BizException\b)[A-Za-z0-9_$.]+' fw-plus-core/fw-plus-project/fw-plus-ai`

Expected: 输出 10 个 Core AI 异常的现有父类声明。

- [ ] **Step 2: 将 10 个声明和必要导入改为 BizException**

顶层和嵌套类都使用同一声明形式：

```java
import com.xiongx9527.fw.plus.common.exception.BizException;

final class OptimisticLockException extends BizException {
```

`PartialIndexException` 从受检异常改为非受检异常；保留现有构造器和字段。

- [ ] **Step 3: 验证 Core 全范围 GREEN**

Run: `test -z "$(rg -n --glob '*.java' --pcre2 'class\s+(?!BizException\b)[A-Za-z0-9_]*Exception\b[^\{]*extends\s+(?!BizException\b)[A-Za-z0-9_$.]+' fw-plus-core)"`

Expected: PASS，退出码为 `0`。

### Task 5: 迁移 Boot 和 AI Boot 异常

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/exception/DbTxException.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/JavaEntityCompilationException.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/auth/McpAuthorizationException.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/client/McpClientCapabilityException.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/protocol/McpJsonRpcException.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/session/McpSessionNotFoundException.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/coordinator/LocalDbTxCoordinatorTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/coordinator/SeataDbTxCoordinatorTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/datasource/SeataDataSourceAdapterTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/starter/MagicAPISeataConfigurationTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/AskDataModelException.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/exception/MagicAPIVectorException.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalKnowledgeService.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalController.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowKnowledgeAccessKeyService.java`

**Interfaces:**
- Consumes: Task 2 的通用 `BizException` 构造器。
- Produces: 11 个 Boot/AI Boot 异常声明直接继承 `BizException`，包括嵌套的 `ExternalKnowledgeException`、`HeaderException` 和 `AuthenticationException`。

- [ ] **Step 1: 确认 Boot 和 AI Boot 范围仍为 RED**

Run: `rg -n --glob '*.java' --pcre2 'class\s+(?!BizException\b)[A-Za-z0-9_]*Exception\b[^\{]*extends\s+(?!BizException\b)[A-Za-z0-9_$.]+' fw-plus-boot fw-plus-ai`

Expected: 输出 11 个 Boot/AI Boot 异常的现有父类声明。

- [ ] **Step 2: 将 Boot 和 AI Boot 声明改为 BizException**

先将 `DbTxException` 相关断言改为调用 `getDbTxCode()`，运行 Boot autoconfigure 相关测试并确认因新方法不存在而失败。随后把领域码字段改为 `dbTxCode`，提供 `getDbTxCode()`，保留构造器与 `DbTxOutcome` 行为；继承的 `getCode()` 使用统一整数状态码。

```java
import com.xiongx9527.fw.plus.common.exception.BizException;

public class McpAuthorizationException extends BizException {
```

`HeaderException` 不再继承 `ExternalKnowledgeException`，构造器改为 `super("Authorization header is required")`。在控制器现有 `catch (ExternalKnowledgeException)` 之前增加 `catch (HeaderException)`，固定调用 `error(403, 1001, exception.getMessage())`，保留原响应语义。

- [ ] **Step 3: 运行全仓审计并确认 GREEN**

Run: `bash scripts/verify-biz-exception-inheritance.sh`

Expected: PASS，无输出，退出码为 `0`。

### Task 6: 分层编译、回归测试和差异审计

**Files:**
- Verify only: 所有本计划修改文件。

**Interfaces:**
- Consumes: Tasks 1-5 的完整异常继承改造。
- Produces: 可编译、测试通过且无无关差异的最终工作区。

- [ ] **Step 1: 安装 Core 受影响模块及其依赖**

Run from `fw-plus-core`: `mvn -f fw-plus-project/pom.xml -pl fw-plus-common,fw-plus-dify,fw-plus-nosql/fw-plus-redis,fw-plus-tiny-engine/fw-plus-tiny-engine-core,fw-plus-graph/fw-plus-nebula-jdbc,fw-plus-ai/fw-plus-ai-model,fw-plus-ai/fw-plus-ai-store-embedding,fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core,fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-neo4j -am install -DskipTests`

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 运行 Common 与受影响 Core 测试**

Run from `fw-plus-core`: `mvn -f fw-plus-project/pom.xml -pl fw-plus-common,fw-plus-dify,fw-plus-nosql/fw-plus-redis,fw-plus-tiny-engine/fw-plus-tiny-engine-core,fw-plus-graph/fw-plus-nebula-jdbc,fw-plus-ai/fw-plus-ai-model,fw-plus-ai/fw-plus-ai-store-embedding,fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core,fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-neo4j -am test`

Expected: `BUILD SUCCESS`，不运行需外部数据库的 live smoke profile。

- [ ] **Step 3: 编译并测试 Boot autoconfigure**

Run from `fw-plus-boot`: `mvn -f fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -am test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 4: 编译并测试 AI Boot autoconfigure**

Run from `fw-plus-ai`: `mvn -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -am test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 最终结构与格式检查**

Run: `bash scripts/verify-biz-exception-inheritance.sh`

Run: `git diff --check`

Run: `git status --short`

Expected: 继承审计和格式检查通过；状态中只新增本计划文档、审计脚本、测试和异常继承相关修改，原有脏文件保持存在且内容未被本任务覆盖。
