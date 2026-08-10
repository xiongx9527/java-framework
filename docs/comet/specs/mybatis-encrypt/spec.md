# mybatis-encrypt 能力规格

## Requirement: 通用核心框架无关

`fw-plus-mybatis-encrypt` 必须提供不依赖 MyBatis-Plus、MyBatis-Flex 和 Spring 的字段加解密核心，仅依赖原生 MyBatis 与 `fw-plus-common`/`fw-plus-annotation`。

#### Scenario: 模块依赖检查

- **GIVEN** 检查 `fw-plus-mybatis-encrypt` 的 pom
- **THEN** 依赖仅包含 `org.mybatis:mybatis`、`fw-plus-common`、`fw-plus-annotation` 及测试依赖
- **AND** 不包含 `com.baomidou`、`com.ln`、`spring-*` 编译依赖

## Requirement: 写入加密

带 `@FieldEncrypt` 且值为非空 `String` 的实体字段，在 MyBatis `INSERT`/`UPDATE` 执行前必须被加密为密文。

#### Scenario: MyBatis-Plus 写入加密

- **GIVEN** MP 实体字段带 `@FieldEncrypt`
- **WHEN** 通过 MP Mapper 执行 INSERT 或 UPDATE
- **THEN** 落库参数为密文（与明文不同且可解密回明文）

#### Scenario: MyBatis-Flex 写入加密

- **GIVEN** Flex 实体字段带 `@FieldEncrypt`
- **WHEN** 通过 Flex Mapper 执行 INSERT 或 UPDATE
- **THEN** 落库参数为密文（与明文不同且可解密回明文）

## Requirement: 查询解密

查询返回的实体中，带 `@FieldEncrypt` 的字段在结果集处理阶段必须被解密为明文。

#### Scenario: MyBatis-Plus 查询解密

- **GIVEN** 数据库中存储的是密文
- **WHEN** 通过 MP Mapper 查询返回实体
- **THEN** 实体字段值必须是明文

#### Scenario: MyBatis-Flex 查询解密

- **GIVEN** 数据库中存储的是密文
- **WHEN** 通过 Flex Mapper 查询返回实体
- **THEN** 实体字段值必须是明文

## Requirement: 类型约束

`@FieldEncrypt` 只允许标注 `String`（含子类）字段；字段解析发现非 `String` 字段必须抛出框架运行时异常。

#### Scenario: 非 String 字段被拒绝

- **GIVEN** 实体字段类型为 `Integer` 且带 `@FieldEncrypt`
- **WHEN** 解析该字段元数据
- **THEN** 抛出 `BaseRuntimeException` 且提示仅支持 String 类型

## Requirement: ORM 适配器 SPI 与属性名定位

通用核心通过 `FieldNameResolver` SPI 由 ORM 适配器提供属性到列名映射（MP 使用 `@TableField`，Flex 使用 Flex 实体元数据）；字段的 MetaObject 定位必须使用属性名，保证实体参数加解密在属性名与列名不同时仍正确生效。

#### Scenario: MyBatis-Plus 列名映射

- **GIVEN** 字段带 `@FieldEncrypt` 且 `@TableField(value = "enc_name")`
- **WHEN** MP 适配器解析字段
- **THEN** 列名解析结果为 `enc_name`
- **AND** 字段定位名保持属性名 `name`（MetaObject 读写正确，INSERT 落库为密文、SELECT 返回明文）

#### Scenario: MyBatis-Flex 列名映射

- **GIVEN** Flex 实体字段带 `@FieldEncrypt` 且存在 `@Column` 列名映射
- **WHEN** Flex 适配器解析字段
- **THEN** 列名解析结果为映射列名
- **AND** 字段定位名保持属性名（INSERT 落库为密文、SELECT 返回明文）

## Requirement: 配置与注解契约

`@FieldEncrypt` 注解、`IEncryptor` 接口、`fw.plus.mp-encrypt` 配置前缀与现有 Boot 自动配置注册行为保持不变。

#### Scenario: 现有配置与自动配置兼容

- **GIVEN** 引入 `fw-plus-mp-spring-boot-starter` 的应用
- **WHEN** 使用原有 `fw.plus.mp-encrypt` 配置与 `@FieldEncrypt`
- **THEN** 加密/解密拦截器仍由 `MpAutoConfiguration` 自动注册
- **AND** `MpAutoConfigurationTest` 继续通过
