## ADDED Requirements

### Requirement: Authorization-aware metadata and SQL pipeline
系统 SHALL 在元数据加载、Prompt 构建、SQL 插件链、最终 SQL 校验和只读执行阶段使用同一治理授权摘要。插件 MAY 收窄 SQL 文本或授权对象，但 MUST NOT 清空、扩大、替换项目/操作人上下文、参数、授权对象或行列权限摘要。

#### Scenario: Filter metadata by authorized objects
- **WHEN** AskData 为当前请求构建 Prompt 元数据
- **THEN** Flow SHALL 不可覆盖地只保留授权摘要允许的数据源、表和列；无法判定对象归属的术语、样例和 Prompt 片段 SHALL 默认丢弃，不得向模型暴露未授权对象

#### Scenario: Preserve authorization through SQL plugins
- **WHEN** SQL 插件链改写生成 SQL
- **THEN** 插件结果 SHALL 保留原始项目上下文、参数和授权摘要；任何扩大授权对象、移除行级谓词或替换操作人边界的结果 SHALL 被拒绝

#### Scenario: Revalidate SQL against authorization summary
- **WHEN** 所有 SQL 插件完成后准备执行最终 SQL
- **THEN** SQL 校验器 SHALL 基于授权摘要重新检查引用表、列、系统 schema、行级谓词和只读限制，未授权引用 SHALL 导致安全校验失败

#### Scenario: Inject row predicates with parameters
- **WHEN** 业务治理桥接提供行级数据范围条件
- **THEN** SQL 管线 SHALL 将受控 SQL 片段注入匹配表查询、与已有条件以 `AND` 合并，并从最终命令绑定具名参数；同名参数冲突、解析失败或谓词无法安全落实时 SHALL fail-closed

#### Scenario: Reject unauthorized columns after model or plugin rewrite
- **WHEN** 模型或 SQL 插件生成只引用授权表但包含未授权列或受限通配列的 SQL
- **THEN** 最终校验 SHALL 解析列引用并拒绝该 SQL；受限列授权下无法安全解析或归属的 SQL SHALL fail-closed
