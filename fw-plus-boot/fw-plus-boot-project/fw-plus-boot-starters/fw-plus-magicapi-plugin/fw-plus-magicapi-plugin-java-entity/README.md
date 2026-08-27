# Magic-API Java Entity 插件

`fw-plus-magicapi-plugin-java-entity` 用于在 Magic-API 控制台资源树中维护 Java 实体源码。保存后，后端会把 `java-entity` Resource 动态编译并加载，主要供 Magic-Script 的 `entity` 模块、Spring Bean、Java 代码以及对象转换工具使用。

本模块只负责 Java 实体源码编辑、动态编译、热更新和实体实例创建，不包含 ORM、XML Mapper、CRUD、分页或 Join 的使用说明。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-magicapi-plugin-java-entity</artifactId>
    <version>${fw-plus.version}</version>
</dependency>
```

插件默认启用：

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

运行环境必须使用 JDK，不能只用 JRE，因为动态编译依赖 `javax.tools.JavaCompiler`。
编译超时后会中断候选任务；不响应中断的任务仍占用并发名额，退出前新的编译会返回 `compiler.busy`。

## 控制台使用

引入插件并启动应用后，Magic-API 控制台左侧资源树会新增“Java实体”。

使用方式：

1. 在“Java实体”资源树中右键分组，选择新建资源。
2. Resource 名称填写实体类简单名，例如 `User`。
3. 在底部“Java实体信息”面板填写路径和说明。
4. 在 Java 编辑器中编写完整源码。
5. 保存资源，保存成功后会触发动态编译。
6. 编译失败时保存会返回 Java 诊断信息；旧的成功实体代际继续生效。

持久化资源类型为 `java-entity`，文件后缀为 `.java`。每个资源只能包含一个顶级 `public` 类，并且 Resource 名称必须等于类简单名。

字段说明：

- `启用`：控制该实体是否参与动态编译和发布；关闭后资源仍保留，但不会注册到 `entity` 模块。
- `名称`：资源名，必须等于 Java 源码中的顶级 `public` 类简单名。
- `路径`：Magic-API Resource 路径，用于资源树分组和检索；不参与 Java 包名解析。
- `说明`：实体描述字段，可填写用途、字段约定、业务备注等，会随 Resource 一起保存。

## 实体源码示例

实体可以是普通 Java Bean，也可以使用 Lombok、Fastjson、Fastjson2、Jackson 等注解。

```java
package demo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class User {

    private Long id;

    @JSONField(name = "user_name")
    @com.alibaba.fastjson2.annotation.JSONField(name = "user_name")
    @JsonProperty("user_name")
    private String userName;

    @JSONField(serialize = false, deserialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(serialize = false, deserialize = false)
    @JsonIgnore
    private String password;
}
```

Lombok 会在每次代际编译时重新执行。`@Data` 生成的 getter/setter 和 `@Builder` 生成的内部类型都是真实字节码。

注意：`User.create()` 和 `User.create(map)` 都要求实体存在可访问的无参构造器；使用 Lombok `@Builder` 时，建议同时声明 `@NoArgsConstructor`。

## Magic-Script 使用

```javascript
import entity

var User = entity.User
var emptyUser = User.create()
var user = User.create({
    id: 1,
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

同名实体存在于不同包时，使用全限定类名：

```javascript
var User = entity['demo.User']
```

## DynamicEntityDescriptor 方法

`entity.User` 或 `entity['demo.User']` 返回的是动态实体描述器，常用方法如下：

- `name()`：返回实体全限定类名。
- `version()`：返回当前描述器绑定的动态实体代际版本。
- `javaClass()`：返回运行期真实 `Class<?>`。
- `create()`：不传参数，调用无参构造器创建空实例。
- `create(map)`：创建实例后按属性名写入 map 中的值，优先调用 setter，没有 setter 时回退到字段赋值。
- `create(null)`：按空 map 处理，效果等同于 `create()`；推荐直接写 `create()`。
- `resourceId()`：返回当前类型对应的 Magic-API Resource 标识。
- `sourceDigest()`：返回当前类型源码的 SHA-256 摘要。

示例：

```javascript
var User = entity['demo.User']

var a = User.create()
var b = User.create({
    id: 1,
    userName: '张三'
})
```

## 热更新机制

新增、修改、删除或启停任一 Java Entity Resource 后，会触发全部启用实体共同编译。

- 编译成功：发布新的动态实体代际；新访问的 `entity.User` 会拿到新描述器和新 `Class<?>`。
- 编译失败：不会替换当前成功代际；已经成功加载的旧实体继续可用。
- 冷启动首次编译失败：应用和已有 Magic-API 入口继续启动，`entity` 入口返回诊断信息。

已经取得的旧描述器不会被强制替换，可继续完成当前脚本调用。下一次重新从 `entity` 模块获取时，会拿到最新成功代际。

## Java 代码中使用

Java 代码里不能直接写动态实体的 `User.class`，因为 `User` 不是编译期固定类。需要从 `DynamicEntityRegistry` 解析描述器，再获取运行期 `Class<?>` 或创建实例。

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityDescriptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

DynamicEntityRegistry registry = SpringUtils.getBean(DynamicEntityRegistry.class);
DynamicEntityDescriptor User = registry.resolve("demo.User");

Class<?> userClass = User.javaClass();
Object user = User.create();
```

同名实体唯一时可以使用简单类名：

```java
DynamicEntityDescriptor User = registry.resolve("User");
```

同名实体存在于不同包时，使用全限定类名：

```java
DynamicEntityDescriptor User = registry.resolve("demo.User");
```

## Spring Bean 中使用

Spring Bean 插件中的动态 Bean 也可以通过 `entity` 模块或 `DynamicEntityRegistry` 使用 Java Entity。

Magic-Script 调用方式：

```javascript
import entity
import springBean

var User = entity['demo.User']
var user = User.create({
    id: 1,
    userName: '张三'
})

return springBean.userService.hello(user)
```

Java 代码调用方式：

```java
DynamicEntityRegistry registry = SpringUtils.getBean(DynamicEntityRegistry.class);
Object user = registry.resolve("demo.User").create(values);
```

## 使用 OrikaUtils 转换

动态实体没有编译期固定的 `User.class`，所以先获取运行期 `Class<?>`，再传给 `OrikaUtils`。

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.common.util.OrikaUtils;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

DynamicEntityRegistry registry = SpringUtils.getBean(DynamicEntityRegistry.class);
Class<?> userClass = registry.resolve("demo.User").javaClass();

Object user = OrikaUtils.convert(source, userClass);
```

如果需要先创建实体实例，再把 DTO 属性拷贝进去：

```java
Object user = registry.resolve("demo.User").create();
OrikaUtils.copy(dto, user);
return user;
```

字段名不一致时，使用 `fieldMapping`：

```java
Map<String, String> fieldMapping = new HashMap<>();
fieldMapping.put("name", "userName");

Object user = OrikaUtils.convert(dto, registry.resolve("demo.User").javaClass(), fieldMapping);
```

## 使用 Fastjson 转换

Fastjson 目标类型使用动态实体的 `Class<?>`。

```java
import com.alibaba.fastjson.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

DynamicEntityRegistry registry = SpringUtils.getBean(DynamicEntityRegistry.class);
Class<?> userClass = registry.resolve("demo.User").javaClass();

Object user = JSON.parseObject(json, userClass);
String jsonText = JSON.toJSONString(user);
```

Fastjson2 写法：

```java
Object user = com.alibaba.fastjson2.JSON.parseObject(json, registry.resolve("demo.User").javaClass());
String jsonText = com.alibaba.fastjson2.JSON.toJSONString(user);
```

如果动态实体字段上写了 Fastjson 注解，例如 `@JSONField(name = "user_name")`，序列化和反序列化会按注解生效。

## 使用 Jackson 转换

Jackson 目标类型同样使用运行期 `Class<?>`。

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

DynamicEntityRegistry registry = SpringUtils.getBean(DynamicEntityRegistry.class);
Class<?> userClass = registry.resolve("demo.User").javaClass();

ObjectMapper objectMapper = SpringUtils.getBean(ObjectMapper.class);
Object user = objectMapper.readValue(json, userClass);
String jsonText = objectMapper.writeValueAsString(user);
```

如果动态实体字段上写了 Jackson 注解，例如 `@JsonProperty("user_name")` 或 `@JsonIgnore`，序列化和反序列化会按注解生效。

## 能力边界

- 支持 Java 源码动态编译和热更新。
- 支持按资源启用/禁用；禁用资源不参与当前动态实体代际。
- 支持 Lombok 注解处理。
- 支持 Fastjson、Fastjson2、Jackson 注解编译和运行时保留。
- 支持 Java 17 运行环境；源码仍需遵守当前项目编译兼容约束。
- 不生成动态 Mapper。
- 不提供数据库 CRUD、分页、Wrapper、Join 或 XML Mapper 能力。
- 外部框架取得 `javaClass()` 后可能缓存该 `Class`；本插件不清理第三方缓存，也不承诺被第三方缓存引用的退休代际可回收。
- 不提供 JVM 内不可信代码沙箱；Java 源码拥有宿主 JVM 的代码执行权限，只允许受信任的 Magic-API 管理员编辑和启用。

## 相关插件说明

数据库相关使用说明已经拆到对应插件文档：

- `fw-plus-magicapi-plugin-mybatisplus/README.md`
- `fw-plus-magicapi-plugin-mybatisflex/README.md`
- `fw-plus-magicapi-plugin-spring-bean/README.md`

更多后端设计说明见根目录文档：`docs/magicapi-dynamic-java-entity.md`。
