# Brainstorm Summary

- Change: add-langchain4j-model-magicapi
- Date: 2026-07-26

## 确认的技术方案

采用“新增 core model 契约 + Boot registry + 新 Magic model 插件，删除旧 Magic LangChain4j 重复实现”的方案。

用户已明确：旧的 Magic LangChain4j 实现应该删除，可以参考其代码，但最终使用更优雅、合理的新方案。

方案要点：
- `fw-plus-ai-langchain4j-model` 成为通用模型契约的唯一底座。
- AI Boot 自动配置负责配置绑定、provider adapter、factory 和 registry。
- Magic 插件侧新增统一 `model-datasource` 动态资源、动态注册器、controller 和脚本模块，直接委托 core/boot model registry。
- 旧 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 下重复的 model factory、dynamic datasource、resource storage、controller、module 等实现用于参考后删除或替换。
- 不保留旧 `langchain4j-datasource` 资源契约和旧 `langchain4j` 脚本入口兼容。

## 关键取舍与风险

采用旧实现删除方案的收益：
- 只有一套模型定义、工厂、registry 和 Magic 动态资源契约，边界更清晰。
- 不需要维护旧 `langchain4j` facade 和新 `model` registry 的双入口映射。
- 新插件命名可以对齐能力语义，避免继续把通用模型能力绑定到旧 Magic 插件实现细节。

主要风险：
- 旧 Magic 脚本入口和 `langchain4j-datasource` 资源不再兼容，属于破坏性变更，需要文档说明迁移方式。
- 示例项目或已有 starter 依赖 `fw-plus-ai-magicapi-plugin-langchain4j` 时需要迁移到新的 model 插件或 starter。
- 前端插件资源需要同步替换，否则 Magic 控制台仍会显示旧数据源类型。
- provider SDK 缺失、敏感字段泄露、动态模型 key 冲突、Java 1.8 兼容仍需测试覆盖。

## 测试策略

采用分层测试。
- core：工厂、adapter 选择、异常上下文、registry 默认 key。
- boot：配置绑定、条件装配、provider SDK 缺失隔离、默认模型 key 校验。
- Magic：新 `model-datasource` 动态资源注册/销毁/列举、脚本按 key 获取、controller 真实创建测试、敏感字段过滤。
- 迁移：确认旧 `langchain4j` Magic 插件 Java 类、starter 依赖和前端资源被删除或替换，样例项目依赖更新。
- 回归：vector 插件和 store embeding 相关测试继续通过。

## Spec Patch

已回写 OpenSpec delta spec：
- 删除“旧 LangChain4j Magic 入口可继续使用”的兼容要求。
- 新增“旧 Magic LangChain4j 插件实现被删除或替换”的要求。
- 将 Magic 资源类型明确为新 `model-datasource`，并把旧资源/前端/依赖迁移列为验收场景。
- proposal/tasks 同步标记这是对旧 Magic langchain4j 插件的破坏性替换，而不是兼容 facade。
