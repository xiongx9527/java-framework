# tasks: mcp-toolprovider-group-select

- [x] 1. mcp-server 控制台：新增 `magic-group-select.vue` 多选下拉组件（数据源 inject('resources')，值=组路径数组）
- [x] 2. mcp-server 控制台：`magic-mcp-server-info.vue` 函数/api 区块字段收敛（只留包含组下拉 + requireOptIn），i18n 同步清理
- [x] 3. mcp-server 控制台：npm build 重建 dist
- [x] 4. tool-provider 控制台：新增 `magic-group-select.vue` 多选下拉组件
- [x] 5. tool-provider 控制台：`magic-langchain4j-tool-provider-info.vue` 函数/api 区块字段收敛（只留包含组下拉 + requireOptIn），i18n 同步清理
- [x] 6. tool-provider 控制台：npm build 重建 dist
- [x] 7. 回归验证：两插件 dist 构建通过；表单渲染与已有实例数据回显正常（静态检查 + 构建证据）
