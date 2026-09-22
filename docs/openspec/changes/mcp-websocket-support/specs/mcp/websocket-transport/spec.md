# Spec Delta

## Purpose

允许 MCP 客户端通过 WebSocket 协议与服务端通信，提供与 Streamable HTTP 等效的工具调用、资源读取、提示词和日志能力，同时支持服务端向客户端的双向实时推送。

## ADDED Requirements

### Requirement: WebSocket 端点可建立连接
The system SHALL 在配置路径上提供 WebSocket 端点，客户端完成握手后即进入 JSON-RPC 消息交换状态。

#### Scenario: 客户端连接 WebSocket 端点
- **WHEN** 客户端向配置的 WebSocket 路径发起 `ws://` 或 `wss://` 握手请求
- **THEN** 服务端完成握手并保持连接，等待 JSON-RPC 消息

#### Scenario: 连接被拒绝
- **WHEN** 客户端连接到一个未被 MCP 注册的 WebSocket 路径
- **THEN** 服务端返回 404 或拒绝握手

### Requirement: WebSocket 传输支持完整的 MCP JSON-RPC 生命周期
The system SHALL 通过 WebSocket 连接支持 MCP 协议的全部请求类型，包括 `initialize`、`tools/list`、`tools/call`、`resources/list`、`resources/read`、`prompts/list`、`prompts/get`、`completions` 和 `logging/setLevel`。

#### Scenario: 初始化握手
- **WHEN** 客户端通过 WebSocket 发送 `initialize` 请求
- **THEN** 服务端返回包含协议版本、服务端能力和指令的响应

#### Scenario: 工具调用
- **WHEN** 客户端通过 WebSocket 发送 `tools/call` 请求
- **THEN** 服务端执行工具并返回结果或错误响应

### Requirement: 服务端可通过 WebSocket 推送通知
The system SHALL 允许服务端通过已建立的 WebSocket 连接向客户端推送 JSON-RPC 通知消息，例如 `notifications/progress` 和 `notifications/logging/message`。

#### Scenario: 服务端推送日志通知
- **WHEN** 服务端产生日志事件且客户端已订阅日志级别
- **THEN** 服务端通过 WebSocket 向客户端发送 `notifications/logging/message` 通知

### Requirement: 连接异常时清理会话状态
The system SHALL 在 WebSocket 连接异常关闭（客户端断开、网络中断、服务端关闭）时，清理对应的 MCP 会话状态，防止内存泄漏。

#### Scenario: 客户端主动断开
- **WHEN** 客户端正常关闭 WebSocket 连接
- **THEN** 服务端清理该会话，不再接收新消息

#### Scenario: 客户端异常断开
- **WHEN** WebSocket 连接因网络原因异常中断
- **THEN** 服务端检测到连接关闭后清理该会话

### Requirement: WebSocket 传输可独立开关
The system SHALL 提供 `websocket-enabled` 配置属性，允许在不影响 Streamable HTTP 传输的情况下独立启用或禁用 WebSocket 传输。

#### Scenario: WebSocket 启用
- **WHEN** `websocket-enabled` 设置为 `true`
- **THEN** WebSocket 端点正常注册并接受连接

#### Scenario: WebSocket 禁用
- **WHEN** `websocket-enabled` 设置为 `false` 或未设置
- **THEN** WebSocket 端点不注册，连接请求被拒绝
