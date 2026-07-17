## ADDED Requirements

### Requirement: Clients can request event streaming through replication commands
系统 SHALL 支持 MySQL 复制协议的受控子集，使客户端可以通过复制命令读取网关侧事件。

#### Scenario: 注册或握手
- **WHEN** 客户端发起复制相关握手或注册
- **THEN** 系统建立事件读取上下文。

#### Scenario: 请求 binlog dump
- **WHEN** 客户端请求从指定 position 读取
- **THEN** 系统返回对应事件流。

### Requirement: Unsupported replication features fail explicitly
系统 SHALL 对完整 GTID 复制、真实 MySQL slave 语义或其他不支持命令返回明确错误。

#### Scenario: GTID 请求
- **WHEN** 客户端请求未支持的 GTID 复制能力
- **THEN** 系统返回明确不支持错误。

#### Scenario: 未知复制命令
- **WHEN** 客户端发送本子集未定义的复制命令
- **THEN** 系统返回明确不支持错误，且不进入普通查询或写入执行链路。

### Requirement: Replication stream uses gateway event positions
复制协议输出 SHALL 基于 binlog-compatible stream 的 position，不伪造全局 Nebula 变更顺序。

#### Scenario: position 对齐
- **WHEN** 客户端从已知 position 继续读取
- **THEN** 收到该 position 后的网关事件。

#### Scenario: position 不可用
- **WHEN** 客户端请求的 position 已过期、格式非法或不在可读取范围内
- **THEN** 系统返回明确 position 不可用错误，且不得静默跳到最新 position。

#### Scenario: 事件来源边界
- **WHEN** binlog-compatible stream 中不存在某个 Nebula 原生变更事件
- **THEN** 复制协议输出不得生成、补偿或伪造该事件。
