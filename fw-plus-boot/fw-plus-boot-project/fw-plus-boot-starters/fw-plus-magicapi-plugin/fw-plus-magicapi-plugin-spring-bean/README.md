# Magic-API Spring Bean 插件

`fw-plus-magicapi-plugin-spring-bean` 用于在 Magic-API 控制台资源树中维护 Java Spring Bean 源码。保存后后端会把 `spring-bean` Resource 动态编译，并按 Spring `BeanDefinition` 注册到当前应用上下文。

动态类默认就是 Spring Bean，不需要写 `@Component`、`@Service`。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-magicapi-plugin-spring-bean</artifactId>
    <version>${fw-plus.version}</version>
</dependency>
```

插件默认启用：

```yaml
magic-api:
  plugin:
    spring-bean:
      enabled: true
```

运行环境必须使用 JDK，不能只用 JRE，因为动态编译依赖 `javax.tools.JavaCompiler`。

## 控制台使用

引入插件并启动应用后，Magic-API 控制台左侧资源树会新增“Spring Bean”。

使用方式：

1. 在“Spring Bean”资源树中右键分组，选择新建资源。
2. Resource 名称填写 Bean 类简单名，例如 `UserService`。
3. 在底部“Spring Bean 信息”面板填写路径和说明。
4. 在 Java 编辑器中编写完整源码。
5. 保存资源，保存成功后会触发动态编译并注册 Spring Bean。
6. 编译或注册失败时保存会返回 Java 诊断信息；旧的成功 Bean 继续生效。

持久化资源类型为 `spring-bean`，文件后缀为 `.java`。每个资源只能包含一个顶级 `public` 类，并且 Resource 名称必须等于类简单名。

字段说明：

- `启用`：控制该 Bean 是否参与动态编译和注册；关闭后资源仍保留，但不会注册到 Spring 容器。
- `名称`：资源名，必须等于 Java 源码中的顶级 `public` 类简单名。
- `路径`：Magic-API Resource 路径，用于资源树分组和检索；不参与 Java 包名解析。
- `说明`：Bean 描述字段，可填写用途、依赖、事务边界等，会随 Resource 一起保存。

## Bean 示例

```java
package demo;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class UserService {

    @DS("slave")
    @Transactional(rollbackFor = Exception.class)
    public String saveUser() {
        log.info("save user");
        return "ok";
    }
}
```

默认 Bean 名称按 Spring 规则生成；没有 `@Component`、`@Service` 时，使用类简单名首字母小写，例如 `UserService` 注册为 `userService`。

如果源码里写了 Spring stereotype 注解并指定名称，也会按 Spring 的命名规则处理。

动态 Spring Bean 支持 Lombok 注解，例如 `@Data`、`@NoArgsConstructor`、`@RequiredArgsConstructor`、`@Slf4j` 等。运行环境需要引入本插件提供的 Lombok 依赖，并使用 JDK 动态编译环境。

动态 Spring Bean 也支持 `@DS` 注解。`@DS` 属于 Spring AOP 注解，生效前提是应用已经启用动态数据源及其 AOP Advisor，例如项目中的动态数据源自动配置正常加载。调用时需要通过 Spring Bean 代理进入方法，类内部自调用不会触发 `@DS` 切换。

## Magic-Script 使用

```javascript
import springBean

return springBean.userService.saveUser()
```

`springBean.xxx` 会按 `xxx` 从 Spring 容器取 Bean。

## 在 Spring Bean 中使用 Java Entity

如果同时引入了 `fw-plus-magicapi-plugin-java-entity`，动态 Spring Bean 可以通过 `DynamicEntityRegistry` 使用动态实体。

推荐在动态 Bean 源码中按类型从 Spring 容器获取 `DynamicEntityRegistry`，再通过实体名解析：

```java
package demo;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityDescriptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

import java.util.HashMap;
import java.util.Map;

public class UserService {

    public Object buildUser() {
        DynamicEntityRegistry registry = SpringUtils.getBean(DynamicEntityRegistry.class);
        DynamicEntityDescriptor User = registry.resolve("User");

        Map<String, Object> values = new HashMap<>();
        values.put("userName", "张三");
        values.put("password", "secret");
        return User.create(values);
    }
}
```

同名实体存在于不同包时，使用全限定类名：

```java
DynamicEntityDescriptor User = registry.resolve("demo.User");
```

也可以获取实体 `Class`：

```java
Class<?> userClass = registry.resolve("demo.User").javaClass();
```

注意：

- Spring Bean 源码中不能直接写动态实体的 `User.class`，因为 `User` 不是编译期固定类。
- 推荐通过 `DynamicEntityRegistry.resolve("User")` 或 `resolve("demo.User")` 获取。
- `resolve("User")` 只适合同名实体唯一的情况；存在同名时用全限定类名。

## 事务说明

- 支持 Spring `@Transactional`。
- 事务基于 Spring AOP 生效，因此仍遵循 Spring 事务规则：方法需要通过 Spring Bean 代理调用，自调用不会触发事务。
- 动态 Bean 通过 `BeanDefinition` 注册，不是简单 `registerSingleton`，所以可以走 Spring BeanPostProcessor、AOP 和事务代理流程。

## 能力边界

- 支持 Java 源码动态编译和热更新。
- 支持按资源启用/禁用；禁用资源不参与当前 Spring Bean 注册。
- 支持 Lombok 注解处理。
- 支持动态注册为 Spring Bean。
- 支持 `@Transactional` 事务注解。
- 支持 `@DS` 动态数据源注解，前提是运行环境已启用动态数据源 AOP。
- 不要求源码写 `@Component`、`@Service`。
- 不生成 Mapper，不改造 MyBatis-Plus、MyBatis-Flex、MP-Join。
