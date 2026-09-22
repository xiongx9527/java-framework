# Spec Delta

## Purpose

允许 MCP 会话状态存储在外部系统（Redis）而非单机内存，使集群环境下多实例共享会话数据，支持故障转移和横向扩展。

## ADDED Requirements

### Requirement: 会话状态可存储到外部系统
The system SHALL 提供 `McpSessionStore` 抽象接口，支持将 MCP 会话数据持久化到外部存储。

#### Scenario: 会话创建时写入外部存储
- **WHEN** 新 MCP 会话建立（SSE 或 WebSocket）
- **THEN** 会话数据被写入外部存储（Redis 或内存）

#### Scenario: 会话读取时从外部存储获取
- **WHEN** 客户端携带 `Mcp-Session-Id` 重新连接
- **THEN** 服务端从外部存储加载会话数据并恢复

### Requirement: Redis 可用时自动使用 Redis 存储
The system SHALL 在 `spring-boot-starter-data-redis` 在 classpath 且 Redis 连接可用时，自动选择 Redis 作为会话存储后端。

#### Scenario: Redis 可用时选择 Redis
- **WHEN** 应用引入 Redis 依赖且 Redis 连接正常
- **THEN** 会话数据存储到 Redis，所有实例共享

#### Scenario: Redis 不可用时回退内存
- **WHEN** 应用未引入 Redis 依赖，或 Redis 连接失败
- **THEN** 会话数据存储在单机内存，行为与当前版本一致

### Requirement: 会话超时与过期清理
The system SHALL 支持会话超时机制，外部存储实现的会话应设置过期时间，内存实现的会话应定期清理。

#### Scenario: Redis 会话过期
- **WHEN** 会话在 Redis 中超过 `session-timeout-seconds` 未活跃
- **THEN** Redis 自动过期该键，下次访问时视为会话失效

#### Scenario: 内存会话清理
- **WHEN** 内存会话超过 `session-timeout-seconds` 未活跃
- **THEN** 清理线程定期移除过期会话

### Requirement: 会话外部化对客户端透明
The system SHALL 确保会话外部化的切换不改变客户端可见行为，客户端无需感知存储后端。

#### Scenario: 客户端不感知存储后端
- **WHEN** 客户端通过 SSE 或 WebSocket 进行 MCP 交互
- **THEN** 无论会话存储在内存还是 Redis，客户端体验一致
