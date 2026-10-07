# design: mcp-toolprovider-group-select

## 方案

纯前端控制台改动（2 个插件 console，各自独立构建）：

1. **每组表单内新增本地组件 `magic-group-select.vue`**（两插件各自持有一份，互不共享构建产物）：
   - 展示形态仿 `magic-input`：只读框显示已选组路径（逗号分隔），点击弹出下拉面板
   - 下拉面板内为分组复选框列表；数据来自 `inject('resources')()`，按 `type`
     （`function` / `api`）取对应资源树，递归收集 `folder === true` 节点的 `{name, path}`
   - 勾选即写入 `value`（`string[]` 组路径），`v-model:value` 双向绑定到
     `info.tools.functions.includeGroups` / `info.tools.api.includeGroups`
   - 面板为 `position: fixed` 浮层，点击外部关闭（复用 magic-select 的交互样式与 CSS 变量）
2. **表单字段收敛**（`magic-mcp-server-info.vue`、`magic-langchain4j-tool-provider-info.vue`）：
   - 函数区：删除 includePaths / excludePaths / excludeGroups 行，仅留「包含组」下拉
   - api 区：删除 methods / includePaths / excludePaths / excludeGroups 行，仅留「包含组」下拉
     与 requireOptIn 开关（mcp 的 optInOptionName 随 requireOptIn 联动，保留）
   - 对应 listField computed 与 i18n 键同步清理
3. **不做后端改动**：`MagicApiMcpProperties.Scope` / `Langchain4jToolProperties.Scope` 结构与
   `matchesIncludeGroup`（按组路径通配匹配）保持不变；旧实例配置中的 methods/paths 字段
   仍被后端读取，行为兼容

## 关键依据

- `magic-main.vue` 已 `provide('resources')`，插件表单与主面板同处一个 Vue 应用，
  `inject('resources')` 可直接取得已加载资源树（节点含 `path`），无需新增后端接口
- 后端 `matchesIncludeGroup`：`includeGroups` 与 `magicResourceService.getGroupPath(groupId)`
  匹配 → 下拉值必须是**组路径**而非组名
- `magic-select` 为单选组件，无法承载多选，故每组表单内自建轻量多选下拉
