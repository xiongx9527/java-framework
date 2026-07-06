## 1. 后端控制台接口

- [x] 1.1 为 `/ws/execute` 增加按订阅脚本 id 加载、模拟上下文执行和错误返回逻辑
- [x] 1.2 为 `/ws/datasource/test` 增加必填字段、`other` JSON 和 token 生成能力校验
- [x] 1.3 补充 `MagicWsController` 单元测试覆盖成功、未知脚本、非法数据源和缺少 token secret 场景

## 2. 脚本上下文兼容

- [x] 2.1 统一 `init` 脚本上下文，同时注入 `type` 与 `businessType`
- [x] 2.2 统一业务消息脚本上下文，同时注入 `type` 与 `businessType`，保留 `params`
- [x] 2.3 统一 `close` 脚本上下文，同时注入 `type` 与 `businessType`
- [x] 2.4 补充或更新 `BaseDynamicWebSocketHandler` 测试验证上下文变量兼容

## 3. 前端控制台插件

- [x] 3.1 修复 `magic-ws-datasource.js` 中旧 `com.jugui` 包名和不存在的 `WsProducer` 引用
- [x] 3.2 调整 WS 数据源代码提示逻辑，使其围绕真实 `WsModule` 与 `ws['key']` 动态模块能力
- [x] 3.3 统一 `magic-ws-subscribe.js` 的 `requirePath` 与后端 `WsSubscribeInfoMagicResourceStorage` 规则
- [x] 3.4 增加静态校验或测试，防止旧包名、Producer 残留和路径规则不一致回归

## 4. 文档与验证

- [x] 4.1 更新 WS 插件 README，说明控制台测试语义、模拟上下文和 `type/businessType` 兼容变量
- [x] 4.2 运行 OpenSpec 校验，确认 delta spec 格式有效
- [x] 4.3 运行 WS 插件相关 Maven 测试，确认变更未破坏现有标准 WebSocket 行为
