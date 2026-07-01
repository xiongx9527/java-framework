---
name: magic-api
description: Magic-API magic-script 脚本编写专家，覆盖语法、SQL、Fluent API、HTTP、LINQ、Java 互操作、集合操作、事务缓存等所有领域。
origin: fw-plus
reference: https://www.ssssssss.org/magic-api/
---

# Magic-API 脚本编写

使用此 skill 编写 magic-script 脚本代码。magic-script 是 magic-api 框架的脚本引擎，语法类似 JavaScript 但有独特规则，必须严格遵守。

> **参考来源**：[Magic-API 官方文档](https://www.ssssssss.org/magic-api/) — 更新此 skill 时应同时参考官方文档和项目实际代码。

## 核心语法

- 变量定义：`var name = 'hello'`（var 可省略）；`const val = 1`（常量）；语句末尾分号可选
- 字符串插值：`` `Hello ${name}` ``
- 多行字符串：三引号 `"""..."""`（常用于 SQL）
- 条件：`if / else if / else`
- 循环：`for(item in list)`、`for(key, value in map)`、`for(i in range(0, 10))`、`while`
- Lambda：`e => e + 1`、`(a, b) => a + b`、`(a, b) => { return a + b }`
- 可选链：`obj?.prop`（不抛异常，返回 null）
- 展开运算符：`{...map, key: val}`、`[...list]`
- 类型转换：`value::int`、`value::string`、`value::date('yyyy-MM-dd')`、`value::json`、`value::stringify`
- 扩展方法：`value.asString()`、`value.asInt()`、`123.45.toFixed(2)`、`0.5.asPercent(2)`
- exit 语句：`exit 400, '错误信息'` 直接终止并返回指定状态码
- try/catch/finally：`catch(e)` 中用 `e.getMessage()` 获取错误信息

## 请求参数获取

magic-api 自动将请求参数注入脚本作用域，支持两种模式：

### 默认模式（参数直接注入）

GET 参数、路径变量、Header 等直接以变量名访问：

- **GET 参数** `/user?name=xx`：直接写 `name` 即可
- **路径变量** `/user/{id}`：直接写 `id` 即可
- **POST 请求体**：`body` 是整个请求体对象，`body.name` 访问字段
- **请求头**：`header.Authorization`、`header.token`
- **路径变量（显式）**：`path.id`

```magicscript
// 路径变量
var userId = id

// Query 参数
var userName = name

// 请求体（POST JSON）
var data = body
var userName = body.name

// 请求头
var token = header.token
var auth = header.Authorization
```

### 包装模式（`wrap_request_parameter`）

在接口选项中设置 `wrap_request_parameter` 为一个变量名（如 `query`），所有请求参数会被包装成一个 Map 注入到该变量中，通过 `query.xx` 访问。此模式可避免参数名与作用域中其他变量冲突：

```magicscript
// 接口选项中配置 wrap_request_parameter = query 后：
// GET /user?name=xx&age=18  或  POST form-data

var userName = query.name     // 获取 name 参数
var userAge = query.age       // 获取 age 参数
var pageType = query.pageType // 获取 pageType 参数

// body 仍然是独立的请求体对象
var data = body
```

> **推荐使用包装模式**：当接口参数较多或参数名可能与内置变量（如 `log`、`env`）冲突时，使用 `wrap_request_parameter` 将参数收敛到一个命名空间下，代码更清晰、更安全。

## 数据库参数绑定

SQL 中用 `#{varName}` 引用当前作用域的变量，不需要传参数 map：

```magicscript
var name = '张三'
var age = 25
// #{name} 和 #{age} 自动从作用域取值
db.insert('insert into user(name, age) values(#{name}, #{age})')
```

## 内置模块

必须用 `import` 导入后才能使用：

- `import log` → `log.info('msg: {}', arg)`
- `import env` → `env.get('server.port')`
- `import http` → HTTP 客户端
- `import request` → `request.getParameter('name')`、`request.getFile('file')`
- `import response` → `response.json()`、`response.download()`
- `import redis` → Redis 操作（插件）

`db` 模块无需导入，直接使用。

---

## 数据库 SQL 操作（db 模块）

### 参数绑定规则

- `#{varName}` 是预编译占位符，自动从当前作用域取变量值，防 SQL 注入
- `${varName}` 是字符串拼接，不防注入，慎用
- db 的所有方法只接受 SQL 字符串，不接受参数 map

```magicscript
// ✅ 正确：先定义变量，SQL 中用 #{} 引用
var name = body.name
var status = 1
db.select('select * from user where name = #{name} and status = #{status}')

// ❌ 错误：传参数 map（不支持）
// db.select('select * from user where name = #{name}', {name: 'xx'})
```

### 查询

```magicscript
var list = db.select('select * from user where status = #{status}')
var page = db.page('select * from user order by create_time desc')
var user = db.selectOne('select * from user where id = #{id}')
var count = db.selectInt('select count(*) from user')
var name = db.selectValue('select name from user where id = #{id}')
```

### 增删改

```magicscript
// 新增（返回影响行数）
db.insert('insert into user(name, age) values(#{name}, #{age})')

// 新增并返回自增主键
var newId = db.insert('insert into user(name) values(#{name})', 'id')

// 更新
db.update('update user set name = #{name} where id = #{id}')

// 删除
db.update('delete from user where id = #{id}')
```

### 数组参数自动展开

```magicscript
var ids = [1, 2, 3]
return db.select('select * from user where id in (#{ids})')
```

### 条件片段 `?{}` 语法

```magicscript
return db.select('select * from user where 1=1 ?{name, and name like concat(\'%\',#{name},\'%\')} ?{status, and status = #{status}}')
```

### 命名风格转换

```magicscript
return db.camel().select('select user_name, create_time from user')
// 返回 [{userName: '...', createTime: '...'}]
```

---

## Fluent Table API（db.table）

db.table() 提供链式操作，无需手写 SQL。

### 查询

```magicscript
// 查询所有
return db.table('user').select()

// 指定列 / 排除列
return db.table('user').columns('id', 'name', 'age').select()
return db.table('user').exclude('password').select()

// 条件查询
return db.table('user').where().eq('status', 1).like('name', '%张%').select()

// 分页 / 单条 / 计数 / 存在性
return db.table('user').where().eq('status', 1).page()
return db.table('user').where().eq('id', id).selectOne()
return db.table('user').where().eq('status', 1).count()
return db.table('user').where().eq('name', name).exists()
```

### 条件方法

```magicscript
.where()
    .eq('status', 1)          // = 等于
    .ne('status', 0)          // != 不等于
    .gt('age', 18)            // > 大于
    .ge('age', 18)            // >= 大于等于
    .lt('age', 60)            // < 小于
    .le('age', 60)            // <= 小于等于
    .like('name', '%关键字%')  // LIKE 模糊
    .in('id', [1, 2, 3])     // IN 列表
    .isNull('deleted_at')     // IS NULL
    .isNotNull('name')        // IS NOT NULL
    .and()                    // AND 分组
    .or()                     // OR 分组
```

`.set()` 用于配合 `.where()` 按条件更新指定字段：

```magicscript
return db.table('user')
    .where().eq('id', id)
    .set('name', newName)
    .set('status', newStatus)
    .update()
```

### 排序与分组

```magicscript
return db.table('user')
    .where().eq('status', 1)
    .groupBy('dept_id')
    .orderBy('create_time', 'desc')
    .select()
```

### 新增

```magicscript
// 自增主键
return db.table('user').primary('id').insert({name: '张三', age: 25, status: 1})

// UUID 主键
return db.table('user').primary('id', () => uuid()).insert({name: '张三', age: 25, status: 1})
```

### 修改 / 保存 / 删除

```magicscript
// 按主键修改
return db.table('user').primary('id').update({id: id, name: '李四', age: 30})

// 按条件修改（where + set + update）
return db.table('user')
    .where()
    .eq('id', body.id::int)
    .set('name', body.name)
    .set('status', body.status)
    .update()

// 保存（有则更新，无则插入）
return db.table('user').primary('id', () => uuid()).save(body)

// 删除
return db.table('user').where().eq('id', id).delete()

// 批量插入
var users = [{name: '张三', age: 20}, {name: '李四', age: 25}]
return db.table('user').primary('id', () => uuid()).batchInsert(users)

// 逻辑删除
return db.table('user').logic().where().eq('id', id).delete()
```

---

## 请求与响应处理

### request 对象

```magicscript
import request;

request.getHeader('Authorization')
request.getHeaders('Accept')   // 返回数组
request.getParameter('name')
request.getClientIP()

// 获取上传文件
var file = request.getFile('file')
var fileName = file.getOriginalFilename()
var fileBytes = file.getBytes()
```

### response 对象

```magicscript
import response;

// 返回 JSON（跳过框架默认包装）
response.json({code: 200, data: result})

// 返回纯文本
response.text('ok')

// 设置响应头
response.setHeader('X-Custom', 'value')

// 设置 Cookie
response.addCookie('token', jwtToken)

// 文件下载
response.download(fileBytes, '报表.xlsx')

// 返回图片
response.image(imageBytes, 'image/png')

// 分页结果
response.page(total, list)
```

### exit 快速返回

```magicscript
if (!name || name.trim() == '') {
    exit 400, '名称不能为空'
}
if (age::int < 0 || age::int > 150) {
    exit 400, '年龄不合法'
}

// 带数据返回
exit 200, '操作成功', {id: newId}
```

---

## 动态 SQL（MyBatis 风格）

在三引号 `"""..."""` SQL 中使用 XML 标签实现动态条件。

### `<where>` / `<if>` / `<elseif>` / `<else>` / `<set>` / `<foreach>`

```magicscript
// where + if 组合
return db.select("""
    select * from user
    <where>
        <if test="name != null and name != ''">
            and name like concat('%', #{name}, '%')
        </if>
        <if test="status != null">
            and status = #{status}
        </if>
    </where>
    order by create_time desc
""")

// if / elseif / else 条件分支
return db.select("""
    select * from user where
    <if test="type == 'vip'">
        level >= 3
    </if>
    <elseif test="type == 'normal'">
        level >= 1
    </elseif>
    <else>
        1 = 1
    </else>
""")

// set 标签（自动处理末尾逗号）
return db.update("""
    update user
    <set>
        <if test="name != null">name = #{name},</if>
        <if test="age != null">age = #{age},</if>
        <if test="status != null">status = #{status},</if>
    </set>
    where id = #{id}
""")

// foreach 批量
var idList = [1, 2, 3, 4, 5]
return db.select("""
    select * from user
    where id in
    <foreach collection="idList" item="item" open="(" separator="," close=")">
        #{item}
    </foreach>
""")
```

### 条件搜索 + 分页完整示例

```magicscript
return db.page("""
    select u.*, d.dept_name
    from user u
    left join department d on d.id = u.dept_id
    <where>
        <if test="keyword != null and keyword != ''">
            and (u.name like concat('%',#{keyword},'%')
                 or u.phone like concat('%',#{keyword},'%'))
        </if>
        <if test="deptId != null">
            and u.dept_id = #{deptId}
        </if>
        <if test="startDate != null">
            and u.create_time >= #{startDate}
        </if>
        <if test="endDate != null">
            and u.create_time &lt;= #{endDate}
        </if>
    </where>
    order by u.create_time desc
""")
```

---

## HTTP 客户端

```magicscript
import http;
```

### GET / POST / PUT / DELETE

```magicscript
// GET 带参数和请求头
var result = http.connect('https://api.example.com/users')
    .header('Authorization', 'Bearer ' + token)
    .param('page', 1)
    .param('size', 10)
    .get()
    .getBody()

// POST JSON
var result = http.connect('https://api.example.com/users')
    .contentType('application/json')
    .header('Authorization', 'Bearer ' + token)
    .body({name: '张三', age: 25})
    .post()
    .getBody()

// POST 表单
var result = http.connect('https://api.example.com/login')
    .contentType('application/x-www-form-urlencoded')
    .data('username', 'admin')
    .data('password', '123456')
    .post()
    .getBody()

// PUT / DELETE
http.connect(url).body(data).put().getBody()
http.connect(url).delete().getBody()
```

### 调用第三方 API 并处理结果

```magicscript
import http;

var apiResult = http.connect('https://api.example.com/data')
    .header('Authorization', 'Bearer ' + apiKey)
    .param('keyword', keyword)
    .get()
    .getBody()

if (apiResult.code != 200) {
    exit 500, '第三方接口调用失败: ' + apiResult.message
}

return apiResult.data
```

---

## LINQ 查询语法

类 SQL 的 LINQ 语法，对内存集合进行查询。

### 基本查询

```magicscript
var list = [{name: '张三', age: 20, sex: 0}, {name: '李四', age: 30, sex: 1}]

// 过滤
return select * from list t where t.age > 18

// 选择字段 + 转换
return select t.name, t.age > 18 ? '成年' : '未成年' status from list t
```

### 分组聚合

```magicscript
var data = db.select('select dept_id, salary from employee')

return
    select
        t.dept_id,
        count(t.dept_id) total,
        sum(t.salary) totalSalary,
        avg(t.salary) avgSalary
    from data t
    group by t.dept_id
    order by sum(t.salary) desc
```

### JOIN 关联

```magicscript
var users = db.camel().select('select name, dept_id from user')
var depts = db.camel().select('select id, name from department')

return select u.name, d.name from users u left join depts d on u.deptId = d.id
```

> **注意**：LINQ JOIN 中不用 `as` 关键字定义别名，直接用空格分隔（`users u` 而非 `users as u`）。使用 `db.camel()` 查询后，下划线列名会转为驼峰（如 `dept_id` → `deptId`），JOIN 条件中需用驼峰名。

### 空值处理

```magicscript
return select
    t.name || '未知' name,        // || 将 null/空字符串 替换
    ifnull(t.age, 0) age          // ifnull 仅处理 null
from list t
```

### 递归树形结构

```magicscript
var allDepts = db.select('select id, name, parent_id from department')

var toTree = (list, parentId) =>
    select t.*, toTree(list, t.id) children
    from list t
    where t.parent_id = parentId

return toTree(allDepts, '0')
```

### HAVING 过滤与子查询

```magicscript
// HAVING — 对分组聚合结果过滤
var orders = db.select('select * from orders')

return
    select
        t.category,
        count(t.id) orderCount,
        sum(t.amount) totalAmount
    from orders t
    group by t.category
    having sum(t.amount) > 1000
    order by totalAmount desc

// 子查询
return
    select * from (
        select
            t.dept_id,
            count(t.id) cnt
        from users t
        group by t.dept_id
    ) sub
    where sub.cnt > 5
    order by sub.cnt desc
```

---

## Java 互操作

### 导入 Java 类

```magicscript
import 'java.util.Date' as Date
import 'java.text.SimpleDateFormat' as SimpleDateFormat
import 'java.util.UUID' as UUID

var now = new Date()
var fmt = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
return fmt.format(now)
```

### 导入 Spring Bean

```magicscript
import 'org.springframework.core.env.Environment' as springEnv
return springEnv.getProperty('server.port')
```

### 类型转换

```magicscript
// :: 操作符（null 安全）
var num = '123'::int            // String → int
var str = 123::string           // int → String
var d = '2024-01-01'::date('yyyy-MM-dd')  // String → Date
var n = '3.14'::double          // String → double
var l = '999'::long             // String → long
var num = str::int(0)           // 带默认值

// JSON 转换
var obj = '{"name":"test"}'::json      // JSON 字符串 → 对象
var jsonStr = {name: 'test'}::stringify // 对象 → JSON 字符串

// 扩展方法
value.asString()
value.asInt()
value.asDouble()
```

### 实用示例：生成验证码图片

```magicscript
import 'java.awt.image.BufferedImage' as BufferedImage
import 'java.awt.Color' as Color
import 'java.awt.Font' as Font
import 'javax.imageio.ImageIO' as ImageIO
import 'java.io.ByteArrayOutputStream' as ByteArrayOutputStream
import response;

var width = 120
var height = 40
var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
var g = image.getGraphics()
g.setColor(Color.WHITE)
g.fillRect(0, 0, width, height)
g.setFont(new Font('Arial', Font.BOLD, 24))
g.setColor(Color.BLACK)
g.drawString('ABCD', 10, 30)
g.dispose()

var baos = new ByteArrayOutputStream()
ImageIO.write(image, 'png', baos)
response.image(baos.toByteArray(), 'image/png')
```

### 导入自定义函数

```magicscript
// 调用其他 magic-api 函数
import '@/utils/add' as add
return add(1, 2)

// 调用其他 API 接口
import '@get:/api/user/list' as getUserList
return getUserList()
```

---

## 集合操作

### List 遍历与转换

```magicscript
list.each(it => log.info('name: {}', it.name))
var names = list.map(it => it.name)
var young = list.filter(it => it.age < 25)
var found = list.find(it => it.name == '张三')
```

### 排序 / 聚合 / 截取

```magicscript
var sorted = list.sort((a, b) => a.age - b.age)
var unique = list.distinct()

ages.sum()      // 150
ages.avg()      // 30
ages.max()      // 40
ages.min()      // 20

list.skip(2).limit(5)  // 跳过2个取5个
list.concat(list2)     // 合并列表
```

### 分组

```magicscript
var grouped = list.group(it => it.dept)
var deptCount = list.group(it => it.dept, items => items.size())
```

### 关联（Join）

```magicscript
var result = users.join(depts, (u, d) => u.dept_id == d.id)
    .map(it => { name: it.name, deptName: it.dept_name })
```

### Map / List 互转

```magicscript
var userMap = list.toMap(it => it.id)
var list = map.asList((key, value) => {k: key, v: value})
```

### Map 排序

```magicscript
var map = {a: 1, b: 2, c: 3}
// 按值降序排列，返回新 Map
map.sort((k1, k2, v1, v2) => v2 - v1)
// 按值升序
map.sort((k1, k2, v1, v2) => v1 - v2)
```

### 树结构转换

```magicscript
var toTree = (list, parentId) => {
    return list.filter(it => it.parent_id == parentId)
        .each(it => {
            it.children = toTree(list, it.id)
        })
}
return toTree(allDepts, '0')
```

---

## 进阶特性

### 异步执行

```magicscript
var future = async () => {
    return db.select('select * from big_table')
}
var result = future.get()  // 阻塞获取

// 并行查询
var futures = []
for(id in ids) {
    futures.add(async (id) => db.selectOne('select * from user where id = #{id}'))
}
return futures.map(f => f.get())
```

### Lambda 函数

```magicscript
var double = e => e * 2
var add = (a, b) => a + b
var process = (item) => {
    var result = item.value * 2
    return {name: item.name, value: result}
}

list.sort((a, b) => a - b)
list.filter(it => it > 2)
list.map(it => it * 10)
```

### 可选链 `?.`

```magicscript
var city = user?.address?.city    // 遇到 null 返回 null，不抛异常
var name = list?.first()?.name
```

### 展开运算符 `...`

```magicscript
var base = {name: '张三', age: 20}
var extended = {...base, dept: 'IT', age: 25}  // age 被覆盖

var list2 = [0, ...list1, 4, 5]  // [0, 1, 2, 3, 4, 5]
```

### 日志 / 环境变量 / Redis

```magicscript
import log;
log.info('用户{}登录成功', username)
log.warn('库存不足: 商品{}, 剩余{}', productId, stock)
log.error('操作失败', exception)

import env;
var port = env.get('server.port')
var dbUrl = env.get('spring.datasource.url')

import redis;
redis.setex('user:token:' + userId, 3600, token)
var token = redis.get('user:token:' + userId)
redis.del('user:token:' + userId)
```

---

## 事务与缓存

### 自动事务

```magicscript
var result = db.transaction(() => {
    db.update('update account set balance = balance - #{amount} where id = #{fromId}')
    db.update('update account set balance = balance + #{amount} where id = #{toId}')
    return '转账成功'
})
```

### 手动事务

```magicscript
var tx = db.transaction()
try {
    db.insert('insert into order_log(order_id, action) values(#{orderId}, "create")')
    db.update('update product set stock = stock - #{qty} where id = #{productId}')
    tx.commit()
    return '操作成功'
} catch(e) {
    tx.rollback()
    exit 500, '操作失败: ' + e.getMessage()
}
```

### 缓存查询

```magicscript
// 使用缓存，key='user'，有效期 60000ms
return db.cache('user', 60000).select('select * from user')

// 更新时自动清除对应缓存
db.cache('user').update('update user set name = #{name} where id = #{id}')

// 手动清除缓存
db.deleteCache('user')
```

### 多数据源

```magicscript
return db.slave.select('select * from user')
var dsKey = 'report_db'
return db[dsKey].select('select * from statistics')
return db.slave.table('user').where().eq('status', 1).select()
```

---

## 绝对禁止的错误写法

以下写法在 magic-script 中不存在，使用会导致脚本报错：

| # | 错误写法 | 正确写法 |
|---|---------|---------|
| 1 | `db.select(sql, {key: value})` | 先定义 `var key = value`，SQL 中用 `#{key}` |
| 2 | `query.name`（未配置 `wrap_request_parameter` 时） | 直接用变量名 `name`（GET 参数自动注入作用域），或配置 `wrap_request_parameter = query` 后使用 `query.name` |
| 3 | `request.parameters.name` | 直接用变量名，或 `import request` 后用 `request.getParameter('name')` |
| 4 | `new Date()` 未导入直接用 | `import 'java.util.Date' as Date; var now = new Date()` |
| 5 | `env.get('key')` 未导入直接用 | `import env; env.get('key')` |
| 6 | `.toLocaleString()`、`JSON.stringify()`、`Math.ceil()` | 用 magic-script 方法：`obj::stringify`、`str::json`、`num.ceil()` |
| 7 | `e.message` | `e.getMessage()` |
| 8 | `a ?: b`（Elvis 运算符） | `a != null ? a : b` |
| 9 | 自己写 `LIMIT #{offset}, #{size}` | 用 `db.page('select ... from ...')` |
| 10 | `return {code: 200, message: 'success', data: result}` | `return result`（框架自动包装） |
| 11 | `body.id.trim()`（id 是数字时报错） | `body.id == null \|\| !body.id`、`body.name == null \|\| body.name == ''` |
| 12 | 自增主键用 `uuid()` | `db.table('xxx').primary('id').insert(data)` |
| 13 | `update_time = now()` | SQL 中用 `now()` 或 Java：`import 'java.util.Date' as Date; var now = new Date()` |
| 14 | `return` 和 `select` 分行 | `return select xxx from ...`（同一行） |
| 15 | `delete updateData.id` | 用筛选重建：`var updateData = body.filter((k, v) => k != 'id')` |

---

## 输出规则

- 代码用 ` ```magicscript ` 包裹
- 脚本顶层直接执行，不需要 main 函数
- 最终结果用 `return` 返回
- 直接 return 数据，框架自动包装为 `{code:1, message:'success', data: 你的返回值}`
- 注释简洁，使用中文注释

## 标准代码模式

### 查询列表 + 分页 + 条件搜索

```magicscript
return db.page("""
    select * from user
    <where>
        <if test="name != null and name != ''">and name like concat('%',#{name},'%')</if>
        <if test="status != null">and status = #{status}</if>
    </where>
    order by create_time desc
""")
```

### 新增接口（自增主键）

```magicscript
if (!body.name || body.name == '') {
    exit 400, '名称不能为空'
}
return db.table('user').primary('id').insert(body)
```

### 根据 ID 查询

```magicscript
var user = db.selectOne('select * from user where id = #{id}')
if (!user) {
    exit 404, '用户不存在'
}
return user
```

### 调用外部接口

```magicscript
import http
var result = http.connect('https://api.example.com/data')
    .header('Authorization', 'Bearer ' + token)
    .param('city', city)
    .get()
    .getBody()
return result
```

---

## fw-plus 项目专属约定

### 数据源 Key 命名与环境切换

数据源 key 按功能用拼音命名（如 `services`、`evaluation`、`mysql`），通过环境后缀动态切换：

```magicscript
import '@/public/getEnv' as getEnv;

var env = getEnv();  // 返回 _dev、_uat、_pro 等环境后缀
// 实际访问 db['services_dev'] 或 db['services_uat']
var userList = db['services' + env].select('select * from user')
```

### import 路径约定

| 路径格式 | 说明 | 示例 |
|---------|------|------|
| `import '@/xxx/yyy' as zzz` | 引用其他 Magic-API 函数 | `import '@/public/getEnv' as getEnv` |
| `import '@get:/api/xxx' as xxx` | 引用其他 API 接口（GET 方法） | `import '@get:/api/user/list' as getUserList` |
| `import '全限定类名' as Xxx` | 引用 Java 类 | `import 'com.alibaba.fastjson.JSON' as JSON` |
| `import 内置模块名` | 引用 Magic-API 内置模块 | `import log`、`import request` |

### 获取环境信息（项目标准函数）

项目中使用 `@/public/getEnv` 函数根据请求头 `env` 参数拼接环境后缀：

```magicscript
import request;
var tmpEnv = "";
if (request.getHeaders("env")) {    
    tmpEnv = "_" + request.getHeaders("env")[0];
}
return tmpEnv;
```

### 配置中心读取配置值

通过 `SpringUtils.getAppConfig()` 从 Spring 配置/配置中心（Apollo/Nacos）动态获取配置。所有中间件连接信息优先从配置中心读取，配置 key 格式为：

```
magic.api.<type>.<dataSourceKey>.<configKey>
```

支持的配置类型和配置项：

| 类型 | 配置前缀 | 配置项示例 |
|------|---------|-----------|
| db | `magic.api.db.{key}.` | url, username, password, driverClassName |
| redis | `magic.api.redis.{key}.` | host, port, password, database |
| mongo | `magic.api.mongo.{key}.` | url, database |
| elasticsearch | `magic.api.elasticsearch.{key}.` | host, port |
| kafka | `magic.api.kafka.{key}.` | bootstrapServers |
| kafka 订阅 | `magic.api.kafka.subscribe.{topic}.` | groupId, topics |
| rocketmq | `magic.api.rocketmq.{key}.` | nameServer |
| rabbitmq | `magic.api.rabbitmq.{key}.` | host, port, username, password |
| pulsar | `magic.api.pulsar.{key}.` | serviceUrl |
| s3 | `magic.api.s3.{key}.` | endpoint, accessKey, secretKey |
| ws | `magic.api.ws.{key}.` | host, port |
| sse | `magic.api.sse.{key}.` | host, port |
| mqtt | `magic.api.mqtt.{key}.` | url, clientId |
| mqttclient | `magic.api.mqttclient.{key}.` | url, clientId |
| debezium | `magic.api.debezium.{key}.` | 连接器配置 |
| influx | `magic.api.influx.{key}.` | url, database |
| dtp | `magic.api.dtp.{key}.` | 线程池配置 |
| task | `magic.api.task.{key}.` | Cron 表达式 |
| dify | `magic.api.dify.{key}.` | url, apiKey |

```magicscript
import 'com.xx9527.fw.plus.common.util.SpringUtils' as SpringUtils;

// 读取数据库连接配置
var dbUrl = SpringUtils.getAppConfig('magic.api.db.services.url');

// 读取 Redis 配置
var redisHost = SpringUtils.getAppConfig('magic.api.redis.cache.host');

// 读取 Dify 配置
var difyUrl = SpringUtils.getAppConfig('magic.api.dify.default.url');
var difyKey = SpringUtils.getAppConfig('magic.api.dify.default.apiKey');
```

---

## fw-plus 自定义 @MagicModule 扩展

项目注册了多个自定义模块，通过 `@MagicModule` 注解注入到 magic-script 作用域。按层次分布：

### Boot 层业务模块

#### dify — Dify AI 平台集成

```magicscript
import dify;

// 调用 Dify 工作流
var result = dify.workflows.run({
    inputs: {query: userQuestion},
    response_mode: 'blocking',
    user: userId
});

// 调用对话应用
var chatResult = dify.chatMessages.send({
    query: '你好',
    conversation_id: conversationId,
    user: userId
});
```

#### lock — 分布式锁

```magicscript
import lock;

// 尝试获取锁（获取超时 5 秒，锁过期 30 秒）
if (lock.tryLock('order:' + orderId, 5000, 30000)) {
    try {
        // 业务逻辑
        return '操作成功';
    } finally {
        lock.unlock('order:' + orderId);
    }
} else {
    exit 429, '操作太频繁，请稍后再试';
}
```

#### mybatisflex — MyBatis-Flex 数据库操作

通过 `db` 变量直接使用（提供完整 CRUD、分页、事务、动态数据源）：

```magicscript
// 与 db 模块语法兼容，自动根据数据源 key 切换
var user = db['services' + env].selectOne('select * from user where id = #{id}');

// 动态数据源
var slaveData = db.slave.select('select * from statistics');
```

#### template — 文档模板解析

```magicscript
import template;

// 基于 URL 的 Word 模板填充
var docBytes = template.processByUrl(
    'https://oss.example.com/templates/contract.docx',
    {name: '张三', date: '2024-01-01', amount: 10000}
);

// 基于字节数组的 PDF 模板填充
var pdfBytes = template.processByBytes(fileBytes, templateData);
```

#### uid — 雪花算法分布式 ID

```magicscript
import uid;

var newId = uid.getUID();   // 生成全局唯一 ID
var info = uid.parseUID(newId);  // 解析 UID
```

#### trace — Plumelog 链路追踪

```magicscript
import trace;

trace.startTraceLog('业务操作');
// ... 业务逻辑 ...
trace.endTraceLog();

var traceId = trace.getTraceId();
var spanId = trace.getSpanId();
```

#### ssehttp — SSE 客户端

```magicscript
import ssehttp;

// 消费 SSE 事件流
ssehttp.connect('https://api.example.com/events')
    .header('Authorization', 'Bearer ' + token)
    .onMessage((event) => {
        log.info('收到 SSE 事件: {}', event.data());
    })
    .get();
```

### Boot 层渲染与执行模块

#### dtp — 动态线程池并行执行

```magicscript
import dtp;

// 在指定线程池中执行任务
dtp.execute(() => {
    // 异步执行的逻辑
    db.update('update log set status = 1 where id = #{id}');
});

// 并行执行多个任务，合并所有结果
var results = dtp.allOfWithAllResults([
    () => db.select('select * from user'),
    () => db.select('select * from department'),
    () => db.select('select * from role')
]);
```

#### freemarker — Freemarker 模板渲染

```magicscript
import freemarker;

// 基于 URL 的模板渲染
var html = freemarker.processByUrl(
    'https://oss.example.com/templates/report.ftl',
    {title: '报表', data: reportData}
);
```

#### vue3 — Vue3 页面渲染

将 Vue3 SFC 代码作为 API 接口响应渲染（支持 `<template>` + `<script setup>` + `<style scoped>`）：

```magicscript
import vue3;
// vue3 模块将当前脚本的响应渲染为 Vue3 页面
```

示例脚本（完整的 Vue3 SFC 直接在 magic-script 中编写）：
```magicscript
<template>
  <div class="login-container">
    <h1>{{ title }}</h1>
    <button @click="handleLogin">登录</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const title = ref('欢迎登录')
const handleLogin = () => {
  // 登录逻辑
}
</script>

<style scoped>
.login-container { padding: 20px; }
</style>
```

#### java — 动态 Java 类加载

```magicscript
import java;

// 动态编译并加载 Java 类
var instance = java.load('@/java/test/MyClass.java');
var result = instance.process(data);
```

### AI 层向量数据库模块

#### langchain4j — LLM 集成

```magicscript
import langchain4j;

// 调用大语言模型（支持数据源 key 动态切换）
var response = langchain4j['default'].chat({
    messages: [{role: 'user', content: '你好'}]
});
```

#### qdrant — Qdrant 向量数据库

```magicscript
import qdrant;

// 向量相似度检索（支持数据源 key 动态切换）
var results = qdrant['default'].search({
    collection: 'documents',
    vector: embeddingVector,
    limit: 10
});
```

#### milvus — Milvus 向量数据库

```magicscript
import milvus;

// 向量相似度检索
var results = milvus['default'].search({
    collection: 'knowledge_base',
    vector: embeddingVector,
    limit: 5
});
```

#### weaviate — Weaviate 向量数据库

```magicscript
import weaviate;

// 向量相似度检索
var results = weaviate['default'].search({
    collection: 'articles',
    vector: embeddingVector,
    limit: 10
});
```

> **注意**：所有向量数据库模块均支持 `DynamicAttribute + DynamicModule`，可通过 `模块名['数据源key']` 切换不同实例，数据源连接信息从配置中心读取。

---

## fw-plus 增强接口 Options

在 Magic-API 原生选项基础上，扩展了权限控制、参数校验、Sentinel 流量控制等企业级选项。在接口编辑器的"选项"面板中配置：

### 基础选项

| 选项 key | 默认值 | 说明 |
|---------|--------|------|
| `require_login` | `true` | 该接口需要登录才允许访问 |
| `anonymous` | `true` | 该接口不需要登录也可访问 |
| `permission` | — | 允许拥有该权限的用户访问 |
| `permission_visible` | — | 允许拥有该权限的用户在列表中可见 |
| `role` | — | 允许拥有该角色的用户访问 |
| `role_visible` | — | 允许拥有该角色的用户在列表中可见 |
| `wrap_request_parameter` | — | 将请求参数包装到一个变量中 |
| `default_data_source` | — | 配置默认数据源的 key |
| `disabled_unknown_parameter` | `true` | 不接收未经定义的参数 |
| `disabled_validate_request_body` | `false` | 禁止验证 RequestBody |

### 防重复提交

| 选项 key | 默认值 | 说明 |
|---------|--------|------|
| `avoid_repeat_submit_enable` | `true` | 是否开启防重复提交 |
| `avoid_repeat_submit_timeout` | `1000` | 防重复提交超时时间（秒） |
| `avoid_repeat_submit_key` | `#{magicapi:repeat:submit}` | 防重提交 Key（支持 SpEL） |
| `avoid_repeat_submit_key_subfix_spring_bean` | `springbean` | Key 后缀（Spring Bean 名称） |
| `avoid_repeat_submit_msg` | `重复提交，请稍后再试。` | 重复提交时的提示信息 |

### 热数据限流

| 选项 key | 默认值 | 说明 |
|---------|--------|------|
| `hot_data_enable` | `true` | 是否启用热数据限流 |
| `hot_data_key` | `#{magicapi:hot:data}` | 热数据限流的资源 key（支持 SpEL） |
| `hot_data_grade` | `QPS` | 限流模式：`QPS` 或 `THREAD` |
| `hot_data_count` | `0` | 热数据限流计数 |
| `hot_data_duration` | `1000` | 热数据统计时长（秒） |

### 熔断降级

| 选项 key | 默认值 | 说明 |
|---------|--------|------|
| `fallback_enable` | `true` | 是否启用熔断降级 |
| `fallback_count` | `0` | 熔断阈值（连续失败次数） |

---

## 项目插件体系

fw-plus 为 Magic-API 提供了 25+ 个插件扩展，每个插件遵循统一模式：

1. **POM 模块**：`fw-plus-magicapi-plugin-{name}` 引入第三方依赖
2. **自动配置**：`MagicAPI{Name}Configuration` 实现 bean 注册
3. **条件装配**：通过 `magic-api.plugin.{name}` 配置属性控制开关

| 类别 | 插件 |
|------|------|
| 数据库 | redis, mongo, elasticsearch, influx |
| 消息队列 | kafka, pulsar, rabbitmq, rocketmq |
| 物联网 | mqtt（Broker）, mqttclient（客户端） |
| 对象存储 | s3（基于 x-file-storage） |
| 定时任务 | task（Cron 表达式） |
| 实时通信 | sse（服务端推送）, ws（WebSocket） |
| 监控治理 | config（配置中心）, debezium（CDC）, dtp（动态线程池）, druid（连接池监控）, cluster（集群） |
| 认证 | satoken（Sa-Token 集成） |
| 模板渲染 | freemarker, java（动态类加载） |
| 文档 | swagger |
| 前端 UI | vue3 |
| 系统管理 | admin-server（Spring Boot Admin） |
| Cloud 层 | eureka（服务发现，自动覆盖 http 模块注入负载均衡） |
| AI 层 | langchain4j（LLM）, qdrant（向量数据库）, milvus（向量数据库）, weaviate（向量数据库）, redis-stack（向量存储） |

插件在 magic-script 中使用时，通过对应的内置模块变量名访问（如 `import redis`、`import s3`、`import mqtt`）。
