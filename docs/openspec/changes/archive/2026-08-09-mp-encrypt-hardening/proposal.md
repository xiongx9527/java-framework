## Why

`fw-plus-mp-encrypt` 是框架内基于 `@FieldEncrypt` 注解的字段加解密能力，但目前没有任何单元测试与模块文档，行为仅靠人工维护，缺少回归保障。参照 `fw-plus-mp-datachange` 的处理方式，为 encrypt 补齐测试、文档与 spec 驱动的验收场景，固化字段加解密契约。

## What Changes

- 为 `fw-plus-mp-encrypt` 补充单元测试：字段元数据解析（`FieldProcessor`）、加解密处理（`FieldEncryptorHandler`）、两个 MyBatis 拦截器、配置优先级（`EncryptorProperties`）。
- 新增 H2 端到端测试，用真实 SqlSession 验证 INSERT/UPDATE 加密与查询解密的完整链路。
- 新增 `fw-plus-mp-encrypt/README.md`：配置前缀、`@FieldEncrypt` 用法、支持的算法、自定义加密器扩展。
- 保持现有模块结构、公开 API 与生产代码行为不变（测试暴露的缺陷除外，若有则以最小改动修复）。

## Capabilities

### New Capabilities

- `mp-encrypt-field-security`: 覆盖 `@FieldEncrypt` 字段加解密的契约与验证（类型约束、列名映射、拦截行为、配置优先级）。

### Modified Capabilities

## Impact

- 仅影响 `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-encrypt`：`pom.xml` 增加 test 依赖（`junit-jupiter`、`assertj-core`、`h2`），新增 `src/test/java/**` 与 `README.md`。
- 不改变公开 API、配置前缀、`MpEncryptAutoConfiguration` 注册逻辑与依赖方向。
