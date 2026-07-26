# 样例模块迁移 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Boot、Cloud 和 AI 样例迁移至独立的 `fw-plus-sample` 顶层 Maven 聚合目录，并删除空的 Core 样例聚合目录。

**Architecture:** `fw-plus-sample/pom.xml` 只提供跨层样例的统一聚合入口。三个迁移后的样例组继续继承各自的 `fw-plus-*-build`，以保留原有版本、依赖和插件管理；库层根 POM 不再聚合样例。

**Tech Stack:** Maven Wrapper、Maven Reactor、XML POM、Git。

## Global Constraints

- Java 和 Maven 配置保持 JDK 1.8 兼容。
- 使用各顶层目录的 Maven Wrapper，不使用系统 Maven。
- `fw-plus-sample` 仅为聚合 POM，不能成为三组样例的 Maven 父 POM。
- 不修改任何具体样例的 `artifactId`、Java 源码或功能行为。
- 保留工作区内未跟踪的 LangChain4j 存储扩展目录，不纳入本次操作。

---

### Task 1: 建立目标聚合与 Maven 解析保护

**Files:**
- Create: `fw-plus-sample/pom.xml`
- Modify: `fw-plus-sample/fw-plus-boot-samples/pom.xml`
- Modify: `fw-plus-sample/fw-plus-cloud-samples/pom.xml`
- Modify: `fw-plus-sample/fw-plus-ai-samples/pom.xml`

**Interfaces:**
- Consumes: `com.xiongx9527.fw.plus.boot:fw-plus-boot-build:0.1.1-SNAPSHOT`、`com.xiongx9527.fw.plus.cloud:fw-plus-cloud-build:0.1.1-SNAPSHOT`、`com.xiongx9527.fw.plus.ai:fw-plus-ai-build:0.1.1-SNAPSHOT` 已安装至本地 Maven 仓库。
- Produces: 可由 `fw-plus-sample/pom.xml` 统一解析的三个样例 Reactor 模块。

- [ ] **Step 1: 编写并运行迁移前的结构检查**

```bash
test -f fw-plus-sample/pom.xml
```

预期：命令失败，证明新聚合入口尚未创建。

- [ ] **Step 2: 使用 `git mv` 迁移三组样例目录并创建聚合 POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-sample</artifactId>
    <version>0.1.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Fw-Plus Samples</name>
    <modules>
        <module>fw-plus-boot-samples</module>
        <module>fw-plus-cloud-samples</module>
        <module>fw-plus-ai-samples</module>
    </modules>
</project>
```

在三个样例组 POM 的 `<parent>` 中、`<version>` 后加入所属层根 POM 的显式相对路径：

```xml
<!-- fw-plus-boot-samples -->
<relativePath>../../fw-plus-boot/pom.xml</relativePath>
<!-- fw-plus-cloud-samples -->
<relativePath>../../fw-plus-cloud/pom.xml</relativePath>
<!-- fw-plus-ai-samples -->
<relativePath>../../fw-plus-ai/pom.xml</relativePath>
```

这样 Maven 从所属层根 POM 解析对应的 `fw-plus-*-build` 及其 `revision` 属性，同时不会尝试把 `fw-plus-sample/pom.xml` 作为错误父 POM。

- [ ] **Step 3: 重新运行结构检查和 Maven 模型解析**

```bash
test -f fw-plus-sample/pom.xml
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
```

预期：第一个命令成功；第二个命令以退出码 0 完成，并显示三个样例组属于 Reactor。

- [ ] **Step 4: 提交聚合入口和目录迁移**

```bash
git add fw-plus-sample
git commit -m "refactor: consolidate sample modules"
```

### Task 2: 解除库层对样例目录的聚合

**Files:**
- Delete: `fw-plus-core/fw-plus-samples/pom.xml`
- Modify: `fw-plus-core/pom.xml`
- Modify: `fw-plus-boot/pom.xml`
- Modify: `fw-plus-cloud/pom.xml`
- Modify: `fw-plus-ai/pom.xml`

**Interfaces:**
- Consumes: Task 1 创建的 `fw-plus-sample/pom.xml`。
- Produces: 仅聚合自身库项目的四个原顶层 POM。

- [ ] **Step 1: 编写并运行迁移前的根 POM 断言**

```bash
rg -n '<module>fw-plus-samples</module>|<module>fw-plus-boot-samples</module>|<module>fw-plus-cloud-samples</module>|<module>fw-plus-ai-samples</module>' \
  fw-plus-core/pom.xml fw-plus-boot/pom.xml fw-plus-cloud/pom.xml fw-plus-ai/pom.xml
```

预期：命令输出四条样例聚合配置，证明尚未解除耦合。

- [ ] **Step 2: 删除空 Core 样例目录并移除四个根 POM 的样例模块行**

从以下文件各删除唯一对应的 `<module>` 行：

```text
fw-plus-core/pom.xml: <module>fw-plus-samples</module>
fw-plus-boot/pom.xml: <module>fw-plus-boot-samples</module>
fw-plus-cloud/pom.xml: <module>fw-plus-cloud-samples</module>
fw-plus-ai/pom.xml: <module>fw-plus-ai-samples</module>
```

使用 `git rm -r fw-plus-core/fw-plus-samples` 删除空聚合目录。

- [ ] **Step 3: 验证库层与样例层已解耦**

```bash
! test -e fw-plus-core/fw-plus-samples
! rg -n '<module>fw-plus-samples</module>|<module>fw-plus-boot-samples</module>|<module>fw-plus-cloud-samples</module>|<module>fw-plus-ai-samples</module>' \
  fw-plus-core/pom.xml fw-plus-boot/pom.xml fw-plus-cloud/pom.xml fw-plus-ai/pom.xml
```

预期：两个命令均以退出码 0 完成。

- [ ] **Step 4: 提交库层聚合调整**

```bash
git add fw-plus-core/pom.xml fw-plus-boot/pom.xml fw-plus-cloud/pom.xml fw-plus-ai/pom.xml
git add -u fw-plus-core
git commit -m "refactor: decouple samples from library reactors"
```

### Task 3: 更新文档与运行时资源路径

**Files:**
- Modify: `AGENTS.md`
- Modify: `CLAUDE.md`
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/main/resources/application.yaml`
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/main/resources/opentelemetry-javaagent.properties`
- Modify: `fw-plus-sample/fw-plus-cloud-samples/eureka-gateway-sample/src/main/resources/application.yaml`
- Modify: `fw-plus-sample/fw-plus-cloud-samples/eureka-gateway-sample/src/main/resources/opentelemetry-javaagent.properties`
- Modify: `fw-plus-sample/fw-plus-cloud-samples/eureka-server-sample/src/main/resources/application.yaml`
- Modify: `fw-plus-sample/fw-plus-cloud-samples/eureka-service-sample/src/main/resources/application.yaml`
- Modify: `fw-plus-sample/fw-plus-cloud-samples/eureka-service-sample/src/main/resources/opentelemetry-javaagent.properties`
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yaml`

**Interfaces:**
- Consumes: Task 1 的新目录布局。
- Produces: 与实际样例位置一致的开发文档和运行时资源目录定位。

- [ ] **Step 1: 编写并运行旧路径引用检查**

```bash
rg -n 'fw-plus-(boot|cloud|ai)/fw-plus-(boot|cloud|ai)-samples|fw-plus-core/fw-plus-samples' \
  AGENTS.md CLAUDE.md fw-plus-sample
```

预期：命令输出旧目录路径，证明文档和配置尚未同步。

- [ ] **Step 2: 将所有当前路径改为 `fw-plus-sample/<样例组>`**

在 `AGENTS.md` 和 `CLAUDE.md` 中将四个分散的样例树替换为 `fw-plus-sample/` 下的三组样例树，并删除 Core 样例描述。

在列出的 8 个资源文件中，将每个出现的：

```text
fw-plus-boot/fw-plus-boot-samples
```

替换为：

```text
fw-plus-sample/fw-plus-boot-samples
```

保留绝对路径的其他组成部分和被引用资源文件名不变。

- [ ] **Step 3: 验证文档与资源不再引用旧样例目录**

```bash
! rg -n 'fw-plus-(boot|cloud|ai)/fw-plus-(boot|cloud|ai)-samples|fw-plus-core/fw-plus-samples' \
  AGENTS.md CLAUDE.md fw-plus-sample
```

预期：命令以退出码 0 完成。

- [ ] **Step 4: 提交路径引用更新**

```bash
git add AGENTS.md CLAUDE.md fw-plus-sample
git commit -m "docs: update sample module locations"
```

### Task 4: 验证迁移后的 Maven 构建入口

**Files:**
- Verify: `fw-plus-sample/pom.xml`
- Verify: `fw-plus-sample/fw-plus-boot-samples/pom.xml`
- Verify: `fw-plus-sample/fw-plus-cloud-samples/pom.xml`
- Verify: `fw-plus-sample/fw-plus-ai-samples/pom.xml`

**Interfaces:**
- Consumes: Tasks 1 至 3 完成后的目录、POM 和资源路径。
- Produces: 对统一聚合入口和三个样例组 Maven 解析状态的构建证据。

- [ ] **Step 1: 验证全部 POM 为合法 XML**

```bash
for pom in $(rg --files fw-plus-sample -g 'pom.xml'); do
  xmllint --noout "$pom"
done
```

预期：所有命令以退出码 0 完成，无 XML 错误输出。

- [ ] **Step 2: 验证统一聚合入口与分组构建**

```bash
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
./fw-plus-boot/mvnw -f fw-plus-sample/fw-plus-boot-samples/pom.xml validate
./fw-plus-cloud/mvnw -f fw-plus-sample/fw-plus-cloud-samples/pom.xml validate
./fw-plus-ai/mvnw -f fw-plus-sample/fw-plus-ai-samples/pom.xml validate
```

预期：四条命令均以退出码 0 完成。

- [ ] **Step 3: 执行最终变更范围检查**

```bash
git diff --check HEAD
git status --short
```

预期：无空白错误；变更只包含迁移目录、根 POM、文档和资源路径，且不包含三个未跟踪的 LangChain4j 存储扩展目录。
