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

### Decision 5: Magic-API 模块只做兼容增强，不做脚本语义变更

`DifyModule` 作为 Magic-API 脚本入口，应继续提供：

- `web(server, userName, password)`
- `dataset(server, apiKey)`
- `baseApp(server, apiKey)`
- `chatApp(server, apiKey)`
- `flowApp(server, apiKey)`
- `completionApp(server, apiKey)`

如需新增能力，应新增方法或重载，不应改变已有方法名、参数顺序和返回类型语义。

替代方案：将 Magic-API 入口调整为统一 `dify.client(type, config)`。该方案更抽象，但会破坏现有脚本认知和兼容性，因此不采用。

## Risks / Trade-offs

- [Risk] Dify 官方 API 版本差异导致部分行为难以一次性固定。→ Mitigation：优先规范客户端内部稳定行为，对版本相关响应保持宽松解析。
- [Risk] 增强异常类型可能影响调用方捕获异常的逻辑。→ Mitigation：尽量保留现有异常类型层级或兼容父类型，只增强信息，不强制调用方迁移。
- [Risk] server 规范化规则若处理不当，可能改变已有传参的最终 URL。→ Mitigation：增加边界场景验证，覆盖末尾斜杠、已有基础路径和普通 server。
- [Risk] 覆盖面扩大到所有 Dify API 会导致任务失控。→ Mitigation：本 change 聚焦客户端基础行为和核心入口，不承诺补齐 Dify 全量 API。
- [Risk] Magic-API 入口运行时依赖真实 Dify 服务，难以做完全端到端验证。→ Mitigation：至少通过构建、单元测试或示例说明验证入口兼容性，真实服务验证作为可选增强。

## Migration Plan

1. 在 Core 层完成 Dify 客户端基础行为优化。
2. 保持现有 public 方法签名兼容，必要时新增重载或配置对象能力。
3. 编译验证 `fw-plus-dify` 所在 Maven 模块。
4. 如 Boot 层 `DifyModule` 受影响，验证 Boot 相关模块编译。
5. 如发现兼容性风险，优先回退新增行为或增加兼容分支，不删除现有入口。

## Open Questions

- 是否需要严格对齐某个 Dify 官方版本的 API 文档？
- 是否需要新增 Magic-API 脚本示例文件，还是以 Java 测试和文档说明为主？
- 是否要求将 `ApiConfig` 中的 timeout/连接池配置完整传递到 app、dataset、web console 各客户端？
- 是否需要为 Web console 客户端的登录与 refresh token 行为建立更明确的刷新策略？
