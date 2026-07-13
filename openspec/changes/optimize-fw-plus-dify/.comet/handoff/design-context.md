# Comet Design Handoff

- Change: optimize-fw-plus-dify
- Phase: design
- Mode: compact
- Context hash: 0934084067925930e96d1aa25259b2ce941e9ef5858094e45c8eb061ac3f155f

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/optimize-fw-plus-dify/proposal.md

- Source: openspec/changes/optimize-fw-plus-dify/proposal.md
- Lines: 1-31
- SHA256: 4d229b7ec615bf71ba88d138c66a92fbcc03345f62adbfa69092ff5881a0d5c8

```md
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

```

## openspec/changes/optimize-fw-plus-dify/design.md

- Source: openspec/changes/optimize-fw-plus-dify/design.md
- Lines: 1-118
- SHA256: c859e5fe865be7e684151a138e055ee6d90a3e8eafdb0860a11522b00725273a

[TRUNCATED]

```md
## Context

`fw-plus-dify` 位于 Core 层，提供 Dify API 客户端能力，当前已覆盖 app、dataset、web console 等方向，并通过 Boot 层 `DifyModule` 暴露给 Magic-API 脚本使用。

当前代码中已经存在以下基础结构：

- `DifyClientBuilder`：统一入口，创建 app、dataset、web console 构建器。
- `AppClientBuilder`、`DatasetBuilder`、`WebClientBuilder`：不同客户端类型的构建器。
- `SimpleHttpClient`：基于 OkHttp 的底层 HTTP/SSE/multipart 调用封装。
- `ApiConfig`、`DifyRoute`、`DifyPage`、`DifyException`：基础配置、路由、分页与异常模型。
- `DifyModule`：Boot 层 Magic-API `dify` 模块入口。

本次优化需要在不破坏现有调用方式的前提下，明确 Dify 客户端行为边界，改善配置、异常、响应模型和验证手段。

## Goals / Non-Goals

**Goals:**

- 明确 app、dataset、web console 客户端的构建配置规则。
- 统一 server、apiKey、timeout、连接池和重试等配置的默认值与边界行为。
- 改善 Dify HTTP 调用异常的可诊断性，保留 HTTP 状态、Dify 错误字段、原始响应和底层异常链。
- 稳定分页、动态 JSON、布尔操作等响应模型解析行为。
- 保持 Magic-API `dify` 模块现有入口兼容。
- 补充可执行或可审查的验证证据，至少覆盖编译、客户端构建、异常处理、分页解析或 Magic-API 入口之一。

**Non-Goals:**

- 不重写整个 Dify SDK。
- 不引入新的 AI 框架或替换 Dify 服务端。
- 不变更 Dify 服务端 API。
- 不主动修改 Product 层业务代码或真实运行的 Magic-API 业务脚本。
- 不以破坏兼容性的方式重命名 `DifyModule` 现有方法或调整参数顺序。

## Decisions

### Decision 1: 以兼容优先的方式优化构建器

继续保留 `DifyClientBuilder.app(...)`、`DifyClientBuilder.dataset(...)`、`DifyClientBuilder.web(...)` 作为统一入口，不引入新的顶层入口替代现有 API。

- 现有调用方式保持可用，减少对 Magic-API 脚本和 Java 调用方的迁移成本。
- 允许在现有构建器中补充配置校验、默认值应用和错误信息增强。
- 对于确需新增的配置能力，优先通过重载、配置对象或 builder 链式方法扩展，而不是改变已有方法签名。

替代方案：重写为新的 Dify SDK facade。该方案可获得更统一的架构，但会提高迁移成本，并扩大本次 change 的范围，因此不采用。

### Decision 2: 将 server 规范化作为底层请求一致性的关键点

Dify API 路由拼接依赖 server 与 route 的组合。优化时应明确处理以下情况：

- server 末尾是否包含 `/`。
- server 是否已经包含 `/v1` 或其他基础路径。
- route 是否以 `/` 开头。
- query 参数编码是否稳定。

目标是在客户端内部形成一致规则，避免调用方手动调整 server 格式。

替代方案：要求调用方始终传入完全规范化 server。该方案实现简单，但调用体验差，且容易导致 Magic-API 脚本中出现重复处理逻辑，因此不采用。

### Decision 3: 异常模型保留原始信息，不吞并底层原因

Dify 调用失败时，需要区分并保留：

- HTTP 状态码。
- Dify 响应中的 `code`、`message`、`params`。
- 非 JSON 响应的原始响应体。
- 网络异常、超时异常或序列化异常的原始异常链。

实现时应避免只抛出泛化 `RuntimeException` 或丢失响应体。对于无法解析为 Dify 错误结构的响应，也应能返回可诊断信息。

替代方案：继续保持当前简单包装。该方案改动最小，但不利于排查线上 API Key、网关、超时和 Dify 服务端错误，因此不采用。

### Decision 4: 响应模型保持宽松解析，避免未知字段导致失败

Dify API 演进可能引入新增字段。客户端模型应允许未知字段存在，并在必要时保留动态 JSON 访问能力。

- 对稳定结构如分页响应，可继续使用强类型模型。
- 对接口差异较大或 Dify 版本变动较频繁的响应，可保留 `JSONObject` 返回。
- 不要求本次一次性覆盖所有 Dify API 响应类型。

替代方案：将全部响应都建模为强类型 POJO。该方案类型安全更高，但容易扩大工作量，并增加版本兼容成本，因此不作为本次目标。

```

Full source: openspec/changes/optimize-fw-plus-dify/design.md

## openspec/changes/optimize-fw-plus-dify/tasks.md

- Source: openspec/changes/optimize-fw-plus-dify/tasks.md
- Lines: 1-30
- SHA256: c79af1308a0b8745359155464c6b7939deb599777bc650d9d1d4872499123b91

```md
## 1. 现状梳理与范围确认

- [ ] 1.1 梳理 `fw-plus-dify` 的 app、dataset、web console 客户端构建链路和现有 public API。
- [ ] 1.2 梳理 Boot 层 Magic-API `DifyModule` 入口，确认需要保持兼容的方法名、参数顺序和返回类型语义。
- [ ] 1.3 明确本次实现不覆盖的 Dify API 范围，避免扩展为全量 SDK 重写。

## 2. 客户端构建与配置优化

- [ ] 2.1 优化 app、dataset、web console 客户端构建时的必填参数校验和错误信息。
- [ ] 2.2 明确并实现 server 地址规范化规则，覆盖末尾斜杠、已有基础路径和 route 拼接边界。
- [ ] 2.3 检查 `ApiConfig` 中 timeout、连接池和重试配置是否被关键客户端正确应用，必要时补齐传递链路。

## 3. 异常与响应模型优化

- [ ] 3.1 优化 Dify HTTP 错误响应处理，保留 HTTP 状态、Dify code、message、params 和原始响应体。
- [ ] 3.2 优化非 JSON 错误响应、空响应体和网络异常的诊断信息，保留原始异常链。
- [ ] 3.3 验证分页响应、动态 JSON 响应和布尔操作响应的解析行为，必要时调整模型或解析逻辑。

## 4. Magic-API 兼容性保障

- [ ] 4.1 确认 `dify.web(server, userName, password)`、`dify.dataset(server, apiKey)` 和各 app 入口保持兼容。
- [ ] 4.2 如需新增配置能力，优先通过新增重载或新增方法实现，不改变现有入口语义。
- [ ] 4.3 补充或更新 Magic-API 使用说明/示例，说明 Dify 客户端创建与错误处理行为。

## 5. 验证与收尾

- [ ] 5.1 为客户端构建、server 规范化、异常处理或分页解析补充测试或可审查示例。
- [ ] 5.2 在 `fw-plus-core` 中编译验证 `fw-plus-dify` 相关模块。
- [ ] 5.3 如 Boot 层入口受影响，在 `fw-plus-boot` 中编译验证相关自动配置模块。
- [ ] 5.4 对照 `dify-client-optimization` 规范逐项确认验收场景已覆盖。

```

## openspec/changes/optimize-fw-plus-dify/specs/dify-client-optimization/spec.md

- Source: openspec/changes/optimize-fw-plus-dify/specs/dify-client-optimization/spec.md
- Lines: 1-68
- SHA256: 01a00b811e4e0893e040988fb964437136e51a1ae40e931fdd93f04b0b15a793

```md
## ADDED Requirements

### Requirement: Dify 客户端构建配置行为明确
系统 SHALL 为 Dify app、dataset 和 web console 客户端提供明确、一致、可诊断的构建配置行为，包括服务地址、认证凭据、超时、连接池和连接失败重试配置。

#### Scenario: 使用最小必填参数构建客户端
- **WHEN** 调用方使用 server 与 apiKey 构建 app 或 dataset 客户端
- **THEN** 系统 SHALL 创建对应客户端，并应用默认超时、连接池和重试配置

#### Scenario: 必填参数缺失
- **WHEN** 调用方缺少 server、apiKey、用户名或密码等必填参数
- **THEN** 系统 SHALL 抛出包含缺失配置上下文的异常，不得静默创建不可用客户端

#### Scenario: 服务地址边界格式
- **WHEN** 调用方传入末尾带斜杠、包含基础路径或未包含基础路径的 server
- **THEN** 系统 SHALL 使用明确且一致的规则构建最终请求地址，避免重复斜杠或错误拼接 API 路径

### Requirement: Dify 调用异常可诊断
系统 SHALL 在 Dify HTTP 调用失败、Dify 返回错误响应、响应体不是合法 JSON 或网络连接失败时保留足够诊断信息。

#### Scenario: Dify 返回错误响应
- **WHEN** Dify 服务端返回 HTTP 4xx 或 5xx 响应
- **THEN** 系统 SHALL 抛出 Dify 异常并保留 HTTP 状态码、Dify code、message、params 和原始响应体中可获得的信息

#### Scenario: Dify 返回非 JSON 错误响应
- **WHEN** Dify 服务端返回非 JSON 格式错误响应
- **THEN** 系统 SHALL 抛出可诊断异常，并保留 HTTP 状态码与原始响应体

#### Scenario: 网络连接或超时失败
- **WHEN** 请求因连接失败、读取超时或写入超时失败
- **THEN** 系统 SHALL 抛出客户端异常并保留原始异常链，便于调用方定位网络或配置问题

### Requirement: Dify 响应模型解析稳定
系统 SHALL 对 Dify 常见响应模型提供稳定解析行为，尤其是分页响应、布尔操作响应、应用调用响应和动态 JSON 响应。

#### Scenario: 解析分页响应
- **WHEN** Dify 返回包含 page、limit、total、total_pages、has_more 和 data 的分页响应
- **THEN** 系统 SHALL 正确映射分页字段和数据列表

#### Scenario: 解析动态 JSON 响应
- **WHEN** Dify 返回当前客户端尚未建模的 JSON 字段
- **THEN** 系统 SHALL 保留可访问的原始 JSON 信息，不得因未知字段导致调用失败

### Requirement: Magic-API Dify 模块保持兼容
系统 SHALL 保持 Boot 层 Magic-API `dify` 模块的现有入口兼容，并使脚本侧能够继续通过简洁方法创建 Dify 客户端。

#### Scenario: 创建知识库客户端
- **WHEN** Magic-API 脚本调用 `dify.dataset(server, apiKey)`
- **THEN** 系统 SHALL 返回可用于知识库、文档和检索操作的 dataset 客户端

#### Scenario: 创建应用客户端
- **WHEN** Magic-API 脚本调用 `dify.baseApp(server, apiKey)`、`dify.chatApp(server, apiKey)`、`dify.flowApp(server, apiKey)` 或 `dify.completionApp(server, apiKey)`
- **THEN** 系统 SHALL 返回对应类型的 app 客户端，并保持方法名与参数顺序兼容

#### Scenario: 创建 Web 控制台客户端
- **WHEN** Magic-API 脚本调用 `dify.web(server, userName, password)`
- **THEN** 系统 SHALL 返回已完成认证连接或可明确报告认证失败原因的 web console 客户端

### Requirement: Dify 优化具备可验证交付物
系统 SHALL 为 Dify 客户端优化提供足够的验证手段，包括编译验证、核心行为测试、示例或文档中的至少一种可执行或可审查证据。

#### Scenario: 构建验证
- **WHEN** 完成 Dify 客户端优化实现
- **THEN** 系统 SHALL 能在相关 Maven 模块中通过编译验证

#### Scenario: 核心行为验证
- **WHEN** 验证客户端构建、异常处理、分页解析或 Magic-API 模块入口等核心行为
- **THEN** 系统 SHALL 提供测试、示例或文档说明作为验收证据

```
