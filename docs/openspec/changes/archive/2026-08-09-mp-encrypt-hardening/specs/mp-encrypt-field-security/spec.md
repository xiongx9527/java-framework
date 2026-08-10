## ADDED Requirements

### Requirement: INSERT/UPDATE 时加密 String 字段

带有 `@FieldEncrypt` 且值为非空 `String` 的实体字段，在 MyBatis `INSERT`/`UPDATE` 执行前 MUST 被加密为密文。

#### Scenario: 写入前字段值变为密文

- **GIVEN** 实体字段带 `@FieldEncrypt` 且当前值为明文
- **WHEN** 执行 `INSERT` 或 `UPDATE`
- **THEN** 实际提交的参数中该字段值 MUST 是密文（与明文不同，且可被对应算法解密回明文）

### Requirement: 查询结果集解密

查询返回的实体中，带 `@FieldEncrypt` 的字段在结果集处理阶段 MUST 被解密为明文。

#### Scenario: 查询后字段恢复明文

- **GIVEN** 数据库中存储的是密文
- **WHEN** 查询返回实体
- **THEN** 实体字段值 MUST 是明文

### Requirement: 仅支持 String 类型字段

`@FieldEncrypt` 只允许标注 `String`（含其子类）字段；`FieldProcessor` 解析到非 `String` 字段时 MUST 抛出框架运行时异常。

#### Scenario: 非 String 字段被拒绝

- **GIVEN** 实体字段类型为 `Integer` 且带 `@FieldEncrypt`
- **WHEN** `FieldProcessor` 解析该字段
- **THEN** 抛出 `BaseRuntimeException` 且提示仅支持 String 类型

### Requirement: TableField 列名映射

`@FieldEncrypt` 字段同时标注 `@TableField(value)` 时，字段定位名 MUST 使用 `@TableField` 指定的列名。

#### Scenario: 使用列名定位字段

- **GIVEN** 字段带 `@FieldEncrypt` 且 `@TableField(value = "enc_name")`
- **WHEN** `FieldProcessor` 生成字段属性
- **THEN** 生成的 `FieldSetProperty.fieldName` MUST 为 `enc_name` 而非 Java 属性名

### Requirement: 加密器与算法选择

字段可通过注解指定自定义 `IEncryptor` 与算法；未指定时 MUST 使用注入的默认加密器。同一自定义加密器类 MUST 复用缓存实例。

#### Scenario: 默认加密器

- **GIVEN** 注解未指定 `encryptor`
- **WHEN** 执行加解密
- **THEN** 使用注入的默认加密器处理

#### Scenario: 自定义加密器复用

- **GIVEN** 注解指定了自定义 `IEncryptor` 类
- **WHEN** 多次执行加解密
- **THEN** 使用该类实例且同一类只创建一次（缓存复用）
