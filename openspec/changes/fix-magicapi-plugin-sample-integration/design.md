## Context

Boot 层当前有 25 个 Magic-API 插件模块，AI 层当前有 vector、model 2 个插件。Boot sample 只覆盖其中一部分，AI sample 又是独立应用，现有配置还会主动连接 Apollo、Redis、Mongo 等环境，整体启动结果不可重复。

## Root Cause

插件聚合器、sample 依赖和启动验证之间没有单一同步点。新增插件可以在各自模块内编译，但不会因为 sample 缺失或共同装配冲突而使构建失败。

## Fix

采用一个整体测试宿主：

1. 继续使用 `fw-plus-magicapi-spring-boot-starter-sample`，在其测试/验证 classpath 中显式纳入 Boot 25 个插件和 AI 2 个插件。
2. 为整体测试提供独立配置，使用随机端口和 worktree 内相对/临时资源目录，关闭 Apollo 启动拉取及需要真实连接的动态数据源，避免依赖外部服务。
3. 自动校验每个插件 artifact 均进入依赖树，并通过 Spring 上下文、Magic-API 插件元数据或对应关键 bean 验证可装配插件；受显式条件控制的插件同时验证条件报告。
4. 严格按 Core -> Boot -> Cloud/AI 顺序安装本地快照，再构建并启动 sample；启动后检查 Actuator 健康、Magic-API Web 入口和插件静态资源/管理入口。
5. 若某插件启动失败，按 debug gate 先定位根因、增加最小失败测试，再做单一源码修复。

## Non-Goals

- 不要求在本机启动 Kafka、Pulsar、RocketMQ、RabbitMQ、Redis、Mongo、Elasticsearch、InfluxDB、MQTT、S3 或 Debezium 数据源。
- 不执行依赖真实凭证和外部服务的数据读写业务测试。
- 不改变插件公开 API、配置键或运行时语义。
