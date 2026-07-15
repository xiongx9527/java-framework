## 1. 测试基线与覆盖补强

- [x] 1.1 确认 `fw-plus-nebula-jdbc` 当前 Maven 测试配置，补充必要的单元测试依赖但不引入真实 NebulaGraph 集成环境要求
- [x] 1.2 为 JDBC URL 解析和连接配置合并行为补充单元测试
- [x] 1.3 为连接池数字参数解析的字符串值、默认值和非法值场景补充单元测试
- [x] 1.4 为 Statement 查询/更新语句分类的大小写、换行、多空白和非法类型场景补充单元测试
- [x] 1.5 为资源重复关闭和 ResultSet 边界行为补充单元测试或可隔离验证

## 2. 连接配置与连接池参数稳定化

- [x] 2.1 调整 `NebulaConnection` 初始化逻辑，统一使用 `NebulaJdbcUrlParser.parse` 后的合并配置
- [x] 2.2 实现连接池整数参数的集中解析逻辑，支持字符串和数字类型并提供明确异常
- [x] 2.3 确认 `graphspace`、用户名、密码和连接池参数均来自同一合并配置来源

## 3. JDBC 基础行为稳定化

- [x] 3.1 调整 `Connection.close()`、`Statement.close()`、`ResultSet.close()` 的重复关闭行为，避免非必要异常
- [x] 3.2 改善 `NebulaStatementImpl` 的 nGQL 类型分类逻辑，支持常见空白和大小写格式
- [x] 3.3 改善 `NebulaResultSet` 游标边界和 `wasNull()` 的异常语义，避免非预期 NPE

## 4. 验证与收尾

- [ ] 4.1 运行 `fw-plus-nebula-jdbc` 模块测试并修复失败
- [ ] 4.2 检查变更不引入 SQL 转 nGQL、MySQL wire protocol 或跨层改动
- [ ] 4.3 更新任务勾选并准备进入 Comet design 阶段
