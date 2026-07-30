# Task 1 报告

## 状态

DONE

## 提交

`db01718c4843667951ada4c7938288fefe66c505` - `test(ai): define consolidated autoconfigure boundary`

提交仅包含以下三个测试文件：

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/NeutralStarterDependencyTest.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/LegacyAiArtifactBoundaryTest.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/NeutralAiAutoConfigurationBoundaryTest.java`

## 验证

Baseline GREEN：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -am -DfailIfNoTests=false \
  -Dtest=NeutralStarterDependencyTest,LegacyAiArtifactBoundaryTest,NeutralAiAutoConfigurationBoundaryTest test
```

结果：BUILD SUCCESS；`NeutralAiAutoConfigurationBoundaryTest` 2 个测试、Starter 模块 5 个测试，共 7 个测试通过。

预期 RED：修改测试后以相同命令运行，BUILD FAILURE。首个失败为新增契约 `LangChain4jAdapterAutoConfiguration should be registered`，说明统一 `spring.factories` 注册尚未合入；失败不是依赖下载或环境问题。Maven 在 autoconfigure 模块失败后停止 reactor，因此其余新增 Starter/删除边界断言会由后续实现完成后一起转绿。

附加编译验证：使用 JDK 8 执行 `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter -am -DskipTests test-compile`，BUILD SUCCESS；`git diff --check` 通过。

## 风险信号

本任务命中“跨模块/跨子系统协调改动”：Starter、BOM、AI project reactor 与统一 autoconfigure 注册必须在后续任务保持同步。

## 顾虑

本任务按计划停在 RED，最终 GREEN 由 Task 4 完成。后续实现后须重新运行目标命令，确认所有 7 个测试通过，并验证旧 artifact 在目录、BOM、reactor、Starter 依赖和生产引用中均已移除。

## 审查修复（2026-07-30）

### 修复内容

修正 `LegacyAiArtifactBoundaryTest` 的旧 autoconfigure 模块目录断言。原断言将
`fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` 拼接在统一
`fw-plus-ai-boot-autoconfigure/` 目录下；真实旧模块位于 AI project 直接子目录，
所以旧目录残留时原断言会假通过。现改为实际相对路径：

```text
fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure
```

### 证明与验证

修复前的目录检查证明真实旧模块存在，而原断言推导的
`fw-plus-ai-boot-autoconfigure/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure`
不存在。

使用 JDK 8 和系统 Maven 运行完整聚焦 reactor 测试：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -am -DfailIfNoTests=false \
  -Dtest=NeutralStarterDependencyTest,LegacyAiArtifactBoundaryTest,NeutralAiAutoConfigurationBoundaryTest test
```

结果：预期 RED；reacter 先在既有的 `NeutralAiAutoConfigurationBoundaryTest` 失败，原因是
`LangChain4jAdapterAutoConfiguration` 尚未注册，Starter 模块因此跳过。

为直接验证本次路径修复，使用 JDK 8 和系统 Maven 单独运行：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/pom.xml \
  -Dtest=LegacyAiArtifactBoundaryTest test
```

结果：预期 RED，失败信息为
`Legacy artifact directory must be deleted: fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure`，表明修正后的断言已覆盖真实残留目录。

JDK 8 编译验证：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter \
  -am -DskipTests test-compile
```

结果：BUILD SUCCESS。`git diff --check` 通过。

### 提交

`617f6289edd96c564c7ea3e20ce8f80a666c9ff9` - `test(ai): correct legacy autoconfigure boundary path`

提交仅包含 `LegacyAiArtifactBoundaryTest.java`。

### 风险信号与顾虑

完整聚焦 reactor 命令目前会先被尚未完成的统一注册 RED 测试终止；本次目录断言已通过单独测试确认其 RED 语义。后续实现删除旧模块后，须重跑完整聚焦命令，确认所有结构契约转绿。
