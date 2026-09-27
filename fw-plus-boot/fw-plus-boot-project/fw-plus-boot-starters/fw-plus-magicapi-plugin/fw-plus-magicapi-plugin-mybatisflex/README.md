# Magic-API MyBatis-Flex XML 插件

`fw-plus-magicapi-plugin-mybatisflex` 是独立的 Magic-API 资源树插件，用来在控制台维护 `mybatisflex-xml` 资源。

该插件的核心定位是：通过 Magic-API 资源树创建标准 MyBatis Mapper XML，然后让 Magic-Script 里的 `mybatisflex` 模块按 XML statement 执行查询、更新、分页和动态 SQL；同时保留 MyBatis-Flex 原生 `Row`、`QueryWrapper`、分页和 join 能力。

## 模块信息

- Maven 模块：`fw-plus-magicapi-plugin-mybatisflex`
- 资源类型：`mybatisflex-xml`
- 本模块内容：只包含 Magic-API 控制台前端资源，不包含后端 Java 源码
- 后端实现位置：`fw-plus-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex`
- 前端工程：`src/console`
- 前端入口：`MagicMybatisFlexXml`
- 前端构建产物：`magic-mybatisflex-xml.1.0.0.iife.js`

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-magicapi-plugin-mybatisflex</artifactId>
</dependency>
```

本模块仅提供 Magic-API 侧的 MyBatis-Flex 能力，不强制传递 MyBatis-Flex 装配依赖（`mybatis-flex-spring` 为 `optional`）。宿主需自行引入并保证其在 classpath 上：

```xml
<dependency>
    <groupId>com.mybatis-flex</groupId>
    <artifactId>mybatis-flex-spring</artifactId>
</dependency>
```

## 资源树使用

控制台资源树会新增 `mybatisflex-xml` 类型。新增资源时填写：

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| 名称 | 是 | 资源显示名称 |
| 路径 | 是 | Magic-API 资源路径 |
| 启用 | 是 | `enabled=true` 时注册 XML statement，关闭后只保存资源，不注册 |
| 数据源 key | 否 | 默认数据源固定写作 `default`；留空保存时后端也会归一为 `default` |
| 表名 | 是 | 必须是当前数据源中真实存在的表或视图 |
| 描述说明 | 否 | 建议写清楚 namespace、表、主要 statement 用途 |
| XML 源码 | 是 | 完整 MyBatis Mapper XML |

重要约束：

- `dbKey` 创建后不可修改。
- 同一资源类型下，`dbKey + tableName` 必须唯一。
- `dbKey` 必须能匹配 Magic-API 已存在的数据源。
- `tableName` 必须能在对应数据源中查到。
- 删除 datasource 时，如果仍有 `mybatisflex-xml` 引用该 `dbKey`，会拒绝删除。

默认数据源说明：

> Magic-API 默认数据源没有显式 key。为了让 XML Resource 有稳定标识，本插件统一使用 `default` 表示默认数据源。前端不填 `dbKey` 时，后端也按 `default` 保存和注册。

## XML 校验和注册

保存 XML 时会使用 MyBatis 自带能力校验：

- `XPathParser`
- `XMLMapperEntityResolver`
- `XMLMapperBuilder`

所以 XML 需要是标准 MyBatis Mapper XML，最外层必须是 `<mapper>`。

注册时会把资源里的 logical namespace 映射到当前版本的实际 namespace，Magic-Script 调用时仍然使用逻辑 statement id。更新 XML 后会重新注册新的 statement，脚本侧不需要改 statement 名称。

## Java Entity 关系

`mybatisflex` 可以直接使用动态 Java Entity 插件生成的实体类，也可以使用普通 Java 实体类。

实体类可以不加注解；如果需要映射表名和字段名，建议使用 MyBatis-Flex 注解。

Flex 和 Plus 的脚本写法保持统一：脚本里写 Java 属性名，插件内部再解析数据库列名。有 `@Column("列名")` 时使用注解列名；没有写注解列名时使用属性名本身；`@Column(ignore = true)` 不作为数据库字段。动态 wrapper 条件里写到 `ignore = true` 字段或未知属性时会直接跳过该条件，不会抛错。

```java
package demo;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

@Data
@Table("sys_user")
public class User {
    @Id
    private Long id;

    @Column("user_name")
    private String userName;

    @Column("dept_id")
    private Long deptId;

    @Column("status")
    private Integer status;
}
```

在 Magic-Script 中获取动态实体类：

```javascript
import entity

var User = entity['demo.User']
var user = User.create()
user.setId(1)
user.setUserName('张三')
```

`User.create()` 可以不传参数，表示调用无参构造器创建空实体；需要一次性写入属性时使用 `User.create({ id: 1, userName: '张三' })`。`User.create(null)` 等同于空参数，但推荐直接写 `User.create()`。

`mybatisflex-xml` 支持在 XML 中直接引用动态 Java Entity：

```xml
<insert id="insertUser" parameterType="demo.User">
    INSERT INTO sys_user (id, user_name)
    VALUES (#{id}, #{userName})
</insert>

<select id="selectUserById" resultType="demo.User">
    SELECT id, user_name AS userName
    FROM sys_user
    WHERE id = #{id}
</select>
```

保存和注册 XML 时会把动态实体注册到 MyBatis `TypeAliasRegistry`，所以 `parameterType="demo.User"`、`resultType="demo.User"` 可以解析到动态编译出来的实体 Class。

如果简单类名没有冲突，也可以写 `parameterType="User"`、`resultType="User"`；有重名动态实体时请使用全限定类名。

## 完整 XML 示例

namespace 默认建议使用实体类全限定名，例如 `demo.User`。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="demo.User">

    <sql id="Base_Column_List">
        id,
        user_name AS userName,
        dept_id AS deptId,
        status,
        create_time AS createTime
    </sql>

    <insert id="insertUser" parameterType="demo.User">
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

    <update id="updateUserById" parameterType="demo.User">
        UPDATE sys_user
        <set>
            <if test="userName != null">user_name = #{userName},</if>
            <if test="deptId != null">dept_id = #{deptId},</if>
            <if test="status != null">status = #{status},</if>
        </set>
        WHERE id = #{id}
    </update>

    <delete id="deleteUserById">
        DELETE FROM sys_user WHERE id = #{id}
    </delete>

    <select id="selectUserById" resultType="demo.User">
        SELECT <include refid="Base_Column_List"/>
        FROM sys_user
        WHERE id = #{id}
    </select>

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
            <if test="deptIds != null and deptIds.size() > 0">
                AND dept_id IN
                <foreach collection="deptIds" item="deptId" open="(" separator="," close=")">
                    #{deptId}
                </foreach>
            </if>
        </where>
    </select>

</mapper>
```

## Magic-Script 调用 XML

默认数据源：

```javascript
import entity
import mybatisflex

var User = entity['demo.User']
var mapper = mybatisflex.mapper(User.javaClass())

return mapper.selectList('selectByCondition', {
    status: body.status,
    keyword: body.keyword,
    deptIds: body.deptIds
})
```

指定数据源：

```javascript
var mapper = mybatisflex['master'].mapper(User.javaClass())
return mapper.selectOne('selectUserById', body.id)
```

XML namespace 不等于实体类全限定名时：

```javascript
var mapper = mybatisflex['master'].mapper(User.javaClass(), 'magic.flex.user')
return mapper.selectList('selectByCondition', { status: 1 })
```

也可以传完整 statement id：

```javascript
return mapper.selectList('magic.flex.user.selectByCondition', { status: 1 })
```

## XML 增删改查

`mybatisflex.mapper(User.javaClass())` 主要用于调用 XML statement：

```javascript
mapper.insert('insertUser', user)
mapper.update('updateUserById', user)
mapper.delete('deleteUserById', user.getId())
mapper.selectOne('selectUserById', user.getId())
mapper.selectList('selectByCondition', { status: 1 })
```

这些 statement 名称完全由 XML 决定，不要求叫 `insert`、`updateById`、`selectById`。

## 分页

XML 自定义查询分页：

```javascript
var page = mapper.page('selectByCondition', {
    status: body.status,
    keyword: body.keyword
}, 1, 10)

return {
    pageNumber: page.getPageNumber(),
    pageSize: page.getPageSize(),
    totalRow: page.getTotalRow(),
    records: page.getRecords()
}
```

Flex 动态 QueryWrapper 分页：

```javascript
var query = mybatisflex.query(User.javaClass(), 'u')
    .eq(body.status != null, 'u', 'status', body.status)
    .like(body.keyword != null && body.keyword != '', 'u', 'userName', body.keyword)
    .orderByDesc('u', 'id')

return mybatisflex.paginate(1, 10, query)
```

`@Column(ignore = true)` 字段不是表字段，作为动态条件字段时会被跳过。

## 动态 SQL 能力

复杂 SQL 不需要在 Magic-Script 手动拼接片段，直接写完整 XML：

- `<if>`
- `<choose>`
- `<where>`
- `<set>`
- `<trim>`
- `<foreach>`
- `<sql>`
- `<include>`
- 子查询
- `EXISTS`
- `GROUP BY`
- `HAVING`
- `ORDER BY`

示例：

```xml
<select id="selectDeptUserCount" parameterType="map" resultType="map">
    SELECT d.id AS deptId, d.dept_name AS deptName, COUNT(u.id) AS userCount
    FROM sys_dept d
    LEFT JOIN sys_user u ON u.dept_id = d.id
    <where>
        <if test="status != null">
            AND u.status = #{status}
        </if>
        <if test="keyword != null and keyword != ''">
            AND EXISTS (
                SELECT 1
                FROM sys_user su
                WHERE su.dept_id = d.id
                  AND su.user_name LIKE CONCAT('%', #{keyword}, '%')
            )
        </if>
    </where>
    GROUP BY d.id, d.dept_name
    <if test="minCount != null">
        HAVING COUNT(u.id) &gt;= #{minCount}
    </if>
</select>
```

## 完整 SQL 能力

`mybatisflex` 模块保留 Magic-API `db` 的完整 SQL 能力，也保留 Flex Row SQL 方法，适合复杂查询、子查询、按关联表条件更新等场景。

db 风格方法包括：

- `select / selectOne / selectValue / selectInt / count`
- `page`
- `insert / update / batchUpdate`
- `call`
- `table`
- `cache / deleteCache`
- `camel / pascal / lower / upper / normal / columnCase`

Flex Row SQL 方法包括：

- `selectListBySql`
- `selectOneBySql`
- `selectObjectBySql`
- `insertBySql`
- `updateBySql`
- `deleteBySql`

这些方法支持 Magic-API db 的动态 SQL 拼接语法，例如 `?{condition, sql}`、`#{param}`，也支持 datasource 动态属性，例如 `mybatisflex['slave'].select(...)`。

动态查询示例：

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

Flex Row SQL 查询：

```javascript
var rows = mybatisflex.selectListBySql("""
    select id, user_name as userName
    from sys_user
    where exists (
        select 1
        from sys_dept d
        where d.id = sys_user.dept_id
          and d.dept_name = ?
    )
""", body.deptName)

return rows.stream()
    .map(row => User.create(row))
    .toList()
```

按关联表条件修改主表：

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

## Flex Join 使用

动态实体 join 不要求在脚本里写表名和字段名。使用 Class、别名和 Java 属性名；列名映射规则与 `mybatisflex.query(User.javaClass(), 'u')` 一致：

```javascript
import entity
import mybatisflex

var User = entity['demo.User']
var Dept = entity['demo.Dept']
var UserDeptVO = entity['demo.UserDeptVO']

var query = mybatisflex.query(User.javaClass(), 'u')
    .leftJoin(Dept.javaClass(), 'd', 'u', 'deptId', 'id')
    .selectAs(User.javaClass(), 'u', 'id', 'id')
    .selectAs(User.javaClass(), 'u', 'userName', 'userName')
    .selectAs(Dept.javaClass(), 'd', 'deptName', 'deptName')
    .eq(body.status != null, 'u', 'status', body.status)
    .like(body.keyword != null && body.keyword != '', 'u', 'userName', body.keyword)
    .orderByDesc('u', 'id')

return mybatisflex.paginate(1, 10, query)
```

如果 VO 字段名和实体字段名一致，可以用 `select(VO.class)` 自动按已注册 alias 匹配字段：

```javascript
var query = mybatisflex.query(User.javaClass(), 'u')
    .leftJoin(Dept.javaClass(), 'd', 'u', 'deptId', 'id')
    .select(UserDeptVO.javaClass())

return mybatisflex.paginate(1, 10, query)
```

支持的动态条件方法：

- `eq(condition, alias, property, value)`
- `ne(condition, alias, property, value)`
- `like(condition, alias, property, value)`
- `ge(condition, alias, property, value)`
- `le(condition, alias, property, value)`
- `gt(condition, alias, property, value)`
- `lt(condition, alias, property, value)`
- `in(condition, alias, property, values)`
- `isNull(condition, alias, property)`
- `isNotNull(condition, alias, property)`
- `groupBy(alias, property)`
- `orderByAsc(alias, property)`
- `orderByDesc(alias, property)`

## Java Entity 转 Row / Map

转换为 Flex `Row`：

```javascript
var row = mybatisflex.toRow(user)
var rows = mybatisflex.toRows([user1, user2])
```

转换为数据库列名 Map：

```javascript
var map = mybatisflex.toMap(user)
var maps = mybatisflex.toMaps([user1, user2])
```

`toRow`、`toMap` 会识别 MyBatis-Flex 注解，并输出数据库列名作为 Row/Map key；没有注解列名时使用 Java 属性名：

- `@Id`
- `@Column`
- `@Column(ignore = true)`

## Flex 原生 CRUD

动态 Java Entity 转成 `Row` 后，可以继续使用 Flex 原生 Row 能力：

```javascript
var row = mybatisflex.toRow(user)
mybatisflex.insert('sys_user', row)
```

普通查询优先使用动态 wrapper：

```javascript
var query = mybatisflex.query(User.javaClass(), 'u')
    .eq(body.status != null, 'u', 'status', body.status)

return mybatisflex.selectListByQuery('sys_user', query.unwrap())
```

具体可用方法以 `@MagicModule("mybatisflex")` 对应的 `MybatisFlexModule` 为准。

## 事务

```javascript
return mybatisflex.transaction(() => {
    mapper.insert('insertUser', user)
    mapper.update('updateUserById', user2)
    return true
})
```

## 注意事项

- 本模块自身不放后端 Java 代码；后端注册、校验、执行逻辑在 `fw-plus-boot-autoconfigure`。
- XML 是完整 MyBatis Mapper XML，不是 SQL 片段资源。
- `mapper(User.javaClass())` 默认 namespace 是实体类全限定名。
- `mapper(User.javaClass(), namespace)` 用于 XML namespace 和实体类名不一致的场景。
- 脚本侧复杂 SQL 优先写 XML，Wrapper 适合简单条件、分页和 join 查询。
- 默认数据源 key 固定使用 `default`。
