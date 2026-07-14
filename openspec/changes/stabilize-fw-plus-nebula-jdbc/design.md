## Context

`fw-plus-nebula` 当前只包含 `fw-plus-nebula-jdbc` 子模块，提供 `com.vesoft.nebula.jdbc` 包下的 JDBC Driver、Connection、Statement、PreparedStatement、ResultSet、MetaData 与 URL 解析工具。该模块面向框架内部和业务侧 JDBC 风格调用，运行环境仍需兼容 Java 1.8。

现状中，连接构造、URL 解析、连接池初始化、Nebula 会话获取、语句执行、ResultSet 读取和不支持方法的异常语义分散在多个类中，缺少明确的行为边界与测试保护。优化目标是先稳定已有 JDBC 表面，而不是扩展 Nebula 查询能力。

## Goals / Non-Goals

**Goals:**

- 明确 JDBC URL 与连接属性的解析结果来源，避免解析后属性与原始入参混用。
- 让连接、连接池、会话、Statement、PreparedStatement、ResultSet 的关闭与失败路径可预测。
- 对支持的方法补齐关闭状态、参数、列索引等基础校验。
- 对暂不支持的方法保持 `SQLFeatureNotSupportedException`，避免静默成功或返回误导性默认值。
- 增加无需真实 Nebula 服务即可运行的单元测试，覆盖解析、生命周期和异常边界。

**Non-Goals:**

- 不新增 Nebula Graph 查询语法能力。
- 不改变 `com.vesoft.nebula.jdbc` 包名、Maven 坐标或公共 JDBC 类型。
- 不升级 `com.vesoft:client` 依赖版本。
- 不引入 Java 9+ API 或新的重量级测试基础设施。
- 不要求本 change 完成真实 Nebula 服务的端到端集成测试。

## Decisions

### Decision 1: 以 JDBC 契约稳定性为主线，而不是重写驱动架构

保留现有类结构和公开入口，逐步收紧行为边界。这样可以降低对 Boot、Magic-API 或业务项目的连带影响，也符合该模块当前体量。

备选方案是重构为新的连接/执行抽象层，但会扩大修改面，并且在缺少现有测试的情况下增加回归风险。本次不采用。

### Decision 2: URL 解析结果作为连接配置的单一事实来源

`NebulaJdbcUrlParser.parse(url, properties)` 返回的 `Properties` 应成为后续 graph space、用户、密码、连接池参数读取的来源。原始 `properties` 只作为输入，不在解析完成后继续参与连接初始化。

备选方案是在每个调用点临时合并属性，但这会重复解析规则并让错误路径不一致。本次统一在解析层或连接构造入口处理。

### Decision 3: 资源生命周期按“成功创建则负责关闭”处理

连接初始化过程中，如果连接池创建成功但会话获取或 `use <space>` 失败，必须释放已创建的池/会话资源。`close()` 应支持幂等；关闭后再调用执行、创建语句、读取结果等方法必须返回明确 `SQLException`。

备选方案是依赖 Nebula client 或 JVM 回收资源，但这会在初始化失败和异常路径下留下连接泄漏风险。本次显式处理。

### Decision 4: 测试优先覆盖无外部服务路径

优先为 URL 解析、属性类型转换、关闭状态、列索引校验、Unsupported 方法异常等无需真实 Nebula 服务的路径补单元测试。对涉及 `NebulaPool` 或 `Session` 的逻辑，优先通过小范围可替换构造、受保护方法或轻量测试替身降低外部依赖。

备选方案是直接引入真实 Nebula 集成测试，但会增加环境复杂度，不适合作为稳定性 change 的第一步。

## Risks / Trade-offs

- 现有调用方可能依赖部分宽松或静默行为 → 通过保持公共 API 不变、仅收紧异常路径，并在任务中要求回归编译与单元测试缓解。
- Nebula client 类型可能不易 mock → 优先测试解析和 JDBC 契约逻辑，必要时用受保护工厂方法隔离外部连接创建。
- 部分 JDBC 方法当前返回默认值而不是不支持异常 → 逐项判断是否属于已支持行为，避免一次性改变过多调用语义。
- 没有真实 Nebula 服务时无法验证完整网络链路 → 本 change 明确把端到端验证列为非目标，保留后续集成测试空间。

## Migration Plan

1. 先补充解析、异常和生命周期相关单元测试，固定期望行为。
2. 再调整实现，使测试覆盖的失败路径和关闭路径稳定。
3. 运行 `fw-plus-nebula-jdbc` 模块测试与必要的 Maven 编译。
4. 如发现公共行为变更，优先通过兼容处理保留旧调用路径；确需变更时在任务和验证记录中说明。

## Open Questions

- 是否存在团队内固定的 Nebula 测试环境，可在后续 verify 或独立 change 中补充集成测试。
- 是否有外部业务依赖当前某些 JDBC 方法的默认返回值，需要在实现前通过搜索调用方进一步确认。
