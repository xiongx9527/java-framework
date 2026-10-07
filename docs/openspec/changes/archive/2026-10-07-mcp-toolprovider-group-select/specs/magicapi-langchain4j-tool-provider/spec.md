# magicapi-langchain4j-tool-provider 变更 delta

## ADDED Requirements

### Requirement: 工具集表单的函数与 api 范围字段收敛

langchain4j toolprovider 工具集控制台表单中，`函数` 与 `api` 两个区块的范围筛选 SHALL 仅保留
「包含组」一个字段，且「包含组」SHALL 以下拉多选形式呈现（选项为对应类型的 magic 分组，
值为组路径），不得再以自由文本形式暴露 methods、includePaths、excludePaths、excludeGroups
等其余筛选字段。

#### Scenario: 函数区块仅剩包含组下拉

- **WHEN** 用户在工具集表单中查看 `函数` 区块
- **THEN** 该区块仅包含「启用」开关与「包含组」下拉多选，无路径/排除组/排除路径等文本输入

#### Scenario: api 区块仅剩包含组下拉

- **WHEN** 用户在工具集表单中查看 `api` 区块
- **THEN** 该区块仅包含「启用」开关、「包含组」下拉多选与 requireOptIn 开关，
  无 methods/路径等文本输入

#### Scenario: 包含组下拉选项来自资源分组

- **WHEN** 用户展开「包含组」下拉
- **THEN** 面板内以复选框列出 magic 控制台当前资源树中对应类型（函数 / api）的分组，
  选中项以组路径写入实例配置的 includeGroups
