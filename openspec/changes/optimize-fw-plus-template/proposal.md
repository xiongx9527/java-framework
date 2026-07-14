## Why

`fw-plus-template` 是框架中 Word、Excel、HTML 模板解析与格式转换能力的核心入口，同时通过 Spring Boot 自动配置和 Magic-API `template` 模块对外暴露。当前模块存在入口命名不一致、解析器注入后可能为空、异常缺少上下文、自动配置条件使用不准确、缺少关键测试覆盖等风险，容易在模板解析、可选依赖缺失或用户自定义 parser 场景中出现运行期问题。

本次变更旨在稳定 `fw-plus-template` 的核心解析契约，提升自动装配兼容性、错误可诊断性和测试可信度，同时保持现有对外调用兼容。

## What Changes

- 梳理并稳定 `TemplateParser` 对 Word / Excel / HTML 三类解析器的统一入口。
- 修正 Word URL 解析入口的命名问题：新增正确拼写的 `parseWordByUrl`，并保留现有 `parsWordByUrl` 作为兼容入口。
- 改善 parser 为空、可选转换器缺失、输入非法、底层解析失败等场景的错误信息，避免无上下文 `RuntimeException` 或运行期 NPE。
- 优化 Spring Boot 自动配置条件，使用户自定义 `WordAbstractParser`、`ExcelAbstractParser`、`HtmlAbstractParser`、`TemplateParser` 时能够优先使用自定义 Bean。
- 保持 Magic-API `TemplateModule` 的现有方法可用，并在必要时切换到正确拼写入口。
- 补充覆盖核心入口、自动装配、兼容 API、异常边界的测试。
- 不替换底层模板引擎或转换依赖，不改变 Maven 坐标、模块结构或包名。

## Capabilities

### New Capabilities

- `template-parser-optimization`: 规范并稳定 `fw-plus-template` 的模板解析入口、自动装配行为、兼容 API 与错误处理契约。

### Modified Capabilities

- 无。本仓库现有 `openspec/specs/` 中没有专门描述 `fw-plus-template` 模块行为的规格；本次以新增能力规格承接该模块的优化契约。

## Impact

- 影响 Core 层模板模块：`fw-plus-core/fw-plus-project/fw-plus-template`。
- 必要联动 Boot 层自动配置：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/`。
- 必要联动 Magic-API 模块暴露：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/`。
- 影响对外 API 兼容策略：新增 `parseWordByUrl`，保留 `parsWordByUrl`，避免破坏已有调用。
- 影响测试与验证：需要在 Core/Boot 相关模块补充或调整单元测试、自动配置测试，并运行 Maven 编译/测试验证。
