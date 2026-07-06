## Why

Magic WebSocket 插件的运行时主链路已经具备标准化基础，但控制台查询、脚本测试和代码提示仍存在遗留实现：管理端测试接口固定返回成功，前端 JavaClass 指向旧包名和不存在的 Producer 类型，脚本上下文变量也不完全一致。现在需要补齐这些能力，避免控制台显示“可测试/可提示”但实际无效，保证 WebSocket 插件从运行时到 MagicAPI 管理端都符合既有标准。

## What Changes

- 修复 `ws-subscribe` 控制台执行测试，使 `/ws/execute` 能按脚本 id 加载订阅脚本并使用模拟上下文执行。
- 修复 `ws-datasource` 控制台测试，使 `/ws/datasource/test` 至少执行真实字段校验、JSON 校验和 token 生成能力校验。
- 修复控制台代码提示配置，使用真实 `WsModule` 类名与模块方法，移除旧 `com.jugui` 包名、`WsProducer` 和 `extra=producer` 遗留逻辑。
- 统一 `ws-subscribe` 前后端资源标准，确保路径要求与后端 storage 一致。
- 统一脚本上下文变量兼容性，在业务、init、close 场景中提供一致的 `type` 与 `businessType` 访问方式。
- 补充关键单元测试，覆盖控制台测试接口、上下文变量兼容与前端配置静态风险。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `standard-websocket-plugin`: 补强 MagicAPI WebSocket 插件的控制台测试、代码提示、资源元数据一致性和脚本上下文标准。

## Impact

- 后端：`fw-plus-boot-autoconfigure` 中 `magicapi/plugin/ws` 的控制器、处理器或相关服务。
- 前端：`fw-plus-magicapi-plugin-ws/src/console` 中数据源与订阅资源插件配置。
- 测试：WS 插件相关 JUnit 单元测试与必要的静态配置校验。
- API：不引入破坏性连接协议变更；`type`/`businessType` 以兼容方式补齐。
- 依赖：不新增外部依赖，不修改 MagicAPI、Spring Boot 或 JWT 版本。
