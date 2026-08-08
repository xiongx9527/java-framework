# fw-plus-mybatis-datachange

基于原生 MyBatis `StatementHandler` 的数据变更追踪模块。它负责解析最终执行的 SQL、读取变更前后快照、生成统一的 `DataChangeEvent`，并交给一个或多个 `DataChangeHandler` 处理。

## 配置

Spring Boot 配置前缀为 `fw.plus.mybatis-datachange`：

```yaml
fw:
  plus:
    mybatis-datachange:
      enabled: true
      max-snapshot-rows: 1000
      tables:
        - name: test_order
          key-columns: [id]
          include-columns: [status, amount]
```

事务处理是可选的；未配置事务时仍可生成最佳努力（best effort）事件。`CALL procedure_name(...)` 不在支持范围内，会被跳过。

## Handler

实现公共接口 `com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler` 并注册为 Spring Bean：

```java
@Component
public class AuditHandler implements DataChangeHandler {
    public boolean supports(DataChangeEvent event) { return true; }
    public void handle(DataChangeEvent event) { /* 发布或记录事件 */ }
    public int getOrder() { return 0; }
}
```

## ORM 适配

公共模块只依赖 MyBatis。引入 MyBatis-Plus 或 MyBatis-Flex 数据变更扩展后，Boot 自动配置会分别注册对应元数据适配器；两者可以同时存在，互不覆盖。MyBatis-Plus/Flex 特有的填充字段、逻辑删除和实体元数据由各自适配器提供。

## 数据库支持

内置方言覆盖 MySQL、MariaDB、PostgreSQL、Oracle 和 SQL Server。当前已使用真实数据库验证 MySQL 5.7、MariaDB 11、PostgreSQL 16；Oracle 和 SQL Server 尚未执行真实库测试。
