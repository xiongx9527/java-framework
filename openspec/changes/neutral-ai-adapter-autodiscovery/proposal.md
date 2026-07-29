## Why

当前中立 model/store starter 和 AI Boot 自动配置仍直接依赖、装配 LangChain4j，旧的 model/store 模块及 starter 也继续作为公开坐标存在。这违反了框架无关边界，使运行时不能根据已安装的 adapter 安全选择框架，也误导后续 Java 17 adapter 的接入方式。

## What Changes

- **BREAKING** 删除 4 个旧 LangChain4j model/store artifact 及其聚合、BOM、starter、Magic-API 和样例引用。
- 新增统一、可选的 LangChain4j adapter 模块，作为中立 API 的默认 Java 8 实现；它是唯一允许引用 LangChain4j 类型的实现层。
- 中立 Core 模块、中立 starter、AI Boot 公共自动配置和 Magic-API 只依赖中立 API，不传递或引用 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope 类型或配置前缀。
- 新增 adapter 自动发现和选择规则：显式 `framework` 必须匹配已发现 adapter；未显式配置时仅一个候选可自动选用，零个或多个候选必须启动失败。
- **BREAKING** 重写迁移文档，说明旧坐标删除、依赖/配置替换、Magic-API 保持边界，以及 Java 17 / Boot 3.x adapter 仍未实现。

## Capabilities

### New Capabilities

- `ai-adapter-autodiscovery`: 定义中立模型/向量 adapter 的 classpath 发现、唯一候选自动选择和多候选失败规则。
- `ai-neutral-starter-boundary`: 规定中立 starter、公共自动配置和 Magic-API 不得携带任何具体 AI 框架依赖或类型。
- `ai-framework-breaking-migration-guide`: 提供旧 LangChain4j artifact 移除后的迁移、失败诊断和 Java 17 后续实现边界文档。

### Modified Capabilities

- `ai-framework-neutral-model-abstraction`: 强化模型 Core API 的零框架痕迹与 adapter 发现契约。
- `ai-framework-neutral-embedding-store-abstraction`: 强化向量 Core API 的零框架痕迹与 adapter 发现契约。
- `langchain4j-model-module`: 删除旧模型模块，改由统一 LangChain4j adapter 承担实现。
- `langchain4j-embedding-store-starter`: 删除旧向量 store 模块和 starter，改由统一 LangChain4j adapter 承担实现。
- `langchain4j-starter-modules`: 删除旧 model/store starter 聚合和 BOM 坐标。
- `magicapi-unified-vector-plugin`: 保持中立 facade，并使用自动发现后的中立 registry。
- `ai-framework-migration-guide`: 从兼容迁移改为破坏性坐标/配置迁移说明。

## Impact
