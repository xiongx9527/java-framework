# neutral-ai-adapter-autodiscovery 验证报告

## 结论

PASS。当前 `dev` 已包含 `25f46b07 merge: neutral ai adapter autodiscovery`，并已推送到 `origin/dev`。本轮 verify 只补齐 Comet 状态、任务勾选、handoff/snapshot 元数据和验证证据；未修改生产实现代码。

## 范围核对

- 删除旧 `fw-plus-ai-langchain4j-model`、`fw-plus-ai-langchain4j-store-embeding` 及两个旧 starter。
- 保留 Java 8 / Spring Boot 2.7 中立 API：`fw-plus-ai-model`、`fw-plus-ai-store-embedding`。
- LangChain4j 作为默认 Java 8 adapter，通过 `fw-plus-ai-langchain4j-adapter` 与 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 接入。
- Spring AI、Spring AI Alibaba、AgentScope 2 Java 仅写入 Java 17 / Boot 3.x 后续实现文档，本次不实现。
- AskData 仅做删除旧坐标后的最小依赖换线，不做业务迁移。

## Fresh 验证命令

| 检查 | 命令/结果 |
| --- | --- |
| OpenSpec 严格校验 | `openspec validate neutral-ai-adapter-autodiscovery --type change --strict`：通过 |
| 迁移文档校验 | `zsh docs/ai-framework-migration/verify-migration-docs.sh`：`迁移文档校验通过: 3 份目标框架文档` |
| 空白/冲突检查 | `git diff --check`：通过 |
| 旧坐标与旧目录边界扫描 | `find` + `rg` 扫描旧 model/store artifact、旧 starter、公共中立模块 LangChain4j 痕迹：无输出 |
| AI 关键测试 | JDK 8 下 `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure -am -DfailIfNoTests=false -Dtest=NeutralAiAutoConfigurationBoundaryTest,AiModelAutoConfigurationTest,AiEmbeddingStoreAutoConfigurationTest,AiEmbeddingStoreAutoConfigurationMetadataTest,AskDataAutoConfigurationTest,LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest test`：公共 autoconfigure 30 tests、LangChain4j adapter autoconfigure 12 tests，BUILD SUCCESS |
| Adapter starter 依赖边界 | JDK 8 下 `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter validate`：BannedDependencies passed，BUILD SUCCESS |

## 代码审查

- 历史 final review 对 feature head `779cbc73` 结论：可合并；可选 SDK provider 条件装配和迁移文档修复通过。
- 本轮当前 merge diff `d4743e03..25f46b07` 只读 reviewer 阶段性结论：未发现 Critical。
- reviewer Important 1：主规格仍保留旧 model/store artifact 要求；处理方式：这是归档阶段需要按 delta spec 合并主规格的预期事项，不回 build。
- reviewer Important 2：缺 provider SDK 启动回归需稳定保留；处理方式：当前 adapter autoconfigure `FilteredClassLoader` 与 starter 边界测试已覆盖，并已通过 Maven 目标测试。

## 风险与后续

- Java 17 / Spring Boot 3.x adapter reactor、Spring AI、Spring AI Alibaba、AgentScope 2 Java 本次未实现，仅文档化后续方案。
- 旧坐标为破坏性删除，不支持新旧 artifact 混装。
- 当前工作区仍有 `add-tiny-engine-*` 与 `.superpowers/sdd/task-3/4/5/6-brief.md` 无关脏文件，未纳入本 change。
