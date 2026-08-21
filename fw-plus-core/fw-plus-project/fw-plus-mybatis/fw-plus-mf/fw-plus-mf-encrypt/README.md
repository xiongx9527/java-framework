# fw-plus-mf-encrypt

MyBatis-Flex 字段加解密适配层，依赖通用核心 `fw-plus-mybatis-encrypt`。

本模块提供 `MybatisFlexFieldNameResolver`，将 Flex 实体元数据（`@Column` 等）列名映射接入核心的 `FieldNameResolver` SPI；加密/解密拦截器由核心提供，经 `MfEncryptAutoConfiguration` 自动注册（开关 `fw.plus.mf-encrypt.enabled`，配置复用 `fw.plus.mp-encrypt` 前缀）。

使用方式、`@FieldEncrypt` 注解、配置、算法与自定义加密器请参阅 [`fw-plus-mybatis-encrypt`](../fw-plus-mybatis-encrypt/README.md)。
