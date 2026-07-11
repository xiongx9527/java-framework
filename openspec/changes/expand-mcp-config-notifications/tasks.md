## 1. 配置属性影响面确认

- [ ] 1.1 对照 `MagicAPIMcpProperties` 梳理当前 `magic-api.plugin.mcp.*` 属性树，标记已覆盖、候选新增和明确忽略的 key 前缀。
- [ ] 1.2 追踪候选新增 key 在 lifecycle、transport/session、logging、completion、provider/handler 中的使用点，确认是否影响客户端可见能力视图或交互语义。
- [ ] 1.3 形成最终 key → notification method 映射清单，并记录不纳入通知的 MCP key 理由。

## 2. 通知器映射扩展

- [x] 2.1 为新增覆盖 key 编写 `McpConfigurationChangeNotifierTest` 失败用例，覆盖对应 tools/resources/prompts 或能力视图刷新通知。
- [ ] 2.2 扩展 `McpConfigurationChangeNotifier` 的 key 前缀识别和通知映射，保持 Java 8 兼容和确定性顺序。
- [ ] 2.3 补充同批刷新内新增 key 与既有 key 混合时的去重测试。
- [ ] 2.4 补充未纳入 MCP key 和非 MCP key 不触发通知的回归测试。

## 3. 自动配置与兼容验证

- [ ] 3.1 运行 `McpConfigurationChangeNotifierTest`，确认新增映射、去重和忽略逻辑通过。
- [ ] 3.2 运行 `MagicAPIMcpConfigurationTest` 和 `ConfigMagicDynamicRegistryTest`，确认 MCP disabled/absent 与 MagicAPI config 通用事件链路不回归。
- [ ] 3.3 运行 Boot autoconfigure 模块中 MCP/config 相关定向测试，确认现有通知、集群广播和 lifecycle 行为未回归。
