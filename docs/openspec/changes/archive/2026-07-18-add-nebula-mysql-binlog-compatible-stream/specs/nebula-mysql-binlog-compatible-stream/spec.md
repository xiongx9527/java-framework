## ADDED Requirements

### Requirement: Gateway writes produce binlog-compatible events
系统 SHALL 为经 MySQL 协议网关成功执行的写入和 DDL 生成 binlog-compatible 事件。

#### Scenario: 写入事件生成
- **WHEN** 经网关执行 INSERT、UPDATE 或 DELETE 成功后，事件流中出现对应变更事件。
- **THEN** 系统产生符合要求的结果

#### Scenario: DDL 事件生成
- **WHEN** 经网关执行受支持 DDL 成功后，事件流中出现对应 schema 事件。
- **THEN** 系统产生符合要求的结果

### Requirement: Consumers can read events by position
系统 SHALL 提供可按 position 消费的事件流，使 CDC 工具可以持续读取网关侧事件。

#### Scenario: 从指定位置读取
- **WHEN** 消费者给定已有 position 
- **THEN** 系统从该位置之后返回事件。

#### Scenario: 持续读取
- **WHEN** 消费者持续订阅
- **THEN** 系统在新事件产生后可继续推送或拉取。

### Requirement: Capture boundary is explicit
系统 SHALL 明确只捕获经网关执行的写入，不声称捕获绕过网关直接写入 NebulaGraph 的变更。

#### Scenario: 绕过网关写入
- **WHEN** 当数据由 Nebula 原生客户端直接写入
- **THEN** binlog-compatible stream 不产生虚假事件，并在文档中说明边界。

