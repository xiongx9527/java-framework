## Context

`fw-plus-nebula-jdbc` 是 `fw-plus-nebula` 模块当前唯一子模块，用于将 NebulaGraph Java client 包装为 JDBC 风格接口。现有实现已经提供 `Driver`、`Connection`、`Statement`、`PreparedStatement`、`ResultSet` 和 metadata 基础类，但存在以下稳定性问题：

- `NebulaConnection` 解析 JDBC URL 后保存到 `this.properties`，但部分后续逻辑仍使用传入的原始 `Properties`，可能导致 URL 中解析出的 `graphspace`、认证信息或连接池参数未生效。
- 连接池参数通过强制 `(int)` 转换读取，而 `Properties` 的值通常是字符串，容易在运行时触发类型转换异常。
- 资源关闭语义不够稳定，重复关闭连接可能抛出异常，不利于常见 JDBC 调用模式和 try-with-resources 场景。
- `Statement` 通过简单空格拆分判断查询或更新语句，对换行、多空白和大小写混合语句不够稳健。
- `ResultSet` 游标和 `wasNull()` 边界行为可能产生非预期异常或 NPE。
- 关键行为缺少不依赖真实 NebulaGraph 服务的单元测试。

本 change 只稳定现有 JDBC 基础层，为后续 SQL 转 nGQL 和 MySQL 协议网关提供可验证的基础。

## Goals / Non-Goals

**Goals:**

- 保持现有 `com.vesoft.nebula.jdbc` 包名和主要 JDBC API 兼容。
- 统一使用解析后的连接配置，确保 JDBC URL、query 参数和传入 `Properties` 的合并结果进入连接逻辑。
- 为连接池整数参数提供集中、安全的解析逻辑，并对非法值给出明确 `SQLException`。
- 改善资源关闭和边界行为，使其更接近 JDBC 常见预期并避免非预期 NPE。
- 改善 nGQL 语句分类逻辑，使 `executeQuery` / `executeUpdate` 对常见格式更稳定。
- 补充不依赖真实 NebulaGraph 服务的单元测试覆盖关键稳定性场景。

**Non-Goals:**

- 不实现 SQL 转 nGQL。
- 不实现 MySQL wire protocol 或独立服务端。
- 不改动 Boot、Cloud、AI、Product 层。
- 不要求连接真实 NebulaGraph 服务进行集成测试。
- 不默认升级 Nebula Java client 大版本。
- 不追求完整 JDBC 规范覆盖，只修复本模块基础稳定性和后续扩展所需的关键行为。

## Decisions

### 1. 以解析后的连接配置作为唯一连接输入

连接初始化应先调用 `NebulaJdbcUrlParser.parse(url, properties)` 得到合并配置，然后后续 `graphspace`、用户名、密码、连接池参数等均从该合并配置读取。

**理由：** 当前代码中解析结果和原始参数混用，容易造成 URL query 参数被忽略。统一配置来源可以减少歧义，也便于单元测试。

**备选方案：** 保持原逻辑，只修复单个字段读取。该方案改动小，但未来新增参数时仍容易重复出现混用问题，因此不采用。

### 2. 为连接池数值参数提供集中解析方法

连接池参数应通过一个小型工具方法从 `Properties` 中读取，支持 `Number` 和 `String` 两类常见值；为空时使用默认值；无法解析时抛出带参数名的 `SQLException`。

**理由：** `Properties` 常以字符串存储值，直接 `(int)` 转换不安全。集中解析可以统一默认值、错误消息和测试覆盖。

**备选方案：** 在每个参数读取处分别处理。该方案重复代码多，错误信息不一致，因此不采用。

### 3. 资源关闭保持稳定且尽量幂等

`Connection.close()`、`Statement.close()`、`ResultSet.close()` 应允许重复调用不产生非预期失败；关闭过程中对底层 session/pool 的释放需保留必要保护。

**理由：** JDBC 使用者常通过 try-with-resources 或 finally 重复触发关闭。重复关闭抛错会放大资源清理路径的风险。

**备选方案：** 严格保持当前第二次关闭抛异常。该行为与常见 JDBC 经验不符，也不利于上层协议网关复用，因此不采用。

### 4. 语句分类只做轻量规范化，不引入 SQL/nGQL parser

`executeQuery` / `executeUpdate` 的分类逻辑应在现有关键字集合基础上，对输入做 trim、小写化和连续空白规范化，支持换行、多空白和大小写混合；暂不引入完整 parser。

**理由：** 本 change 的目标是稳定现有 nGQL 执行层，而 SQL 转 nGQL 会在后续独立 change 中处理。轻量规范化能解决当前明显问题，避免扩大范围。

**备选方案：** 引入完整 nGQL parser 或 SQL parser。该方案超出本 change 范围，也会与后续 SQL 转换 change 重叠，因此不采用。

### 5. 单元测试优先覆盖纯逻辑和可隔离边界

测试应优先覆盖 URL parser、配置转换、语句分类、资源关闭和 ResultSet 边界行为；对真实 Nebula session/pool 的交互不作为本 change 的强制集成测试。

**理由：** 当前未知真实 NebulaGraph 测试环境，纯逻辑测试更稳定，也能作为后续重构安全网。

**备选方案：** 只做人工验证或要求真实 NebulaGraph 环境。该方案无法稳定在本地和 CI 复现，因此不采用。

## Risks / Trade-offs

- **[Risk] 资源关闭语义调整可能改变少数调用方对重复关闭异常的依赖。** → Mitigation：重复关闭幂等是更常见的 JDBC 预期，且不改变首次关闭释放资源的行为。
- **[Risk] 语句分类仍不是完整 nGQL 语法识别。** → Mitigation：明确限制为轻量规范化；复杂 SQL/nGQL 转换留给后续 `add-sql-to-ngql-translator`。
- **[Risk] 不做真实 NebulaGraph 集成测试可能遗漏 client 交互问题。** → Mitigation：本 change 聚焦可本地验证的稳定性问题；后续可在 verify 阶段或单独 change 增加集成测试 profile。
- **[Risk] 连接配置解析修复可能暴露原先被忽略的非法参数。** → Mitigation：非法数值给出明确异常，便于定位配置错误。

## Migration Plan

1. 在 `fw-plus-nebula-jdbc` 内补充单元测试，先刻画 URL 解析、连接池参数、语句分类和边界行为。
2. 修复连接配置使用路径和连接池参数解析。
3. 调整资源关闭和 ResultSet 边界行为。
4. 运行 `fw-plus-nebula-jdbc` 模块测试，确认不依赖真实 NebulaGraph 服务即可验证新增行为。
5. 若发现生产连接串中存在非法参数，按新异常消息修正配置。

Rollback 策略：若修复引入兼容性问题，可回退到本 change 前实现；新增测试保留为行为对照，后续再按具体兼容需求调整。

## Open Questions

- 是否存在必须兼容的生产 JDBC URL 样例？当前设计假设维持现有 URL 格式并修复 query 参数生效问题。
- 是否需要在后续 verify 阶段接入真实 NebulaGraph 测试环境？本 change 不强制要求。
