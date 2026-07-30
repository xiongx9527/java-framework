# Task 14 实现报告：Starter POM、BOM 和兼容验证

## 初始验证（RED/基线）

执行命令：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package
```

结果：`BUILD SUCCESS`。因此本任务的 RED 不是构建失败，而是基线 POM 依赖缺口：Starter 仅直接依赖
`fw-plus-ai-boot-autoconfigure` 与 `fw-plus-ai-langchain4j-model-spring-boot-starter`，没有直接暴露
Core `fw-plus-ai-askdata` 契约；AI BOM 也没有管理该 Core artifact。

基线日志同时提示本地 `fw-plus-ai-boot-autoconfigure:0.1.1-SNAPSHOT` POM 无效，Maven 不会提供其传递依赖。
该问题不属于本任务允许修改范围，但说明 Starter 不能仅依赖自动配置的传递关系来暴露契约。

## 修改内容

- 在 `fw-plus-ai-boot-dependencies` 的 `dependencyManagement` 中加入
  `com.xiongx9527.fw.plus:fw-plus-ai-askdata:${project.version}`。
- 在 AskData Starter 中显式依赖以下三个编译依赖，并按契约、默认适配器、自动配置排序：
  - `fw-plus-ai-askdata`
  - `fw-plus-ai-langchain4j-model-spring-boot-starter`
  - `fw-plus-ai-boot-autoconfigure`
- 未添加 Spring AI、Spring AI Alibaba、AgentScope Java 或 Java 17-only 依赖；未改动模块排序、Java 源码或测试。

## 最终验证（GREEN）

执行命令：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package
```

结果：`BUILD SUCCESS`，生成
`fw-plus-ai-askdata-spring-boot-starter-0.1.1-SNAPSHOT.jar`。

额外依赖树核验：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter dependency:tree -Dincludes=com.xiongx9527.fw.plus:fw-plus-ai-askdata,com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter,com.xiongx9527.fw.plus.ai:fw-plus-ai-boot-autoconfigure
```

结果：`BUILD SUCCESS`，三项均出现在 Starter 的直接 `compile` 依赖中。

## 风险信号

- Maven：本地 `fw-plus-ai-boot-autoconfigure:0.1.1-SNAPSHOT` POM 仍被 Maven 报为无效，导致其传递依赖不可用；本任务通过显式直接依赖保持 Starter 装配完整，但应由拥有自动配置模块的后续任务修复或重新安装该 POM。
- BOM：Core AskData 已由 AI BOM 显式管理，版本保持 `${project.version}`，与现有 AI 内部模块版本策略一致。
- Starter 依赖：Core 契约、LangChain4j 默认 adapter 与自动配置均为直接编译依赖；未引入禁止依赖或 Java 17 基线依赖。
