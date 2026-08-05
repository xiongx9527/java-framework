# Magic-API 动态 Java 实体插件设计

## 背景与范围

首期交付一个独立的 Magic-API 插件，用 Resource 保存完整 Java 8 源码，并在运行时完成 Lombok 注解处理、编译、加载和热更新。实体主要在 Magic-Script 中使用，同时保留真实 `Class<?>`，供后续 MyBatis-Plus 和 MyBatis-Flex 能力接入，并支持项目现有 Fastjson 1.x、Fastjson2 和 Jackson 序列化注解。

首期明确不做 CRUD、分页、Join、MP-Join 或动态 Mapper，也不修改 MyBatis-Plus、MyBatis-Flex、MP-Join 和现有 `MybatisFlexModule`。

详细需求以 [Comet brief](../../comet/changes/magicapi-dynamic-entity-plugin/brief.md) 和 [能力规格](../../comet/changes/magicapi-dynamic-entity-plugin/specs/magicapi-dynamic-java-entity/spec.md) 为准。

## 方案比较

### 方案 A：全部实体按 ClassLoader 代际编译

每次资源变化时编译全部已启用源码，生成不可变实体注册表和新的 ClassLoader，完整成功后原子切换。

优点是支持跨实体引用、结构性修改和一致快照；失败回滚边界清晰。代价是保存单个实体也会重新编译全部实体，需要限制源码规模和并发编译。

### 方案 B：每个实体独立 ClassLoader

单次编译更快，但动态实体之间的类可见性和更新传播复杂。`User` 引用新版 `Dept` 时容易继续绑定旧 Class，难以保证 Join 和类型一致性。

### 方案 C：Instrumentation 重定义

可保留 Class 身份，但标准 HotSwap 无法可靠支持增加、删除字段和方法，还引入 Java Agent 部署要求，不符合实体结构频繁变化的场景。

采用方案 A。

## 模块与组件

插件模块命名为 `fw-plus-magicapi-plugin-java-entity`，包含以下边界清晰的组件：

- Resource 适配器：注册 `java-entity` 类型，读取完整源码并监听保存、删除和启动恢复事件。
- 源码分析器：从编译器语法树取得包名、公开类名和顶级类型数量，避免使用正则解析 Java。
- 编译 classpath 解析器：支持目录、普通 JAR 和 Spring Boot 可执行 JAR，向 JavaCompiler 提供实体所需依赖。
- 代际编译器：执行 Java 8 编译和 Lombok 注解处理，输出内存字节码及结构化诊断。
- Generation ClassLoader：只持有一个候选代际的字节码，父加载器使用应用 ClassLoader。
- 实体注册表：保存不可变的全限定类名、唯一简单名别名、Resource、版本和 Class 映射。
- Magic 动态模块：实现 `entity.User` 描述器及实体创建 API。

编译核心不依赖 ORM；MP、Flex 执行器以后作为独立适配层接入。

## 编译与切换流程

```text
Resource 新增、修改或删除
  -> 获取全部已启用源码快照
  -> 解析并校验公开类和名称冲突
  -> Java 8 + Lombok 编译候选代际
  -> 新 ClassLoader 加载全部公开实体
  -> 构建不可变注册表
  -> 原子替换当前代际引用
  -> 释放插件持有的旧代际引用
```

任何候选步骤失败都不会切换当前引用。并发保存通过单写编译队列串行化；如果编译期间又发生变化，完成当前候选后立即以最新 Resource 快照再编译，不能让较旧候选覆盖较新源码。

## Lombok

Lombok 是运行时动态编译能力的一部分，因此插件必须把 Lombok 放入编译 classpath 和 processor path，并保持默认注解处理开启。测试以生成字节码中的构造器、getter、setter 和 builder 为准，不能只验证源码成功返回。

开发目录可直接使用依赖路径。Spring Boot 可执行 JAR 中的嵌套依赖需要由 classpath 解析器物化到受控临时缓存，编译完成后按插件生命周期清理。找不到 JavaCompiler 或 Lombok 处理器时，应用仍启动，但动态实体模块返回明确诊断。

## Magic-Script API

```javascript
import entity

var User = entity.User
var user = User.create({
    userName: '张三',
    deptId: 10
})

return {
    name: User.name(),
    version: User.version(),
    type: User.javaClass(),
    data: user
}
```

描述器不跨代际自动漂移：已经取得的 `User` 描述器绑定取得时的 Class；再次访问 `entity.User` 才解析当前代际。这保证单次脚本执行期间类型一致。

`create(map)` 使用无参构造器创建实例，优先调用 setter，再回退到非 static、非 final 的实例字段。未知属性、不可转换值或缺少无参构造器时返回带实体名和属性名的错误。

## JSON 注解

插件编译依赖覆盖项目同时使用的 Fastjson 1.x、Fastjson2 和 Jackson 注解。它们是运行时注解，不参与 Lombok processor path。动态实体 Class 保留注解及全部属性，由实际序列化器解释。

测试分别使用三套项目当前序列化器验证字段重命名和忽略行为。Jackson databind 的 `JsonSerialize`、`JsonDeserialize` 可参与编译，但测试不执行用户自定义序列化器，避免把任意业务代码执行混入编译校验。

## ORM 兼容策略

普通 Bean 与带 MP/Flex 注解的 Bean 使用同一编译路径。首期只保证注解可被动态编译并保留在真实 Class 上，不主动初始化两套 ORM 的元数据。

不预热元数据是有意设计：MP 3.3.2 和 Flex 1.11.4 都存在按 Class 保存的静态元数据缓存，过早注册会延长已退休 ClassLoader 生命周期。后续 CRUD/Join 适配需要单独定义缓存清理、过期实例和事务语义。

## 错误与安全

编译诊断按 Resource 分组，保留错误级别、行号、列号和摘要，并限制总长度。冷启动失败不阻止 Spring 上下文启动；没有成功代际时，`entity` 模块返回不可用原因。

动态 Java 源码等同宿主 JVM 任意代码执行权限，只允许受信任管理员编辑。插件不承诺沙箱隔离，不在诊断中输出凭据、环境变量或无关宿主路径。

## 测试策略

- 编译器单元测试：普通 Bean、Lombok、MP/Flex 注解、Fastjson/Jackson 注解、跨源码引用和结构化诊断。
- 序列化兼容测试：Fastjson 1.x、Fastjson2、Jackson 字段重命名和忽略行为。
- 注册表单元测试：别名冲突、原子切换、删除和并发读取。
- 热更新测试：新增/删除字段和方法、Lombok 变化、失败回滚、旧描述器一致性。
- Magic 集成测试：`entity.User`、`create()`、`create(map)`、`javaClass()` 和版本变化。
- Spring Boot 集成测试：插件启用、禁用、冷启动错误、目录 classpath 和可执行 JAR classpath。
- Maven 验证：插件模块、插件聚合、Boot BOM、自动配置和 Java 8 编译。
