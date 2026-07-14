## Context

`fw-plus-template` 位于 Core 层，承担 Word、Excel、HTML 模板解析与部分格式转换能力。当前核心入口 `TemplateParser` 持有三类 parser：`WordAbstractParser`、`ExcelAbstractParser`、`HtmlAbstractParser`，默认实现分别依赖 EasyPoi、FreeMarker 以及可选的格式转换器。

该能力还通过 Boot 层自动配置创建 Bean，并通过 Magic-API `TemplateModule` 暴露给脚本侧调用。因此本次优化虽然目标模块是 `fw-plus-template`，但需要同步检查 Boot 自动配置与 Magic-API 模块入口，否则 Core 层修复可能无法在实际使用路径中生效。

当前主要约束：

- 项目保持 Java 8 与 Spring Boot 2.7.18 兼容。
- 不替换现有底层依赖，不改变 Maven 坐标、模块结构或包名。
- 对外 API 需要兼容已有调用，特别是已存在的 `parsWordByUrl` 拼写错误入口。
- 可选转换能力（如 Word 转 PDF、HTML 转 PDF）在依赖或 Bean 缺失时应明确失败，而不是 NPE。

## Goals / Non-Goals

**Goals:**

- 稳定 `TemplateParser` 的 Word / Excel / HTML 统一解析入口。
- 提供正确命名的 `parseWordByUrl`，同时保留旧 `parsWordByUrl` 兼容入口。
- 让缺失 parser、缺失可选转换器、非法输入、底层解析失败等场景具备明确错误信息。
- 修正或优化 Boot 自动配置条件，确保默认 Bean 能创建、用户自定义 Bean 能优先。
- 保持 Magic-API `template` 模块现有方法可用，并与 Core 入口命名保持一致。
- 补充测试覆盖核心入口、兼容 API、自动配置与异常边界。

**Non-Goals:**

- 不重写模板引擎体系。
- 不替换 EasyPoi、FreeMarker、docx4j、OpenPDF、FO 等底层依赖。
- 不改变现有模块结构、包名、Maven 坐标或对外 starter 体系。
- 不新增模板语言或文档格式。
- 不扩展 Product 层报表业务或 Magic-API 资源脚本。

## Decisions

### 1. 以兼容增强方式修正 Word URL API 命名

新增 `TemplateParser.parseWordByUrl(String url, Map<String, Object> map)` 作为正确拼写入口。旧方法 `parsWordByUrl` 保留，并委托到新方法。

理由：

- 直接重命名会破坏已有 Java 调用或 Magic-API 间接调用。
- 保留旧入口可以实现无破坏迁移。
- Magic-API `TemplateModule.parseWordByUrl` 应调用新入口，避免继续扩散拼写错误。

替代方案：直接删除旧方法。该方案更干净，但属于破坏性变更，不适合本次稳定性优化。

### 2. `TemplateParser` 对 parser 可用性进行显式校验

`TemplateParser` 在调用 Word / Excel / HTML parser 前应检查对应 parser 是否存在，并在不可用时抛出带能力名称的明确异常。

理由：

- 当前带参构造允许传入 `null`，自动配置也可能因条件或依赖导致某类 parser 为空。
- 直接调用会产生 NPE，无法判断是 Word、Excel 还是 HTML 能力缺失。
- 明确错误能提升 Magic-API 脚本侧和业务侧排障效率。

替代方案：构造函数中强制三类 parser 均非空。该方案会让只需要部分模板能力的使用方也必须引入全部依赖，不利于可选依赖场景。

### 3. 自动配置使用“缺 Bean”而不是“缺 Class”作为覆盖条件

Boot 层 parser Bean 与 `TemplateParser` Bean 的默认创建条件应基于 `@ConditionalOnMissingBean`，而不是无参数 `@ConditionalOnMissingClass`。

理由：

- 用户扩展通常通过提供自定义 Bean 完成，而不是移除 classpath 中的类。
- 无参数 `@ConditionalOnMissingClass` 在语义上不适合表达“容器中没有同类 Bean”。
- Spring Boot 自动配置惯例是：有依赖类时启用默认实现，用户 Bean 存在时让用户 Bean 优先。

替代方案：继续保留现状，仅在 `TemplateParser` 内做容错。该方案不能解决用户自定义 Bean 被覆盖或默认 Bean 未按预期创建的问题。

### 4. 转换能力保持可选，但失败信息必须明确

Word 转 HTML/PDF、HTML 转 PDF、Excel 转 HTML/PDF 等能力不在本次强制实现范围内。缺少转换器时继续抛出 `UnsupportedOperationException`，但消息应明确指出具体能力不可用。

理由：

- 格式转换依赖可能较重，且并非模板解析的核心路径。
- 现有实现已经将部分转换能力视为可选。
- 明确失败比静默降级或返回空结果更安全。

替代方案：自动引入默认转换器或降级为原文件返回。前者扩大依赖范围，后者容易制造静默错误。

### 5. 测试优先覆盖行为契约，不追求真实 Office 全链路

测试应优先覆盖 `TemplateParser` 委托行为、兼容方法、异常边界、自动配置 Bean 覆盖规则；对于 EasyPoi/Office 真实解析链路，只覆盖可稳定构造的轻量场景。

理由：

- Office 文件全链路测试容易受依赖、字体、平台环境影响。
- 本次主要目标是稳定框架入口和装配契约。
- 行为契约测试更能防止未来回归。

替代方案：引入大量真实 docx/xlsx 样例做端到端测试。该方案覆盖更完整，但维护成本高，不适合作为本次 open 阶段的默认方案。

## Risks / Trade-offs

- [Risk] 保留 `parsWordByUrl` 会继续暴露历史拼写错误。→ Mitigation：标记为兼容入口，内部委托到 `parseWordByUrl`，新代码和 Magic-API 模块统一使用正确名称。
- [Risk] 自动配置条件调整可能影响已有用户自定义 Bean 的装配顺序。→ Mitigation：使用 Spring Boot 惯用的 `@ConditionalOnMissingBean`，并补充自动配置测试锁定覆盖规则。
- [Risk] 部分 parser 为空时改为明确异常，可能改变原先 NPE 的异常类型。→ Mitigation：该变化提升可诊断性，不作为破坏性 API；测试中明确新异常信息。
- [Risk] 真实模板文件解析依赖外部库行为，测试可能不稳定。→ Mitigation：优先测试入口契约和轻量可控样例，避免过度依赖平台环境。
- [Risk] Boot 层与 Core 层分属不同模块，验证时可能需要按 Core → Boot 顺序构建。→ Mitigation：任务中明确构建顺序，先验证 Core，再验证涉及的 Boot 自动配置模块。

## Migration Plan

1. 在 Core 层完成 `TemplateParser` 入口兼容、parser 可用性校验和异常信息优化。
2. 在 Boot 层调整自动配置条件，确保默认 Bean 与用户 Bean 覆盖规则符合预期。
3. 在 Magic-API `TemplateModule` 中切换 Word URL 调用到 `parseWordByUrl`。
4. 补充单元测试和自动配置测试。
5. 先运行 Core 层相关 Maven 测试，再运行 Boot 层相关 Maven 测试或编译。
6. 若发现兼容风险，回滚策略是保留 Core 兼容方法不变，仅撤回自动配置条件调整或缩小调整范围。

## Open Questions

- 是否需要在本次实现中新增专门的 `TemplateException` 类型，还是先使用 `IllegalStateException` / `IllegalArgumentException` 搭配明确消息？默认建议先避免新增异常体系，除非实现阶段发现统一异常类型能明显降低调用方成本。
- 是否需要在后续文档中补充 Magic-API `template` 模块调用示例？默认不纳入本次 open 范围，除非实现阶段发现现有示例缺失会影响验收。
