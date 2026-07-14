# Brainstorm Summary

- Change: optimize-fw-plus-template
- Date: 2026-07-14
- Status: brainstorming in progress

## 已确认事实

- OpenSpec change `optimize-fw-plus-template` 已进入 `phase: design`。
- `context_compression: beta`，handoff 使用 `spec-context.md` / `spec-context.json`。
- 产物语言为 `zh-CN`。
- Canonical spec 为 `openspec/changes/optimize-fw-plus-template/specs/template-parser-optimization/spec.md`。
- 目标聚焦 `fw-plus-template` 的 TemplateParser 入口稳定性、兼容 API、异常边界、Spring Boot 自动配置和 Magic-API template 模块兼容。
- 不替换底层模板引擎，不改变 Maven 坐标、模块结构、包名，不引入新模板格式。

## 当前代码观察

- `TemplateParser` 当前直接持有 Word/Excel/HTML parser，并将异常统一包装为无上下文 `RuntimeException`。
- `TemplateParser` 当前存在拼写错误方法 `parsWordByUrl`，Magic-API `TemplateModule.parseWordByUrl` 委托到该旧方法。
- `TemplateParser` 带参构造允许 parser 为 null，调用路径可能 NPE。
- `TemplateParserConfiguration` 当前使用无参数 `@ConditionalOnMissingClass`，不符合“用户 Bean 优先”的自动配置语义。
- `WordParser` 对旧版 `.doc` 已有识别并抛出较明确错误；Word/HTML 转 PDF 等可选转换器缺失时已抛出 UnsupportedOperationException，但仍需统一检查消息和测试覆盖。

## 确认的技术方案

用户已确认采用方案 A：最小兼容稳定化。

用户已确认第 1 段整体架构与组件边界：保持调用方/Magic-API → TemplateModule → TemplateParser → Word/Excel/HTML parser 的现有层次；Core 层负责入口与异常边界，Boot 层负责用户 Bean 优先的自动配置，Magic-API 层只做薄委托。

用户已确认第 2 段 Core 层实现细节中的关键取舍：`map == null` 时统一转为空 Map，而不是视为非法输入或直接透传到底层。

用户已确认第 3 段 Boot 自动配置与 Magic-API 兼容设计：Boot 默认 Bean 使用缺失 Bean 条件并尊重用户自定义 parser；`TemplateParser` 可在部分 parser 缺失时创建，调用时明确失败；`TemplateModule.parseWordByUrl` 委托到正确拼写的 `TemplateParser.parseWordByUrl`；Magic-API 仅做 Java 层委托测试。

方案边界：

- Core：在现有 `TemplateParser` 内新增 `parseWordByUrl`，保留 `parsWordByUrl` 兼容委托；增加 parser 可用性校验、非法输入校验和上下文明确的异常包装。
- Parser：保持 `WordParser`、`ExcelParser`、`HtmlParser` 现有职责，只治理可选转换器缺失和失败消息，不引入新抽象。
- Boot：将默认 parser Bean 和 `TemplateParser` Bean 的覆盖规则调整为用户 Bean 优先，并使用 `ApplicationContextRunner` 风格测试验证。
- Magic-API：`TemplateModule.parseWordByUrl` 委托到 `TemplateParser.parseWordByUrl`，只做 Java 层委托兼容测试。
- 不采用统一异常体系，不采用能力注册模型重构。

## 已比较但未采用的方案

1. 方案 B：统一异常与校验工具增强。未采用原因：用户已确认不新增 `TemplateException`，该方案会扩大 API/调用方理解成本。
2. 方案 C：能力注册模型重构。未采用原因：明显超出当前优化范围，违反“不重写模板引擎体系”的非目标。

## 已确认设计取舍

- 用户已确认选择 1：本次不新增专门 `TemplateException`，优先使用 Java 标准异常类型与明确错误消息。
- 异常语义建议：`IllegalArgumentException` 表示非法输入，`IllegalStateException` 表示 parser 能力缺失，`UnsupportedOperationException` 表示可选转换能力不可用。
- 用户已确认选择 1：允许在 Boot autoconfigure 模块中使用 Spring Boot `ApplicationContextRunner` 风格测试覆盖默认 Bean、用户 Bean 优先和 `TemplateParser` 组装规则。
- 用户已确认选择 1：Magic-API `TemplateModule` 只做 Java 层委托兼容测试，不引入 Magic-API 运行时脚本集成测试。

## 待确认问题

- 当前核心设计取舍已确认，下一步进入备选方案对比与推荐方案确认。

## 关键取舍与风险

- 采用标准异常可降低调用方适配成本，但需要通过消息和测试保证可诊断性。
- 自动配置测试放在 Boot autoconfigure 模块能真实覆盖条件装配，但需要按 Core → Boot 顺序验证，且测试依赖应保持轻量。
- Magic-API 测试只覆盖 Java 委托路径可以降低不稳定性，但不会验证完整脚本运行时；本次范围接受该取舍。

## 测试策略

用户已确认第 4 段测试策略与验证顺序：

- Core 层测试优先覆盖 `TemplateParser` 行为契约：正确拼写入口、旧入口兼容、Word/Excel/HTML 委托、parser 缺失、非法 data/url、`map == null` 转为空 Map。
- Parser 转换能力测试只覆盖可稳定断言的 unsupported 边界，不做复杂 Office 全链路端到端。
- Boot 自动配置测试使用 `ApplicationContextRunner` 覆盖默认 Bean 创建、用户 Bean 优先、`TemplateParser` 注入容器 parser、部分 parser 缺失时调用明确失败。
- Magic-API `TemplateModule` 只做 Java 层委托兼容测试，不启动 Magic-API runtime。
- 验证顺序按 Core → Boot 执行；若测试成本过高，至少运行目标模块测试和编译，并记录跳过项原因。

## Spec Patch

用户已确认选择 1：不回写 Spec Patch。

原因：现有 delta spec 已覆盖稳定入口、Word URL 兼容、parser 缺失、非法输入、可选转换、Boot 自动配置和 Magic-API 兼容；`map == null` 转为空 Map 属于实现层细化，不提升为 OpenSpec 行为需求。
