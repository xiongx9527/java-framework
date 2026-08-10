## Design

`ChangeDiffEngine` 保留快照 Map 的原始列标签，以兼容现有数据库和字段审计输出；读取监控字段时先进行精确匹配，再回退到 `equalsIgnoreCase` 匹配。这样只改变字段查找，不改变事件中的原始列名和值类型。

集成契约对金额字段统一通过 `Number.intValue()` 比较，避免 Oracle `NUMBER`、MySQL 数值类型和 Java 参数类型之间的包装类差异。

验证使用现有五个数据库 IT：MySQL、MariaDB、PostgreSQL、SQL Server、Oracle。
