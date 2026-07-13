# Brainstorm Summary

- Change: optimize-fw-plus-dify
- Date: 2026-07-13
- Status: confirmed

## 确认的技术方案

采用“兼容 API + 内部基础设施层”方案。

现有外部入口保持不变：

- `DifyClientBuilder.app(...)`
- `DifyClientBuilder.dataset(...)`
- `DifyClientBuilder.web(...)`
- `DifyModule.web(server, userName, password)`
- `DifyModule.dataset(server, apiKey)`
- `DifyModule.baseApp(server, apiKey)`
- `DifyModule.chatApp(server, apiKey)`
- `DifyModule.flowApp(server, apiKey)`
- `DifyModule.completionApp(server, apiKey)`

在内部新增少量 helper，用于集中处理：

- URL 规范化：统一 server、route、query 参数拼接和编码规则。
- 参数校验：集中校验 server、apiKey、userName、password 等必填参数，并输出带上下文的错误信息。
- 异常转换：集中处理 HTTP 错误、Dify JSON 错误、非 JSON 错误、空响应体和网络异常。
- 响应解析辅助：优先稳定分页和动态 JSON 等基础响应模型。

helper 只作为内部实现细节，不引入新的 public facade，不改变现有 public API 与 Magic-API 入口语义。

## 关键取舍与风险

### 已确认取舍

- 接受新增少量内部 helper 类来集中 URL 规范化、配置校验和异常转换。
- public API 与 Magic-API 现有入口保持兼容，不改变现有方法名、参数顺序和返回类型语义。
- timeout、连接池和 retry 配置采用最小处理：本次明确默认值和校验，不强制完整打通所有配置链路。
- 响应模型保持宽松：分页等稳定结构可强类型解析，Dify 版本差异较大的响应继续保留 `JSONObject` 或原始 JSON 可访问性。

### 主要风险

- helper 设计过重，演变为 SDK 重构。
  - 缓解：helper 只服务 URL、参数、异常、响应辅助四类问题，不引入新 facade。
- 异常增强影响调用方 catch 行为。
  - 缓解：尽量保留现有异常类型或父类型兼容，只增强诊断信息。
- URL 规范化改变已有合法输入的最终 URL。
  - 缓解：通过单元测试覆盖典型 server/route/query 组合，保持已有合法输入结果稳定。
- timeout/连接池/retry 未全量打通可能超出调用方期待。
  - 缓解：Design Doc 明确本次为最小处理，完整配置链路作为后续扩展点。

## 测试策略

采用单元测试优先，文档/示例作为补充。

优先覆盖：

- URL 规范化：server 末尾有无 `/`、route 开头有无 `/`、query 参数编码。
- 参数校验：缺 server、缺 apiKey、缺 userName/password 等场景。
- 异常转换：JSON 错误响应、非 JSON 错误响应、空响应体、网络异常 cause 保留。
- 响应解析：分页字段 `total_pages`、`has_more`、`data` 等。

编译验证：

- 必跑 `fw-plus-core` 中 `fw-plus-dify` 相关模块。
- 如 Boot 层 `DifyModule` 受影响，再跑 `fw-plus-boot` 自动配置相关模块编译。

## Spec Patch

无。当前 OpenSpec delta spec `dify-client-optimization` 已覆盖本次设计边界，无需回写规范。