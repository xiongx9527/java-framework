# proposal: mcp-toolprovider-group-select

## 问题描述

MCP 服务实例（`fw-plus-ai-magicapi-plugin-mcp-server`）与 langchain4j toolprovider 工具集
（`fw-plus-ai-magicapi-plugin-langchain4j-tool-provider`）两个控制台表单中，`函数` 与 `api`
两个区块暴露了过多手填筛选字段：

- 函数：includeGroups / excludeGroups（仅 toolprovider）/ includePaths / excludePaths
- api：methods / includeGroups / excludeGroups（仅 toolprovider）/ includePaths / excludePaths

其中「包含组」为逗号分隔的自由文本输入，用户需要手填组路径，易错且不可发现。

## 根因

表单按后端 Scope 结构 1:1 平铺了全部筛选字段，未针对实际使用方式收敛；
「包含组」未利用控制台已有的资源树数据（`inject('resources')`，节点含组路径）做成选择控件。

## 修复目标

1. 两个插件的 `函数` 与 `api` 区块**只保留「包含组」**一个筛选字段（methods、includePaths、
   excludePaths、excludeGroups 从表单移除；api 的 requireOptIn 开关与「包含组」无关，保留）
2. 「包含组」必须是**下拉选择**：下拉面板内以复选框列出对应类型（function / api）的分组，
   值为组路径（与后端 `matchesIncludeGroup` 的 `getGroupPath` 匹配语义一致）
3. 纯前端控制台改动：后端 Scope 结构与匹配逻辑不变（旧数据兼容，多余字段仍可被后端读取）
