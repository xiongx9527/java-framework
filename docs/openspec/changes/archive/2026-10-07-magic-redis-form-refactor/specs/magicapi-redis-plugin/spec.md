# Spec Delta

## Purpose

定义 redis datasource 插件控制台动态表单的行为契约：metadata 驱动的字段渲染、必填标记与保存前校验、properties JSON 双向绑定、i18n 文案结构，以及前端元数据与后端字段口径的一致性校验。

## ADDED Requirements

### Requirement: 表单字段由 metadata 元数据驱动渲染

redis datasource 编辑表单 SHALL 根据插件内置的字段元数据描述符渲染，每个字段描述符包含字段名、控件类型与是否必填；控件类型至少支持 text、number、password（敏感字段）、select（枚举）。表单标签 SHALL 优先使用 i18n 翻译，缺失时回退到原始字段名。mode（standalone/sentinel/cluster）与 clientType（lettuce/jedis）等枚举字段的候选项 SHALL 由元数据声明，而非组件内硬编码。

#### Scenario: 按元数据渲染全部字段
- **WHEN** 用户在控制台打开 redis datasource 编辑弹窗
- **THEN** 表单按元数据声明的顺序渲染 name、key、hostname、port、username、password、database、mode、clientType、properties 等字段，label 与控件类型与元数据一致

#### Scenario: 敏感字段渲染为密码框
- **WHEN** 字段在元数据中标记为敏感（如 password）
- **THEN** 该字段以密码框渲染，内容不回显

#### Scenario: 枚举字段以下拉框渲染
- **WHEN** 字段类型为 select（如 mode、clientType）
- **THEN** 该字段渲染为下拉框，候选项来自元数据声明的枚举值

### Requirement: 必填标记与保存前前端校验

必填字段的标签 SHALL 显示必填标记（*）。用户点击保存时，若存在必填字段为空或 properties 不是合法 JSON，控制台 SHALL 弹出明确提示且不得发出保存请求；校验通过后才发出保存请求。控制台的保存前校验 SHALL 为可选钩子：仅当插件 service 定义了校验方法时才执行，未定义该方法的插件保存行为保持不变。

#### Scenario: 必填为空阻止保存
- **WHEN** hostname 或 port 等必填字段为空时用户点击保存
- **THEN** 弹出必填提示，保存请求不发出，弹窗保持打开

#### Scenario: properties 非法 JSON 阻止保存
- **WHEN** properties JSON 编辑框内容不是合法 JSON 时用户点击保存
- **THEN** 弹出 JSON 解析错误提示，保存请求不发出

#### Scenario: 校验通过正常保存
- **WHEN** 全部必填字段非空且 properties 为合法 JSON（或为空）时用户点击保存
- **THEN** 保存请求正常发出，后端校验与既有行为不变

#### Scenario: 未定义校验方法的插件不受影响
- **WHEN** 任一未实现该可选校验钩子的其它 datasource 插件点击保存
- **THEN** 保存流程与本次变更前完全一致，不出现额外提示或行为变化

### Requirement: properties 字段与 JSON 编辑器双向绑定

表单中的 properties SHALL 与后端实体的 properties 映射双向绑定：编辑时 SHALL 将已保存的 properties 映射为格式化的 JSON 文本回显；保存时 SHALL 将 JSON 文本解析回映射后随请求体提交。用户在 JSON 编辑框中的修改 SHALL 能持久化并在重新打开编辑时回显。

#### Scenario: 编辑时回显已有属性
- **WHEN** 用户打开一个已配置 properties 的 redis datasource
- **THEN** JSON 编辑框回显格式化的现有 properties 内容，而非空白

#### Scenario: 保存的属性不丢失
- **WHEN** 用户在 JSON 编辑框输入合法属性并点击保存
- **THEN** 保存请求体包含解析后的 properties 映射，重新打开后内容与保存时一致

### Requirement: 中英文文案结构对齐参照实现

插件 SHALL 提供 zh-cn 与 en 两套对称的 i18n 文案，按 common（通用字段标签）、hint（提示语）、field（字段级标签）、extra（扩展信息）分层组织；两种语言的键集合必须一致。

#### Scenario: 切换语言表单文案完整
- **WHEN** 用户切换控制台语言为中文或英文
- **THEN** 表单标签、必填提示、枚举展示名与 JSON 编辑提示均显示对应语言文案，无缺键回退到原始字段名的情况

### Requirement: 前端元数据与后端字段口径交叉校验

插件 SHALL 提供可执行的自动化测试（`npm test`），交叉校验前端字段元数据与后端权威口径一致：元数据中的字段键必须能映射到后端实体 `RedisDataSourceInfo` 的字段，必填标记必须与后端必填校验集合（name/key/hostname/port）一致。当任一侧发生漂移时该测试 MUST 失败。测试与构建脚本 SHALL 在 `npm run build` 之外独立提供。

#### Scenario: 口径一致时测试通过
- **WHEN** 执行插件 console 目录的 `npm test`
- **THEN** 测试通过，确认元数据字段与后端字段、必填集合一致

#### Scenario: 字段漂移时测试失败
- **WHEN** 后端实体新增/删除字段或必填集合变化，而前端元数据未同步
- **THEN** `npm test` 失败并指出不一致的字段
