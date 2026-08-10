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
      max-concurrent-compilations: 1
      max-diagnostics: 100
      max-diagnostic-message-length: 500
```

编译超时后会中断候选任务；不响应中断的任务仍占用并发名额，退出前新的编译会返回 `compiler.busy`。

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

## 控制台使用

引入 `fw-plus-magicapi-plugin-java-entity` 后，Magic-API 控制台会在左侧资源树新增“Java实体”。前端代码位于插件模块自己的 `src/console` 目录，打包后注册为 `MagicJavaEntity`，入口文件为 `magic-java-entity.1.0.0.iife.js`。

在资源树中右键分组选择“新建 Java实体”，会打开 Monaco Java 源码编辑器，并填充一个包含 Lombok、MyBatis-Plus、MyBatis-Flex、Fastjson 1.x、Fastjson2 和 Jackson 注解的示例实体。保存时仍走 Magic-API 原生资源保存流程，后端 `java-entity` Resource 会校验源码并触发动态编译。

新建后通常只需要改三处：

- Resource 名称改为实体类简单名，例如 `User`。
- Java 源码中的 `package`、`public class` 和表/字段注解按实际库表调整。
- 确保每个 Resource 只有一个顶级 `public` 类，并且类名与 Resource 名称一致。

该控制台插件只提供源码编辑入口，不提供动态 Mapper、CRUD 表单、Join 设计器或字段生成器。

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
var emptyUser = User.create()
var user = User.create({
    userName: '张三',
    password: 'secret'
})

return {
    name: User.name(),
    version: User.version(),
    type: User.javaClass(),
    empty: emptyUser,
    data: user
}
```

描述器提供以下方法：

- `name()`：全限定类名。
- `version()`：当前描述器绑定的代际版本。
- `javaClass()`：真实的 `Class<?>`。
- `create()`：不传参数，调用无参构造器创建空实例。
- `create(map)`：优先通过 setter 写入属性，没有 setter 时回退到非 `static`、非 `final` 实例字段。
- `create(null)`：按空 Map 处理，效果等同于 `create()`；文档和脚本中推荐直接写 `create()`。
- `resourceId()`、`sourceDigest()`：当前类型对应的 Resource 标识和 SHA-256 源码摘要。

两个包中存在同名类时，不注册冲突的简单名别名，应使用全限定类名：

```javascript
var User = entity['demo.User']
```

## 热更新

新增、修改或删除任一 Resource 都会触发全部动态实体共同编译。成功后，新访问的 `entity.User` 返回新描述器和新 `Class<?>`；已经取得的旧描述器仍可完成当前调用。

语法错误、依赖缺失、Lombok 处理失败、超时或类加载失败不会替换当前成功代际。冷启动首次编译失败时，应用和现有 Magic-API 入口继续启动，动态实体入口返回受限诊断。

## MyBatis-Flex 使用

`@MagicModule("mybatisflex")` 对应 Magic-Script 模块名是 `mybatisflex`。动态实体既可以转换为 Flex `Row`，也可以通过 Class 化 Query Wrapper 做单表查询、Join、动态 where、分组和排序。

### Row / Map 转换

```javascript
import entity
import mybatisflex

var User = entity['demo.User']

var user = User.create({
    id: 1,
    userName: '张三'
})

var row = mybatisflex.toRow(user)
var map = mybatisflex.toMap(user)

mybatisflex.insert('sys_user', row)
```

批量转换：

```javascript
var rows = mybatisflex.toRows([user1, user2])
var maps = mybatisflex.toMaps([user1, user2])
```

`toRow/toMap` 只识别 MyBatis-Flex 注解：

- `@Column("user_name")`：映射到指定列名。
- `@Column(ignore = true)`：跳过该字段。
- `@Id`：在 `toRow` 时标记主键，方便 `updateById/deleteById/selectOneById` 使用。

### Class 化动态查询

```javascript
import entity
import mybatisflex

var User = entity['demo.User']
var userName = body.userName
var status = body.status

var query = mybatisflex.query(User.javaClass(), 'u')
    .selectAs(User.javaClass(), 'u', 'id', 'id')
    .selectAs(User.javaClass(), 'u', 'userName', 'userName')
    .eq(status != null, 'u', 'status', status)
    .like(userName != null && userName != '', 'u', 'userName', userName)
    .orderByDesc('u', 'id')

return mybatisflex.selectListByQuery(query)
```

### Class 化 Join

```javascript
import entity
import mybatisflex

var User = entity['demo.User']
var Dept = entity['demo.Dept']
var UserDeptVO = entity['demo.UserDeptVO']

var query = mybatisflex.query(User.javaClass(), 'u')
    .leftJoin(Dept.javaClass(), 'd', 'u', 'deptId', 'id')
    .select(UserDeptVO.javaClass())
    .eq(body.status != null, 'u', 'status', body.status)
    .groupBy('u', 'id')
    .orderByDesc('u', 'id')

var page = mybatisflex.paginate(1, 10, query)
return page.getRecords().stream()
    .map(row => row.toEntity(UserDeptVO.javaClass()))
    .toList()
```

说明：

- `leftJoin(Dept.javaClass(), 'd', 'u', 'deptId', 'id')` 表示 `u.deptId = d.id`，字段参数是 Java 属性名，不是数据库列名。
- `select(UserDeptVO.javaClass())` 会按 VO 属性名匹配已注册别名中的同名实体属性。
- `eq/like/ge/le/gt/lt/in/isNull/isNotNull` 都支持 `condition` 参数，`false` 时不追加 where 条件。
- Flex Join Wrapper 只用于查询、分页和统计，不用于修改数据；修改数据走单表 Row 更新，按关联条件修改走完整动态 SQL。

## MyBatis-Plus 使用

`@MagicModule("mybatisplus")` 对应 Magic-Script 模块名是 `mybatisplus`。模块通过 `mybatisplus-xml` 里的 XML statement 驱动动态实体 CRUD、Wrapper、分页和 Join；用户不需要写动态 Mapper 接口，但需要先创建对应 XML statement。

### CRUD / 分页

```javascript
import entity
import mybatisplus

var User = entity['demo.User']

var user = User.create({
    id: 1,
    userName: '张三'
})

mybatisplus.insert(user)
mybatisplus.updateById(User.create({
    id: 1,
    userName: '李四'
}))

var one = mybatisplus.selectById(User.javaClass(), 1)
mybatisplus.deleteById(User.javaClass(), 1)
```

分页仍走 MyBatis-Plus 分页拦截器，不在插件里硬写 `LIMIT/OFFSET`。Plus/Flex 脚本侧字段写法统一为 Java 属性名：有 ORM 注解列名时使用注解映射，没有注解列名时使用属性名本身。非表字段（Plus 的 `@TableField(exist = false)`、Flex 的 `@Column(ignore = true)`）和未知属性在动态 wrapper 条件里会直接跳过，不抛错：

```javascript
var query = mybatisplus.query(User.javaClass())
query.eq(body.status != null, 'status', body.status)
query.like(body.userName != null && body.userName != '', 'userName', body.userName)

return mybatisplus.selectPage(User.javaClass(), 1, 10, query)
```

### Class 化 MP-Join

```javascript
import entity
import mybatisplus

var User = entity['demo.User']
var Dept = entity['demo.Dept']
var UserDeptVO = entity['demo.UserDeptVO']

mybatisplus.init(User.javaClass(), Dept.javaClass(), UserDeptVO.javaClass())

var wrapper = mybatisplus.join(User.javaClass(), 'u')
    .leftJoin(Dept.javaClass(), 'd', 'u', 'deptId', 'id')
    .select(UserDeptVO.javaClass())
    .eq(body.status != null, 'u', 'status', body.status)
    .like(body.userName != null && body.userName != '', 'u', 'userName', body.userName)
    .groupBy('u', 'id')
    .orderByDesc('u', 'id')

return mybatisplus.selectJoinList(
    User.javaClass(),
    UserDeptVO.javaClass(),
    wrapper
)
```

分页：

```javascript
var page = mybatisplus.selectJoinPage(
    User.javaClass(),
    UserDeptVO.javaClass(),
    1,
    10,
    wrapper
)
```

说明：

- 脚本侧只写实体 `Class`、表别名和 Java 属性名，不写表名、列名或 Join 片段。
- `select(UserDeptVO.javaClass())` 会按 VO 属性名自动匹配已注册实体别名中的同名属性；字段名不一致时使用 `selectAs(Entity.javaClass(), alias, property, voProperty)`。
- `joinWrapper(User.javaClass())` 保留给旧脚本兼容，新脚本推荐使用 `join(User.javaClass(), 'u')`。
- Magic-Script 里不能直接写 Java 源码的 `User::getId`；普通 MP Lambda Wrapper 可用 `mybatisplus.fn(User.javaClass(), 'userName')` 生成运行期字段函数。
- MPJ Join Wrapper 只用于查询、分页和统计，不支持 Join 更新；修改数据走单表 CRUD，按关联条件修改走完整动态 SQL。

## 完整动态 SQL

普通 CRUD / Join / where 使用实体 Class 封装；复杂查询如果需要子查询、`exists`、数据库函数或高度动态 SQL，或者需要按关联表条件更新主表，应使用完整 SQL，不在 Join Wrapper 里拼半截 SQL 片段。

### MyBatis-Flex 完整动态 SQL

`mybatisflex` 模块支持 db 的完整 SQL 能力，同时保留 Flex 自己的 Row SQL 方法。

db 动态 SQL 能力包括：

- `select / selectOne / selectValue / selectInt / count`
- `page`
- `insert / update / batchUpdate`
- `call`
- `table`
- `cache / deleteCache`
- `camel / pascal / lower / upper / normal / columnCase`

这些方法支持 Magic-API db 的动态 SQL 拼接语法，例如 `?{condition, sql}`、`#{param}`，也支持 datasource 动态属性，例如 `mybatisflex['slave'].select(...)`。

Flex Row SQL 方法包括：

- `selectOneBySql(sql, args...)`
- `selectListBySql(sql, args...)`
- `insertBySql(sql, args...)`
- `updateBySql(sql, args...)`
- `deleteBySql(sql, args...)`

使用 db 动态 SQL 能力查询：

```javascript
import entity
import mybatisflex

var User = entity['demo.User']

var rows = mybatisflex.select("""
    select id, user_name as userName
    from sys_user
    where 1 = 1
    ?{body.status != null, and status = #{body.status}}
    ?{body.userName != null && body.userName != '', and user_name like concat('%', #{body.userName}, '%')}
""")

return rows.stream()
    .map(row => User.create(row))
    .toList()
```

完整 SQL 分页：

```javascript
import entity
import mybatisflex

var User = entity['demo.User']

var page = mybatisflex.page("""
    select id, user_name as userName
    from sys_user
    where 1 = 1
    ?{body.status != null, and status = #{body.status}}
    ?{body.userName != null && body.userName != '', and user_name like concat('%', #{body.userName}, '%')}
    order by id desc
""", 10, 0)

return {
    total: page.getTotal(),
    records: page.getList().stream()
        .map(row => User.create(row))
        .toList()
}
```

使用 Flex Row SQL 方法查询时，结果返回 Flex `Row`，可以转成动态 Java Entity：

```javascript
var rows = mybatisflex.selectListBySql("""
    select id, user_name as userName
    from sys_user
    where status = ?
""", body.status)

return rows.stream()
    .map(row => row.toEntity(User.javaClass()))
    .toList()
```

按关联表条件修改主表时，可以使用 db 动态 SQL：

```javascript
return mybatisflex.update("""
    update sys_user u
    set u.status = #{body.status}
    where exists (
        select 1
        from sys_dept d
        where d.id = u.dept_id
          and d.dept_name = #{body.deptName}
    )
""")
```

也可以使用 Flex Row SQL 方法：

```javascript
return mybatisflex.updateBySql("""
    update sys_user u
    set u.status = ?
    where exists (
        select 1
        from sys_dept d
        where d.id = u.dept_id
          and d.dept_name = ?
    )
""", 0, body.deptName)
```

### MyBatis-Plus 完整动态 SQL

`mybatisplus` 模块同样支持 db 的完整 SQL 能力：

- `select / selectOne / selectValue / selectInt / count`
- `page`
- `insert / update / batchUpdate`
- `call`
- `table`
- `cache / deleteCache`
- `camel / pascal / lower / upper / normal / columnCase`

这些方法支持 Magic-API db 的动态 SQL 拼接语法，例如 `?{condition, sql}`、`#{param}`，也支持 datasource 动态属性，例如 `mybatisplus['slave'].select(...)`。

动态 Java Entity 可以作为结果类型使用：

```javascript
import entity
import mybatisplus

var User = entity['demo.User']

var rows = mybatisplus.select("""
    select id, user_name as userName
    from sys_user
    where 1 = 1
    ?{body.status != null, and status = #{body.status}}
    ?{body.userName != null && body.userName != '', and user_name like concat('%', #{body.userName}, '%')}
""")

return rows.stream()
    .map(row => User.create(row))
    .toList()
```

完整 SQL 分页：

```javascript
import entity
import mybatisplus

var User = entity['demo.User']

var page = mybatisplus.page("""
    select id, user_name as userName
    from sys_user
    where 1 = 1
    ?{body.status != null, and status = #{body.status}}
    ?{body.userName != null && body.userName != '', and user_name like concat('%', #{body.userName}, '%')}
    order by id desc
""", 10, 0)

return {
    total: page.getTotal(),
    records: page.getList().stream()
        .map(row => User.create(row))
        .toList()
}
```

按关联表条件修改主表时，同样使用 db 动态 SQL：

```javascript
return mybatisplus.update("""
    update sys_user u
    set u.status = #{body.status}
    where exists (
        select 1
        from sys_dept d
        where d.id = u.dept_id
          and d.dept_name = #{body.deptName}
    )
""")
```

## MyBatis XML Resource

控制台除“Java实体”外，还提供两套独立 XML 资源：

- `mybatisplus-xml`：MyBatis-Plus XML Mapper，包前缀 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus`。
- `mybatisflex-xml`：MyBatis-Flex XML Mapper，包前缀 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex`。

两套 XML 不共用资源类型、元信息模型或前端入口。XML 内容是完整标准 MyBatis Mapper XML：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="demo.User">
    <sql id="Base_Column_List">
        id, user_name AS userName, status
    </sql>

    <select id="selectByCondition" parameterType="map" resultType="demo.User">
        SELECT <include refid="Base_Column_List"/>
        FROM sys_user
        <where>
            <if test="status != null">
                AND status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
                AND user_name LIKE CONCAT('%', #{keyword}, '%')
            </if>
        </where>
    </select>
</mapper>
```

### XML 中使用动态 Java Entity 类型

`mybatisplus-xml` 和 `mybatisflex-xml` 都支持在 MyBatis XML 中直接引用动态 Java Entity。保存、校验和注册 XML 时，插件会把动态实体注册到 MyBatis `TypeAliasRegistry`，所以 `parameterType`、`resultType`、`resultMap type` 等位置可以解析到运行时动态编译出来的 Class。

例如动态实体全限定类名为 `demo.User` 时：

```xml
<insert id="insert" parameterType="demo.User">
    INSERT INTO sys_user
    (
        id,
        user_name,
        dept_id,
        status,
        create_time
    )
    VALUES
    (
        #{id},
        #{userName},
        #{deptId},
        #{status},
        #{createTime}
    )
</insert>

<select id="selectById" resultType="demo.User">
    SELECT
        id,
        user_name AS userName,
        dept_id AS deptId,
        status,
        create_time AS createTime
    FROM sys_user
    WHERE id = #{id}
</select>
```

如果动态实体简单类名没有冲突，也可以写：

```xml
<insert id="insert" parameterType="User">
    INSERT INTO sys_user (id, user_name)
    VALUES (#{id}, #{userName})
</insert>

<select id="selectById" resultType="User">
    SELECT id, user_name AS userName
    FROM sys_user
    WHERE id = #{id}
</select>
```

如果存在多个同名动态实体，例如 `demo.User` 和 `test.User`，简单类名 `User` 不会作为唯一别名使用，请写全限定类名。

MyBatis XML 插件会把解析出的动态 `Class` 保存在 `TypeAliasRegistry`、`MappedStatement` 等第三方缓存中。仅热更新 Java Entity Resource 不会刷新这些已注册类型引用；实体结构变化后需要重启服务以重建 MyBatis 配置。本限制不影响 `entity` 模块和直接从 `DynamicEntityRegistry` 重新解析描述器的热更新语义。

资源元信息：

> **默认数据源 key 说明**：Magic-API 默认数据源没有显式 key 时，XML Resource 里的 `dbKey` 固定按 `default` 理解；前端留空保存时，后端也会归一为 `default`。

- `enabled`：关闭后资源保留但不注册。
- `dbKey`：默认 `default`，创建后不可修改。
- `tableName`：必须是对应 datasource 中真实存在的表或视图。
- `description`：说明用途和主要 statement。

校验规则：

- `dbKey` 为空按 `default` 处理。
- `dbKey` 必须存在。
- `dbKey + tableName` 在各自 XML 类型下唯一。
- 保存时校验表名存在。
- 删除 datasource 时，如果仍被 Plus/Flex XML 引用，则拒绝删除；不区分 XML 是否启用。

脚本调用：

```javascript
import entity
import mybatisplus

var User = entity['demo.User']
var mapper = mybatisplus['default'].mapper(User.javaClass())

return mapper.selectList('selectByCondition', {
    status: body.status,
    keyword: body.keyword
})
```

Flex 对应：

```javascript
import entity
import mybatisflex

var User = entity['demo.User']
var mapper = mybatisflex['default'].mapper(User.javaClass())

return mapper.selectList('selectByCondition', {
    status: body.status,
    keyword: body.keyword
})
```

XML 更新采用逻辑 namespace 到物理 namespace 的映射，避免 MyBatis 原生 `MappedStatement` 同名覆盖问题。脚本始终调用逻辑 statement；资源保存后，新调用会自动指向最新版本。

## ORM 边界

MyBatis-Plus、MyBatis-Flex 和 MP-Join 本身不需要改造。插件只在模块层做动态实体适配：

- 不要求用户编写动态 Mapper 接口；XML Resource 等价于动态 Mapper XML。
- MyBatis-Plus 通过 XML Resource 驱动动态实体 CRUD、Wrapper、分页、Class 化 MP-Join；必须先创建 XML statement，不再自动注入动态 BaseMapper statement。
- MyBatis-Flex 支持 Row/Map 转换、Class 化查询、Class 化 Join、动态 where、group by、order by。
- MyBatis-Plus XML 和 MyBatis-Flex XML 是两套独立资源；默认数据源 key 明确为 `default`。
- MyBatis-Plus 和 MyBatis-Flex 都支持 db 的完整动态 SQL 能力。
- Class 化 Join Wrapper 只支持查询、分页和统计，不支持 Join 更新。
- 复杂动态 SQL 走完整 SQL，不提供局部拼接式 Join 入口。

## 安全

Java 源码拥有宿主 JVM 的代码执行权限。只允许受信任的 Magic-API 管理员编辑和激活 `java-entity` Resource；插件不提供同 JVM 内的不可信代码沙箱。
