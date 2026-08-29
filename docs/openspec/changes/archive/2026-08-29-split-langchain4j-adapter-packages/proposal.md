## Why

`com.xiongx9527.fw.plus.ai.langchain4j.adapter` 根包同时承载模型适配与向量存储适配两组职责，21 个直属类型缺少清晰的领域边界，增加了定位代码和维护依赖的成本。本次调整将现有类型按职责归档到两个子包，保持运行行为不变。

## What Changes

- 将模型定义、工厂、注册表、provider adapter 和模型包装器迁移到 `com.xiongx9527.fw.plus.ai.langchain4j.adapter.model`。
- 将 Embedding Store 定义、配置、工厂、注册表、provider adapter 和存储包装器迁移到 `com.xiongx9527.fw.plus.ai.langchain4j.adapter.store`。
- 同步迁移模块内测试包并更新仓库内全部 import。
- **BREAKING**：被迁移公开类型的全限定类名发生变化，调用方需要更新 import；类型名称、方法签名和运行行为不变。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。本次为纯包结构重构，通过 `skip_specs: true` 明确不创建行为规格。

## Impact

- 影响 Core 层 `fw-plus-ai-langchain4j-adapter` 模块中的 21 个主类和 6 个测试类。
- 影响 Boot AI 自动配置层及其测试中引用这些类型的 import。
- 不修改 Maven 依赖、配置 schema、数据结构或运行逻辑。
