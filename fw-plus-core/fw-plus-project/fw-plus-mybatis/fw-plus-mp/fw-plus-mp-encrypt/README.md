# fw-plus-mp-encrypt

MyBatis-Plus 字段加解密适配层，依赖通用核心 `fw-plus-mybatis-encrypt`。

本模块提供 `MybatisPlusFieldNameResolver`，将 `@TableField(value)` 列名映射接入核心的 `FieldNameResolver` SPI；加密/解密拦截器由核心提供，经 `MpEncryptAutoConfiguration` 自动注册。

使用方式、`@FieldEncrypt` 注解、配置（`fw.plus.mp-encrypt`）、算法与自定义加密器请参阅 [`fw-plus-mybatis-encrypt`](../fw-plus-mybatis-encrypt/README.md)。
