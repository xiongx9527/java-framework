## Why

`fw-plus-dify` 已提供 Dify API 客户端、应用调用客户端、知识库客户端与 Magic-API `dify` 模块入口，但当前变更缺少明确的能力边界、验收标准和任务拆分，后续优化容易漂移到重写 SDK 或破坏现有脚本兼容性。

本变更通过 OpenSpec 明确 Dify 客户端优化范围，聚焦配置体验、调用一致性、异常可诊断性和 Magic-API 兼容性，为后续设计与实现提供稳定契约。

## What Changes

- 优化 Dify 客户端基础配置体验，明确 server、apiKey、timeout、连接池和重试等参数的默认值与边界行为。
- 梳理并统一 dataset、document、app、web console 等客户端接口的构建和调用约定。
- 改善 Dify 异常处理与响应模型，确保 HTTP 错误、Dify 错误响应、非 JSON 响应和连接失败均可诊断。
- 明确 Dify 路由拼接、分页响应解析、请求参数命名等基础类型行为。
- 保持 Boot 层 Magic-API `dify` 模块现有入口兼容，避免破坏已有脚本调用方式。
- 补充必要的测试、示例或文档，用于验证核心成功场景和关键边界场景。

## Capabilities

### New Capabilities

- `dify-client-optimization`: 规范并优化 fw-plus Dify 客户端的配置、构建、调用、异常处理和 Magic-API 集成行为。

### Modified Capabilities

- 无。

## Impact

- 主要影响代码：`fw-plus-core/fw-plus-project/fw-plus-dify/`。
- 可能影响 Boot 层 Magic-API 模块：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dify/DifyModule.java`。
- 可能新增或调整 Dify 客户端相关测试、示例和文档。
- 不应引入 Dify 服务端变更，不应改变已有 Magic-API `dify.dataset(...)`、`dify.chatApp(...)`、`dify.flowApp(...)` 等基础入口的兼容用法。
