# Task 5 Report: Model Starter、BOM 与自动配置注册

## 实现内容

- 新增 `fw-plus-ai-langchain4j-model-spring-boot-starter`，产物坐标为 `com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter:${revision}`。
- Starter 直接依赖 `fw-plus-ai-boot-starter`、`fw-plus-ai-boot-autoconfigure` 与 `fw-plus-ai-langchain4j-model`；后者按现有 AI 自动配置模块模式使用 `${revision}`。
- 在 `fw-plus-ai-boot-starters` 聚合 POM 注册新模块，并在 `fw-plus-ai-boot-dependencies` BOM 中以 `${project.version}` 管理该 starter。
- 新增 README 的最小模型配置，明确具体 provider SDK 由业务应用显式引入。
- 扩展 `LangChain4jAutoConfigurationRegistrationTest`：校验 `spring.factories` 注册 `LangChain4jModelAutoConfiguration`，并通过 XML 结构解析校验 starter 的必要依赖与 provider SDK opt-in 边界。
- 将该测试类切换为已存在的 JUnit Jupiter `@Test`。模块的 Surefire 使用 JUnit Platform 且没有 JUnit Vintage Engine，原 JUnit 4 注解不会被执行。

## TDD Evidence

### RED

先按 brief 尝试 Wrapper：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jAutoConfigurationRegistrationTest test
```

失败原因符合环境限制：`.mvn/wrapper/maven-wrapper.properties` 与 `maven-wrapper.jar` 缺失，最终报出 `ClassNotFoundException: org.apache.maven.wrapper.MavenWrapperMain`。

使用等价的 Zulu 8 系统 Maven，并在将测试注解切换至 JUnit Jupiter 后得到实际 RED：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  mvn -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=LangChain4jAutoConfigurationRegistrationTest test
```

关键失败输出：

```text
Tests run: 7, Failures: 1, Errors: 0, Skipped: 0
Model starter POM should exist: .../fw-plus-ai-langchain4j-model-spring-boot-starter/pom.xml
```

### GREEN

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  mvn -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter \
  -am -DskipTests install
```

关键通过输出：10 个 Reactor 项目均为 `SUCCESS`，包括 `fw-plus-ai-langchain4j-model-spring-boot-starter`，总结果为 `BUILD SUCCESS`。

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  mvn -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=LangChain4jAutoConfigurationRegistrationTest test
```

关键通过输出：

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 文件变更

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter/pom.xml`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter/README.md`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/LangChain4jAutoConfigurationRegistrationTest.java`

## 自检结果

- `git diff --check` 通过，无空白错误。
- `mvn dependency:tree '-Dincludes=dev.langchain4j:*'` 仅显示 `fw-plus-ai-langchain4j-model -> dev.langchain4j:langchain4j-core:0.35.0`，未出现 concrete provider SDK。
- 未修改 Task 4 provider 矩阵、Magic runtime 或 plugin 文件。

## concerns

- `fw-plus-ai/mvnw` 继续不可用，原因是 Wrapper 配置和 jar 缺失；本任务所有 Maven 验证均以 Zulu 8 系统 Maven 完成。
- `.superpowers/sdd/task-5-brief.md` 是调度产物，按用户说明保留且不纳入业务提交；本报告同样保持 gitignored，不强制提交。
