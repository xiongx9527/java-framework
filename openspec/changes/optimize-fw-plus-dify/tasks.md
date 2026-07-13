## 1. 现状梳理与范围确认

- [x] 1.1 梳理 `fw-plus-dify` 的 app、dataset、web console 客户端构建链路和现有 public API。
- [x] 1.2 梳理 Boot 层 Magic-API `DifyModule` 入口，确认需要保持兼容的方法名、参数顺序和返回类型语义。
- [x] 1.3 明确本次实现不覆盖的 Dify API 范围，避免扩展为全量 SDK 重写。

## 2. 客户端构建与配置优化

- [x] 2.1 优化 app、dataset、web console 客户端构建时的必填参数校验和错误信息。
- [x] 2.2 明确并实现 server 地址规范化规则，覆盖末尾斜杠、已有基础路径和 route 拼接边界。
- [ ] 2.3 检查 `ApiConfig` 中 timeout、连接池和重试配置是否被关键客户端正确应用，必要时补齐传递链路。

## 3. 异常与响应模型优化

- [x] 3.1 优化 Dify HTTP 错误响应处理，保留 HTTP 状态、Dify code、message、params 和原始响应体。
- [x] 3.2 优化非 JSON 错误响应、空响应体和网络异常的诊断信息，保留原始异常链。
- [x] 3.3 验证分页响应、动态 JSON 响应和布尔操作响应的解析行为，必要时调整模型或解析逻辑。

## 4. Magic-API 兼容性保障

- [x] 4.1 确认 `dify.web(server, userName, password)`、`dify.dataset(server, apiKey)` 和各 app 入口保持兼容。
- [x] 4.2 如需新增配置能力，优先通过新增重载或新增方法实现，不改变现有入口语义。
- [x] 4.3 补充或更新 Magic-API 使用说明/示例，说明 Dify 客户端创建与错误处理行为。

## 5. 验证与收尾

- [ ] 5.1 为客户端构建、server 规范化、异常处理或分页解析补充测试或可审查示例。
- [ ] 5.2 在 `fw-plus-core` 中编译验证 `fw-plus-dify` 相关模块。
- [ ] 5.3 如 Boot 层入口受影响，在 `fw-plus-boot` 中编译验证相关自动配置模块。
- [ ] 5.4 对照 `dify-client-optimization` 规范逐项确认验收场景已覆盖。
