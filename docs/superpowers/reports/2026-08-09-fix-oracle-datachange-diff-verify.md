## 验证报告：fix-oracle-datachange-diff

### 总结

| 维度 | 结果 |
|---|---|
| 完整性 | 5/5 个任务完成 |
| 正确性 | 2/2 个需求覆盖 |
| 一致性 | 符合设计决策 |

### 验证项

1. OpenSpec change 校验通过。
2. Oracle 真实数据库集成测试通过：
   - `OracleDataChangeDialectIT`
   - `Tests run: 1, Failures: 0, Errors: 0`
3. MySQL、MariaDB、PostgreSQL、SQL Server 的既有真实数据库报告均为零失败、零错误。
4. 模块完整测试通过，退出码为 0。
5. Maven package 构建通过，退出码为 0。
6. `git diff --check` 通过。

### 需求覆盖

- 快照字段读取不区分大小写：
  已在 `ChangeDiffEngine.value` 中实现，先精确匹配，再使用 `equalsIgnoreCase` 回退；Oracle 大写 JDBC 列标签由真实集成测试覆盖。
- 跨数据库数值断言：
  集成契约通过 `Number.intValue()` 比较数值，兼容 Oracle `BigDecimal`，不改变事件中的值类型。

### 问题

未发现严重或重要问题。仓库级 Comet 初始化生成了多个平台指令目录，这些生成文件不属于本 change 的功能实现范围。
