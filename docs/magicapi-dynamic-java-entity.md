# Magic-API 动态 Java 实体

`fw-plus-magicapi-plugin-java-entity` 允许在 Magic-API Resource 中维护完整 Java 8 源码。保存后，插件会重新编译全部动态实体，并在候选代际完整成功后原子切换。应用不需要重启。

## 引入插件

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-magicapi-plugin-java-entity</artifactId>
    <version>${fw-plus.version}</version>
</dependency>
```

运行环境必须是 JDK，并提供 `javax.tools.JavaCompiler`。只有 JRE 时应用仍可启动，但动态实体入口会返回 compiler 不可用诊断。

插件默认启用，可按需调整限制：

```yaml
magic-api:
  plugin:
    java-entity:
      enabled: true
      max-source-bytes: 262144
      max-total-source-bytes: 2097152
      compile-timeout: 30s
      max-diagnostics: 100
      max-diagnostic-message-length: 500
```

设置 `magic-api.plugin.java-entity.enabled=false` 可完全禁用自动配置。未引入独立插件依赖时，动态实体自动配置也不会激活。

## 创建 Resource

Resource 类型选择 `java-entity`。持久化文件后缀为 `.java`，内容仍遵循 Magic-API Resource 格式：JSON 头、分隔线、完整 Java 源码。

```text
{
  "id": "user-entity",
  "name": "User",
  "path": ""
}
================================
package demo;

public class User {
    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
```

每个 Resource 只能包含一个顶级 `public` 类，Resource 名称必须等于类的简单名。全限定类名是稳定标识；简单名只在当前代际唯一时可用。

## 常用注解

实体可以完全不使用注解，也可以同时声明 Lombok、MyBatis-Plus、MyBatis-Flex、Fastjson 1.x、Fastjson2 和 Jackson 注解。

```java
package demo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
@Table(value = "sys_user")
public class User {

    @TableId("id")
    @com.mybatisflex.annotation.Id
    private Long id;

    @TableField("user_name")
    @Column("user_name")
    @com.alibaba.fastjson.annotation.JSONField(name = "user_name")
    @com.alibaba.fastjson2.annotation.JSONField(name = "user_name")
    @JsonProperty("user_name")
    private String userName;

    @TableField("password")
    @Column("password")
    @com.alibaba.fastjson.annotation.JSONField(serialize = false, deserialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(serialize = false, deserialize = false)
    @JsonIgnore
    private String password;
}
```

Lombok 会在每次代际编译时重新执行。`@Data` 生成的访问器和 `@Builder` 生成的内部类型都是真实字节码。`create()` 与 `create(map)` 仍要求实体有可访问的无参构造器，因此使用 `@Builder` 时通常同时声明 `@NoArgsConstructor`。

## Magic-Script 使用

```javascript
import entity

var User = entity.User
var user = User.create({
    userName: '张三',
    password: 'secret'
})

return {
    name: User.name(),
    version: User.version(),
    type: User.javaClass(),
    data: user
}
```

描述器提供以下方法：

- `name()`：全限定类名。
- `version()`：当前描述器绑定的代际版本。
- `javaClass()`：真实的 `Class<?>`。
- `create()`：调用无参构造器创建实例。
- `create(map)`：优先通过 setter 写入属性，没有 setter 时回退到非 `static`、非 `final` 实例字段。
- `resourceId()`、`sourceDigest()`：当前类型对应的 Resource 标识和 SHA-256 源码摘要。

两个包中存在同名类时，不注册冲突的简单名别名，应使用全限定类名：

```javascript
var User = entity['demo.User']
```

## 热更新

新增、修改或删除任一 Resource 都会触发全部动态实体共同编译。成功后，新访问的 `entity.User` 返回新描述器和新 `Class<?>`；已经取得的旧描述器仍可完成当前调用。

语法错误、依赖缺失、Lombok 处理失败、超时或类加载失败不会替换当前成功代际。冷启动首次编译失败时，应用和现有 Magic-API 入口继续启动，动态实体入口返回受限诊断。

## ORM 边界

MyBatis-Plus 和 MyBatis-Flex 本身不需要改造。插件只保证动态类上的两套 ORM 注解真实存在，并可通过 `javaClass()` 将类型交给接收 `Class<?>` 的 Java API。

首期不生成 Mapper，不注册实体专属 Mapper，不预热 MP/Flex 元数据，也不提供 CRUD、分页、Join 或 MP-Join。现有静态 Mapper 和 `MybatisFlexModule` 行为不变。

## 安全

Java 源码拥有宿主 JVM 的代码执行权限。只允许受信任的 Magic-API 管理员编辑和激活 `java-entity` Resource；插件不提供同 JVM 内的不可信代码沙箱。
