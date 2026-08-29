## Why

LangChain4j 的 Model 与 Embedding Store Provider Adapter 当前分别散落在 `langchain4j.model.provider` 和拼写错误的 `langchain4j.store.embeding.provider` 中，而负责装配它们的自动配置位于 `langchain4j.adapter`。这使同一 Adapter seam 的实现跨越多个不一致的包层级，也与 Core 层已有的 `adapter.model` / `adapter.store` 结构不对齐。

## What Changes

- 将 Model Provider Adapter 实现及其辅助类迁移到 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.model`。
- 将 Embedding Store Provider Adapter 实现及其辅助类迁移到 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store`。
- 将两组对应的自动配置类分别下沉到 `adapter.model` 与 `adapter.store`，聚合自动配置继续保留在 `adapter` 根包。
- 同步迁移测试包并修正所有 import、package 声明和自动配置引用，保持 Bean 名称与运行行为不变。
- **BREAKING**：公开 Provider Adapter 实现类的 FQCN 将发生变化；不提供旧包兼容壳。

## Capabilities

### New Capabilities

无。本次为纯包结构重构，通过 `skip_specs: true` 明确不创建行为规格。

### Modified Capabilities

无。

## Impact

主要影响 `fw-plus-ai-boot-autoconfigure` 中 LangChain4j Adapter 自动配置、Provider Adapter 实现和相关测试。Core 层的 `LangChain4jModelProviderAdapter`、`EmbeddingStoreProviderAdapter` 接口及工厂选择逻辑不变；Spring Bean 名称和配置属性不变。用户若直接引用旧实现类 FQCN，需要改用新的 `adapter.model` 或 `adapter.store` 包名。
