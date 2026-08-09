## 1. Implementation

- [x] 1.1 在 `fw-plus-mp-encrypt/pom.xml` 增加 test 依赖（`junit-jupiter`、`assertj-core`、`h2`）
- [x] 1.2 `FieldProcessorTest`：字段解析、类型约束、`@TableField` 列名映射、继承与缓存
- [x] 1.3 `FieldEncryptorHandlerTest`：INSERT/UPDATE 加密、非支持命令跳过、Map 参数、加密器缓存复用
- [x] 1.4 `FieldEncryptInterceptorTest` / `FieldDecryptInterceptorTest`：插件签名与包装行为
- [x] 1.5 `EncryptorPropertiesTest`：salt/algorithm 配置优先级
- [x] 1.6 H2 端到端测试：真实 SqlSession 中 INSERT 加密与 SELECT 解密
- [x] 1.7 新增 `fw-plus-mp-encrypt/README.md` 使用文档

## 2. Verification

- [x] 2.1 运行 `fw-plus-mp-encrypt` 模块测试全部通过
- [x] 2.2 执行 `git diff --check` 清理空白问题
