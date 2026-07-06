## Why

归档后复查发现 Magic WebSocket 控制台测试仍有严格标准缺口：`/ws/execute` 未透传请求参数、未按 enabled 策略限制测试脚本，`/ws/datasource/test` 在 handler 缺失时仍可能成功。这会导致控制台测试结果与实际可用性存在偏差。

## Root Cause

当前 WS 控制台测试采用轻量模拟上下文实现，但模拟上下文固定使用空 `params`，脚本查找只按 `id` 匹配，数据源测试仅在 handler 存在时生成 token。

## Fix Goals

- `/ws/execute` 从请求体读取 `params` 并注入脚本上下文。
- `/ws/execute` 只允许测试 enabled 的 `ws-subscribe` 脚本。
- `/ws/datasource/test` 在 WebSocket handler 不可用时返回失败。
- 补充单元测试覆盖上述严格标准缺口。
