# Brainstorm Summary

- Change: unify-vector-magicapi-plugin
- Date: 2026-07-25

## 确认的技术方案

采用 Core 多向量 registry/factory + provider adapter + AI Magic-API 单 vector 插件。Core starter 负责多实例配置、默认 store、provider adapter 构建和启动期校验；AI Magic-API 插件只暴露统一 `vector` 模块，并委托 Core registry 获取具体 `EmbeddingStore<TextSegment>`。

## 关键取舍与风险

不继续维护多个并列 provider 插件，避免脚本侧 API、starter 聚合和 BOM 管理持续分裂。主要风险是旧脚本入口兼容、Qdrant/Weaviate Core adapter 缺口、provider 依赖体积和默认 store 配置错误；通过兼容别名/迁移提示、可插拔 adapter、明确启动期校验和分层 BOM 管理缓解。

## 测试策略

Core 侧覆盖多 provider、多 key、同 provider 多实例、默认 store、重复 key、未知 provider 和缺失 adapter。AI 侧覆盖 `vector` 默认访问、按 key 访问、未知 key 错误、旧入口兼容或迁移提示。构建验证分别覆盖 Core LangChain4j reactor 与 AI boot autoconfigure/Magic-API plugin starter reactor。

## Spec Patch

无。当前 OpenSpec delta spec 已覆盖 starter 多向量、Magic-API 单插件和 starter 模块/BOM 管理。
