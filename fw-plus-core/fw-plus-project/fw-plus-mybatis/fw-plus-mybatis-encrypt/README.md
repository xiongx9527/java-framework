# fw-plus-mybatis-encrypt

基于 MyBatis 拦截器与 `@FieldEncrypt` 注解的字段加解密通用核心（框架无关，不依赖 MyBatis-Plus/MyBatis-Flex/Spring）。在 `INSERT`/`UPDATE` 写入数据库前自动加密标注字段，在查询结果集返回时自动解密，业务代码无需感知明文/密文转换。

## 模块结构

```text
fw-plus-mybatis/
├── fw-plus-mybatis-encrypt   # 本模块：通用核心（拦截器、字段解析、加解密流程、配置）
├── fw-plus-mp/
│   └── fw-plus-mp-encrypt    # MyBatis-Plus 适配层（@TableField 列名映射）
└── fw-plus-mf/
    └── fw-plus-mf-encrypt    # MyBatis-Flex 适配层（Flex 列名映射）
```

依赖方向固定为：

```text
fw-plus-mp-encrypt ─┐
                    ├──▶ fw-plus-mybatis-encrypt
fw-plus-mf-encrypt ─┘
```

## 使用方式

1. 按 ORM 引入适配模块：`fw-plus-mp-encrypt`（MyBatis-Plus）或 `fw-plus-mf-encrypt`（MyBatis-Flex），二者均依赖本核心。
2. 在实体需要加密的 `String` 字段上标注 `@FieldEncrypt`：

```java
@TableName("user")
public class User {

    private Long id;

    @FieldEncrypt
    private String phone;

    private String name;
}
```

3. 启用自动配置后（`MpEncryptAutoConfiguration` / `MfEncryptAutoConfiguration`），加密/解密拦截器自动注册，无需手动配置。

## 配置

配置前缀为 `fw.plus.mp-encrypt`（MP 与 MF 适配共用该前缀）：

```yaml
fw:
  plus:
    mp-encrypt:
      enabled: true          # 是否启用，默认 true
      private-key: your-key  # 加密密钥（对称/非对称算法使用）
      iv: your-iv            # 初始化向量（对称算法需要）
      salt: your-salt        # 盐值
      algorithm: AES         # 全局默认算法
```

- MP 加密开关：`fw.plus.mp-encrypt.enabled`；MF 加密开关：`fw.plus.mf-encrypt.enabled`。
- 字段级配置（注解）优先级高于全局配置，详见下文 `@FieldEncrypt`。

## @FieldEncrypt 注解

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `salt` | 盐值，注解非空时优先于全局配置 | 空 |
| `algorithm` | 加密算法 | `AES` |
| `executeEncrypt` | 是否执行加密 | `true` |
| `executeDecrypt` | 是否执行解密 | `true` |
| `encryptor` | 自定义加密器实现类 | 默认加密器 `DefaultEncryptor` |

约束与映射：

- `@FieldEncrypt` 仅支持 `String`（含子类）字段；标注在非 `String` 字段上会在字段解析时抛出运行时异常。
- 字段的 MetaObject 读写使用**属性名**；属性到列名映射由 ORM 适配器通过 `FieldNameResolver` SPI 提供（MP 使用 `@TableField(value)`，Flex 使用 Flex 元数据）。

## 支持的算法

`com.xiongx9527.fw.plus.common.mate.annotation.Algorithm` 提供：

- 对称加密：`AES`、`DES`、`SM4`
- 非对称加密：`RSA`、`SM2`
- 摘要：`MD5_32`、`MD5_16`、`SM3`
- 编码：`BASE64`
- PBE：`PBEWithMD5AndDES`、`PBEWithSHA1AndDESede`、`PBEWithSHA1AndRC2_40`

## 自定义加密器

实现 `FieldEncrypt.IEncryptor` 接口并注册为 Spring Bean（或使用无参构造的普通类），在注解中指定：

```java
@Component
public class MyEncryptor implements FieldEncrypt.IEncryptor {

    @Override
    public String encrypt(Algorithm algorithm, String key, String iv, String salt, Object data) throws Exception {
        // 自定义加密逻辑
        return ...;
    }

    @Override
    public String decrypt(Algorithm algorithm, String key, String iv, String salt, Object encryptedData) throws Exception {
        // 自定义解密逻辑
        return ...;
    }
}
```

```java
@FieldEncrypt(encryptor = MyEncryptor.class)
private String name;
```

同一自定义加密器类只实例化一次，后续调用复用缓存实例。

## 行为说明

- 加密：MyBatis `Executor.update` 拦截器，仅对 `INSERT`/`UPDATE` 的参数对象加密；`SELECT`/`DELETE` 不处理参数。
- 解密：`ResultSetHandler.handleResultSets` 拦截器，查询结果返回后解密标注字段。
- 字段值为 `null` 或空字符串时不加密，保持原值。
