## Why

当前 Magic-API Boot sample 只引入 25 个 Boot 插件中的 17 个，Mongo 依赖被注释，AI 的 2 个插件位于另一 sample；因此现有 sample 启动无法证明宿主与全部插件能够在同一应用中完成依赖解析、自动配置和启动。首次统一校验还暴露了本地旧 AI BOM 阻断 sample reactor 的问题，需要建立可重复的分层构建与整体启动证据。

## Root Cause

- Magic-API 插件模块持续增加，但 sample 依赖清单没有与 Boot/AI 插件聚合器同步，缺少 Admin、Druid、DTP、Influx、MQTT、MQTT Client、Task，并注释了 Mongo。
- Boot 与 AI 插件分别放在两个 sample 中，没有一个宿主同时覆盖全部插件 classpath。
- sample 依赖本地 `0.1.1-SNAPSHOT` BOM 和库产物；未先按 Core -> Boot -> Cloud/AI 安装时，旧 BOM 会产生与源码不一致的依赖解析结果。
- 现有 application 配置依赖 Apollo、Redis、Mongo 等外部服务和硬编码绝对资源路径，无法作为稳定、可重复的本地整体启动入口。

## What Changes

- 以 Magic-API Boot sample 作为唯一整体测试宿主，补齐当前 Boot 与 AI 全部 Magic-API 插件依赖。
- 提供不依赖外部中间件的整体测试启动配置，同时保持生产代码和默认插件行为不变。
- 增加可自动化的插件 classpath、Spring 装配与关键入口检查，确保遗漏插件或启动冲突能直接失败。
- 按 Core -> Boot -> Cloud/AI -> sample 顺序构建，并实际启动 sample 验证健康状态和 Magic-API 宿主入口。
- 对测试过程中复现的源码缺陷逐项建立 RED 证据后修复并回归。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。本变更只修复 sample 集成和验证覆盖，不改变生产接口、插件行为或已有验收契约。

## Impact

- 影响 `fw-plus-sample` 中 Magic-API sample 的依赖和测试/启动配置。
- 可能影响测试发现存在冲突的 Boot/AI Magic-API 插件实现；任何源码修复都必须先有对应 RED 证据。
- 不升级 Magic-API 2.2.2，不修改内部 Nexus 坐标，不引入 Java 9+ API。

## Failure Evidence

- 命令：`./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate`
- 结果：失败，Maven 报 `fw-plus-ai-magicapi-plugin-model` 缺少版本。
- 定位：源码 AI BOM 已管理该版本，本地 `~/.m2` 中旧 BOM 未包含该条目；这是分层构建前置条件缺失的环境证据，不作为源码缺陷修复。
