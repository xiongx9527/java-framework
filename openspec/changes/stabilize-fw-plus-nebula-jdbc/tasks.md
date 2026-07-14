## 1. 基线梳理与测试基础

- [ ] 1.1 梳理 `fw-plus-nebula-jdbc` 的公开 JDBC 类，区分已支持方法与明确不支持方法。
- [ ] 1.2 仅在现有父级依赖管理未提供所需 JUnit 能力时，补充或调整该模块的测试依赖。
- [ ] 1.3 为 `NebulaJdbcUrlParser` 创建聚焦单元测试，覆盖合法 URL、畸形 URL、缺失 graph space、主机解析、凭据和数字型连接池选项。

## 2. URL 解析与连接配置

- [ ] 2.1 在 `NebulaConnection` 中将解析后的有效 `Properties` 作为 graph space、凭据和连接池配置的单一来源。
- [ ] 2.2 在 Driver 或 Connection 创建边界，将非法 URL、缺失必需属性和非法数字选项统一为清晰的 SQL 层异常。
- [ ] 2.3 增加回归测试，证明非法输入不会创建或初始化 Nebula 连接池。

## 3. 资源生命周期

- [ ] 3.1 确保 `NebulaConnection.close()` 具备幂等性，并释放已成功创建的 session 与 pool 资源。
- [ ] 3.2 确保连接初始化在部分资源创建后失败时，会先清理已创建资源再抛出异常。
- [ ] 3.3 为关闭后的连接行为增加测试，覆盖 `createStatement()`、`execute(String)` 等受支持操作。

## 4. Statement 与 ResultSet 契约

- [ ] 4.1 收紧 `NebulaStatementImpl` 与 `NebulaPreparedStatementImpl` 的关闭状态、SQL 文本、查询/更新分类和当前结果处理。
- [ ] 4.2 收紧 `NebulaResultSet` 与 `NebulaResultSetMetaData` 在受支持方法中的关闭状态、游标状态、空结果和一基列索引校验。
- [ ] 4.3 增加测试，覆盖查询/更新方法不匹配、关闭后执行 Statement、非法元数据索引、空结果元数据和关闭后读取 ResultSet。

## 5. 不支持方法语义与验证

- [ ] 5.1 审计当前返回占位值或静默成功的 JDBC 方法，将真正不支持的行为改为 `SQLFeatureNotSupportedException`。
- [ ] 5.2 保留有意支持的简单状态方法，并确保其返回值稳定且有明确语义。
- [ ] 5.3 运行 `cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc`，并修复失败。
- [ ] 5.4 运行 `cd fw-plus-core && ./mvnw install -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -DskipTests`，验证模块编译与打包。
