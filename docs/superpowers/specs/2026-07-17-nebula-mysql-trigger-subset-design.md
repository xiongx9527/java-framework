---
comet_change: add-nebula-mysql-trigger-subset
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-18-add-nebula-mysql-trigger-subset
status: final
---

# Nebula MySQL Trigger 子集技术设计

## 1. 背景与目标

本 change 为 Nebula MySQL 协议深度兼容路线增加网关侧 trigger 子集。OpenSpec 已明确：trigger 只对经 MySQL 网关执行、且网关能够可靠观察的 INSERT、UPDATE、DELETE 生效；绕过网关的原生 Nebula 写入不会被捕获，也不应被伪装成可触发。

当前代码中稳定的 Nebula 执行路径仍以 nGQL JDBC 直连为主：`NebulaStatementImpl` / `NebulaPreparedStatementImpl` 调用 `NebulaSqlExecutor`，默认 `DirectNebulaSqlExecutor` 只做轻量 query/update 分类并调用 `NebulaConnection.execute`。相邻设计中，MySQL 只读网关定位为独立协议层，SQL parser、SQL 查询转换、SQL 写入/DDL 转换是前置能力。本设计必须消费这些边界，不把 trigger 语义塞入现有 nGQL 直连 JDBC executor。

目标：

- 支持受控 CREATE TRIGGER、DROP TRIGGER 元数据管理。
- 支持经网关执行 INSERT、UPDATE、DELETE 时触发 BEFORE/AFTER trigger。
- trigger body 复用 SQL 到 nGQL 写入能力。
- 定义执行顺序、失败策略、递归保护和不支持语法边界。
- 保持现有 nGQL JDBC 直连路径不变。

非目标：

- 不实现完整 MySQL trigger 引擎。
- 不捕获绕过 MySQL 网关的 Nebula 原生写入。
- 不承诺 MySQL 事务、锁、回滚和行级 OLD/NEW 语义完全等价。
- 不实现 binlog、复制、外部 CDC、事件调度或 routine 引擎。
- 不在本 change 中实现 Spring Boot starter 或 Magic-API 插件入口。

## 2. 推荐方案

采用“网关写入执行链路 trigger-aware executor”方案。

```text
COM_QUERY / COM_STMT_EXECUTE write SQL
  -> MySQL gateway command handler
  -> SQL parser
  -> SQL write/DDL execution port
  -> TriggerAwareWriteExecutor
      -> load BEFORE triggers
      -> execute supported trigger body SQL
      -> execute primary write
      -> load AFTER triggers
      -> execute supported trigger body SQL
  -> MySQL OK / ERR response
```

CREATE TRIGGER 和 DROP TRIGGER 由网关 DDL handler 截获并管理 metadata，不转成 Nebula 原生 schema DDL。普通 INSERT、UPDATE、DELETE 在进入写入执行端口后，由 trigger-aware executor 基于解析后的写入计划确定目标表、事件类型和 schema，再查找匹配 trigger。

不采用底层 `NebulaSqlExecutor` 包装方案。底层 executor 只能看到 nGQL 或尚未结构化的文本，无法可靠区分网关 SQL、原生 nGQL、目标表、OLD/NEW 语义和事件类型，容易误触发现有 JDBC 直连路径。

不采用外部 CDC/日志补偿方案。它无法提供 BEFORE 语义，也会扩大到 OpenSpec 明确排除的绕过网关写入和复制/binlog 方向。

## 3. 模块与组件

### 3.1 `TriggerDefinition`

trigger 元数据建议作为网关 metadata 管理，字段包括：

- `schemaName`：MySQL database / Nebula graphspace 映射后的逻辑 schema。
- `triggerName`：schema 内唯一。
- `targetTable`：SQL 映射表名。
- `event`：`INSERT`、`UPDATE`、`DELETE`。
- `timing`：`BEFORE`、`AFTER`。
- `bodySql`：受控 SQL body。第一阶段只支持单条或明确可拆分的顺序 SQL，具体由 parser/routine 能力边界决定。
- `bodyKind`：普通 SQL body、未来 routine 引用等扩展类型；第一阶段固定为 SQL。
- `order`：同一 table/event/timing 下的稳定执行顺序。若 MySQL 兼容语法未显式支持 `FOLLOWS`/`PRECEDES`，按创建时间和名称排序。
- `enabled`：是否启用。
- `createdAt`、`updatedAt`、`version`：迁移、审计和并发更新使用。

元数据存储必须可替换。最小实现可先提供文件或内存实现用于测试，但生产入口应通过 `TriggerMetadataStore` 接口接入可持久化存储，避免把存储细节绑定到协议处理器。

### 3.2 `TriggerMetadataService`

职责：

- 校验 CREATE TRIGGER / DROP TRIGGER 支持子集。
- 保证 `(schemaName, triggerName)` 唯一。
- 校验 target table 可由 schema mapping 识别。
- 保存、删除、查询 trigger 定义。
- 按 `(schemaName, targetTable, event, timing)` 返回稳定排序后的 trigger 列表。

DROP 语义：

- `DROP TRIGGER name` 找不到定义时返回明确错误。
- `DROP TRIGGER IF EXISTS name` 找不到定义时返回成功或 warning，具体 warning 是否暴露由 MySQL 网关统一错误/OK 模型决定。

### 3.3 `TriggerDdlHandler`

职责：

- 在 MySQL 网关 SQL 分发层识别 CREATE TRIGGER 和 DROP TRIGGER。
- 调用 parser core 或 trigger 专用 parser adapter 生成 `TriggerDefinitionDraft`。
- 拒绝 OpenSpec 外语法，例如完整 MySQL delimiter 脚本、复杂 compound statement、动态 SQL、游标、handler、事务语句、临时表、CALL 和嵌套 CREATE/DROP TRIGGER。
- 将成功或失败转换为 MySQL 风格 OK / ERR 响应。

该 handler 不执行 Nebula 原生 DDL，也不把 trigger 定义写入 Nebula tag/edge schema。

### 3.4 `TriggerAwareWriteExecutor`

职责：

- 只包裹 MySQL 网关的 SQL 写入执行端口。
- 接收结构化写入计划，而不是重新解析 raw SQL。
- 基于写入计划提取 schema、target table、event 和可选 OLD/NEW 上下文。
- 执行 BEFORE triggers、主写入、AFTER triggers。
- 维护触发链、递归深度和失败策略。

建议接口形态：

```java
public interface TriggerAwareWriteExecutor {
	WriteResult execute(WriteExecutionRequest request) throws SQLException;
}
```

`WriteExecutionRequest` 至少包含：

- session context：用户、schema、连接配置、权限上下文。
- write plan：SQL 写入/DDL 转换层生成的结构化计划。
- bindings：prepared statement 参数绑定视图。
- trigger context：当前触发链，顶层写入为空。

### 3.5 `TriggerExecutionContext`

职责：

- 保存当前深度、最大深度、触发链和顶层请求 ID。
- 记录当前 trigger、父 trigger、目标表和事件。
- 暴露 `isTriggeredBy(triggerId)` 或等价能力，用于错误诊断和递归保护。
- 为日志和错误返回提供脱敏 trace id。

默认最大递归深度建议为 8，可配置。达到限制时，当前 trigger body 中止，并向客户端返回明确错误，例如“Trigger recursion depth exceeded”。

### 3.6 `TriggerBodyExecutor`

职责：

- 将 trigger body SQL 作为受控 SQL 片段提交给 SQL 写入执行端口。
- 禁止 body 中使用 CREATE TRIGGER、DROP TRIGGER、CALL、事务语句、动态 SQL、游标、handler、临时表和未支持的多结果集语义。
- 按当前 `TriggerExecutionContext` 继续递归计数，而不是开启新的顶层执行。
- 将 parser、converter、权限和 Nebula 执行错误转换为 trigger 失败。

body SQL 复用现有 SQL 到 nGQL 写入能力，但必须走 trigger-aware executor，以便 body 写入也能触发下一级 trigger 并受递归限制约束。

## 4. 支持语法子集

### 4.1 CREATE TRIGGER

第一阶段支持形态：

```sql
CREATE TRIGGER trigger_name
BEFORE INSERT ON table_name
FOR EACH ROW
<single supported SQL body>
```

事件支持：

- `INSERT`
- `UPDATE`
- `DELETE`

时机支持：

- `BEFORE`
- `AFTER`

约束：

- 必须指定 `FOR EACH ROW`，但实际语义是“网关写入计划可观察范围内的行级兼容子集”。
- 不支持 `FOLLOWS` / `PRECEDES` 时，按创建顺序和名称提供稳定顺序。
- 不支持复杂 compound statement 时，`BEGIN ... END` 应返回明确不支持错误，或仅在 routine 子集已提供稳定顺序 SQL interpreter 后接入。
- 不支持跨 schema target table。

### 4.2 DROP TRIGGER

支持：

```sql
DROP TRIGGER trigger_name
DROP TRIGGER IF EXISTS trigger_name
```

schema 解析遵循网关 session database / graphspace。跨 schema drop 若没有明确设计，第一阶段拒绝。

## 5. 执行语义

### 5.1 BEFORE

```text
write request
  -> load BEFORE triggers
  -> execute each BEFORE trigger body in stable order
  -> if all BEFORE succeeded, execute primary write
```

任一 BEFORE trigger 失败：

- 主写入不执行。
- 后续 BEFORE trigger 不执行。
- AFTER trigger 不执行。
- 客户端收到明确错误。

### 5.2 AFTER

```text
primary write succeeds
  -> load AFTER triggers
  -> execute each AFTER trigger body in stable order
```

主写入失败时不执行 AFTER trigger。

AFTER trigger 失败时，客户端收到错误，但系统不承诺回滚已成功的主写入或已成功的前序 AFTER trigger。错误消息和文档必须明确该边界，避免暗示 MySQL 事务等价。

### 5.3 OLD / NEW

第一阶段提供受限 OLD/NEW 语义：

- INSERT：可暴露 NEW。
- DELETE：可暴露 OLD。
- UPDATE：可暴露 OLD 和 NEW。

如果 SQL 写入计划可以安全提供受影响记录的值，则 trigger body 可以引用对应上下文。若无法提供，例如 UPDATE/DELETE 只转换为 Nebula 条件写入且无法可靠获取修改前数据，则依赖 OLD/NEW 的 trigger 创建或执行必须返回明确不支持错误。

不通过额外不受控查询推测 OLD/NEW。需要查询补全时，必须由写入计划明确声明可安全获取，并在测试中覆盖一致性边界。

## 6. 失败与错误映射

建议错误分类：

| 场景 | 建议 SQLState | 说明 |
|------|---------------|------|
| CREATE TRIGGER 语法不支持 | 42000 | MySQL 语法或子集不支持 |
| trigger 已存在 | 42000 | schema 内名称冲突 |
| DROP 不存在 trigger | 42000 | 非 IF EXISTS 路径返回明确错误 |
| body 语法不支持 | 42000 | body 包含非目标语法 |
| BEFORE trigger 失败 | HY000 | 主写入未执行 |
| 主写入失败 | HY000 | AFTER 未执行 |
| AFTER trigger 失败 | HY000 | 主写入可能已成功 |
| 递归深度超限 | HY000 | 触发链中止 |

客户端错误消息应说明这是 Nebula MySQL 网关 trigger 子集错误，不应伪装为完整 MySQL Server 行为。日志可以记录 trigger id、trace id、schema、table、event 和 timing，但不得记录敏感连接串或未脱敏参数值。

## 7. 权限与安全

第一阶段不实现完整 MySQL 权限系统，但必须保留权限钩子：

- CREATE/DROP TRIGGER 需要 metadata DDL 权限。
- 触发器执行 body 时使用定义者权限还是调用者权限必须由配置固定；推荐第一阶段使用调用者上下文，避免引入 definer 权限模型。
- body SQL 仍需通过 SQL 写入端口的权限、schema mapping 和不支持语义校验。

若后续 MySQL gateway advanced change 提供更完整权限体系，本设计的 metadata service 和 body executor 应通过 session context 接入，而不是直接读取全局配置。

## 8. 与相邻 Change 的边界

`add-nebula-sql-parser-core`：

- 提供 SQL / trigger DDL 解析基础。
- trigger 设计不直接依赖 Druid AST 类型作为公共 API。

`add-nebula-sql-write-and-ddl-support`：

- 提供 INSERT、UPDATE、DELETE 和基础 DDL 到 nGQL 的转换与执行端口。
- trigger body 复用该端口。
- 写入计划需暴露 target table、event、参数绑定和可选 OLD/NEW 上下文能力。

`add-nebula-mysql-protocol-write-and-advanced-support`：

- 提供 MySQL 网关写入、DDL、prepared statement 和安全策略基础。
- trigger 只接入该网关可观察写入路径。

`add-nebula-mysql-routine-subset`：

- routine interpreter 若存在，可作为未来 trigger body 的 compound statement 执行引擎。
- 本 change 不假设 routine 已实现，也不通过 CALL 执行 trigger body。

现有 `jdbc:nebula://...` nGQL 直连：

- 不触发 trigger。
- 不修改 `NebulaStatementImpl`、`NebulaPreparedStatementImpl` 和 `DirectNebulaSqlExecutor` 的既有语义，除非后续 build 阶段有明确、独立且测试覆盖的适配层。

## 9. 测试策略

### 9.1 Metadata 与 DDL 测试

- CREATE TRIGGER 保存定义。
- 重复 CREATE 返回明确错误。
- DROP TRIGGER 删除定义。
- DROP TRIGGER 不存在返回明确错误。
- DROP TRIGGER IF EXISTS 不存在走幂等路径。
- 不支持语法如动态 SQL、CALL、事务语句、复杂 compound body 返回明确错误。

### 9.2 执行顺序测试

- INSERT 触发 BEFORE 和 AFTER。
- UPDATE 触发 BEFORE 和 AFTER。
- DELETE 触发 BEFORE 和 AFTER。
- 无匹配 trigger 时只执行主写入。
- 多个同类 trigger 按稳定顺序执行。

测试应使用 fake write executor 和 metadata store，不依赖真实 NebulaGraph。

### 9.3 失败策略测试

- BEFORE trigger 失败时主写入不执行。
- 主写入失败时 AFTER trigger 不执行。
- AFTER trigger 失败时返回错误，并记录主写入已成功的边界。
- body 内 converter 不支持或 Nebula 执行失败时返回 trigger 执行错误。

### 9.4 递归与边界测试

- 同表 trigger body 写回同表达到最大深度后中止。
- A 表 trigger 写 B 表、B 表 trigger 写 A 表达到最大深度后中止。
- 绕过 MySQL 网关直接调用 `NebulaConnection.execute(nGQL)` 不触发 trigger。
- nGQL JDBC 直连现有 statement/prepared statement 测试继续通过。

## 10. Build 阶段建议顺序

1. 定义 trigger metadata 模型、store 接口和 in-memory 测试实现。
2. 定义 CREATE/DROP TRIGGER parser/handler 的支持子集和错误模型。
3. 定义 trigger-aware write executor 接口和 fake write executor 测试夹具。
4. 实现 BEFORE/AFTER 执行顺序和失败策略。
5. 实现 trigger body executor 与递归保护。
6. 补齐网关写入路径接入和 MySQL 错误映射。
7. 补充边界文档，明确绕过网关写入不会触发 trigger。

整个 build 阶段不得把 trigger 接到现有 nGQL JDBC 直连路径，也不得扩大到 binlog、复制、routine 完整解释器或完整 MySQL 事务语义。
