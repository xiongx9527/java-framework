# Brainstorm Summary

- Change: langchain4j-magicapi-model-plugins
- Date: 2026-09-28

## 确认的技术方案

七类 LangChain4j 模型资源以 magic-api datasource 形式提供，后端与前端分两侧交付：

- **datasource 形态**：每类资源在控制台「数据源」页签注册，JSON 持久化于 `{type}/*.json`；JSON = 公共字段（`key`/`name`/`provider`/`enabled`，`embedding-store` 另加 `embeddingModelKey`）+ `options` 自由参数对象。
- **每类资源三件套**：运行时持有者 `MagicDynamic<Type>DataSource`（`implements DisposableBean`，按 key 持有/驱逐/统一关闭）+ 动态注册器 `<Type>MagicDynamicRegistry`（规则 A：先驱逐 → `enabled` 检查 → 工厂构建 → 成功才落库）+ 资源存储 `<Type>MagicResourceStorage`（key 唯一与格式、provider 短名表、store 必填 `embeddingModelKey`）。
- **替换共享 holder**：删除现有跨七类的 `Langchain4jModelHolder`，改为七个按类型的 `MagicDynamic*DataSource`（用户确认）。
- **装配拆分为七个顶层子 Configuration**：父类 `Langchain4jDatasourceConfiguration` 只留 `plugin()` / `controllerRegister()` 与共享 bean，通过 `@Import` 引入七个子配置；对齐 `MagicAPIDruidConfiguration` 的既有写法（用户确认）。
- **provider 工厂 SPI**：每家一个显式工厂类（参照 core 层 `AutoConfig` 的 builder 链），短名表由已注册工厂的能力探测生成，存储校验与工厂注册表同源。
- **embedding-store 搭配 embedding-model**：`embeddingModelKey` 必填；dimension 决策链为 `options.dimension` → 被引用模型维度 → 中文报错；不做级联删除；启动完成钩子对启动期失败的 store 统一重试一次注册。
- **脚本模块只读化（BREAKING）**：`Langchain4jModule` 改为 `@MagicModule("langchain4j")`，删除全部 `build*` 与内部实例 map，`get*` 只读委托对应类型持有者并在未配置时抛中文异常（列出该类型已配置 key），`remove(type,key)` 改为驱逐语义，`destroy()` 清空并关闭（用户确认按设计 §7）。
- **工具桥纳入本 change（用户确认）**：`modules/langchain4j/tool` 把 magic-api 的 `api` / `function` 暴露为 LangChain4j 本地 `ToolProvider`；工具名 `METHOD /path` 与函数名，同名函数优先；过滤优先级为排除分组/路径 → 包含分组/路径，api 另有方法白名单与 opt-in；opt-in 选项名默认 `mcpTool`、入参 schema 属性默认 `mcpInputSchema`（与 MCP 插件共用同一处元数据）；快照缓存 + 变更事件失效（不用 TTL）；执行异常与未知工具返回错误文本而不抛异常。
- **包结构修正**：datasource 插件在 `plugin/langchain4j/**`（与 mongo/kafka/redis 插件同级），脚本模块与工具桥在 `modules/langchain4j/**`。
- **依赖**：AI 层 BOM 统一管理 langchain4j 全生态版本，`fw-plus-ai-boot-autoconfigure` 以 `optional` 引入；core 层 `fw-plus-ai-langchain4j-*` 零修改。

## 关键取舍与风险

- 脚本侧统一实例来源，删除 `build*` 会破坏旧脚本 → 记录为迁移项（改控制台配置 + `get*`），并提供总开关灰度与回滚。
- 七个持有者替换共享 holder 的重构面覆盖七个注册器与脚本取用路径 → 分类型原子提交，避免中间态出现两套实例来源。
- `options` 为自由 map，键名拼写错误只在构建期暴露 → 强类型读取器缺失报错带完整路径、前端按 provider 元数据渲染表单、连接测试即时回显。
- 显式工厂数量大 → 模板化写法 + 每个资源类型至少一个代表单测；短名表与工厂一一对应由单测保证。
- 全量 optional 依赖增大依赖树 → BOM 统一版本，optional 不向下游传递。
- embedding-store 依赖加载顺序 → 启动完成钩子统一重试。
- 工具桥把异常降级为错误文本可能掩盖脚本缺陷 → 错误文本包含异常摘要，便于日志与模型侧同时定位。

## 测试策略

- 单元测试（JUnit 5）：选项读取器（含完整路径报错）、工厂注册表（短名覆盖与能力一致）、代表工厂（builder 链与默认值）、存储校验（key/provider/embeddingModelKey）、七个持有者（驱逐关闭、关闭异常不阻断、destroy 清空）、embedding-store 注册器（依赖缺失、dimension 三链路、启动重试）、脚本模块（委托、未配置异常、驱逐、关闭）、工具桥（过滤优先级、方法/opt-in、schema、命名与同名优先、执行语义、变更失效、装配开关）。
- 集成测试：上下文拉起后七类存储/持有者/注册器 bean 齐备，文件事件触发注册后取用成功、删除后消失。
- 审计：core 层 `git diff` 为空；命名审计无 `uid` 残留；样例工程静态配置路径不回归。
- 交付物验证：前端子工程 `mvn package` 后 `target/classes/magic-editor/plugins/magic-langchain4j.1.0.0.iife.js` 存在且非空。

## Spec Patch

无。Open 阶段已按最终范围写好三个 delta spec（`langchain4j-model-datasource-plugins`、`langchain4j-script-module`、`magicapi-langchain4j-tool-provider`），设计阶段的确认未引入需要回写的验收场景变更。

## 流程说明

Design 阶段协议要求加载 Superpowers `brainstorming` 技能，但该插件在本机未安装/启用且安装命令在会话内无法完成（三次尝试均无输出）。用户于 2026-09-28 明确授权跳过该技能步骤，以本文件中已确认的设计决策与 `design.md` 的 D1–D10 作为设计依据继续。
