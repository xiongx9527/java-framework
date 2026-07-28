# fw-plus-tiny-engine-core

`fw-plus-tiny-engine-core` 提供 TinyEngine 的领域模型、MyBatis-Plus 持久化接口、服务实现、动态模型访问和 SQL 资源。该模块是无 Web 运行时假设的底层库，供后续集成层按需装配。

## 坐标与包结构

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-tiny-engine-core</artifactId>
    <version>${revision}</version>
</dependency>
```

Java 包根目录为 `com.xiongx9527.fw.plus.tinyengine`。领域模型位于 `model`，Mapper 位于 `mapper`，服务位于 `service`，动态模型能力位于 `dynamic`，可由集成层实现的扩展点位于 `support`。

## 持久化与 SQL 资源

- Mapper XML 由集成层通过 `classpath*:mapper/tinyengine/**/*.xml` 加载。
- 初始化与迁移 SQL 的资源根目录为 `META-INF/tiny-engine/sql/`。
- `META-INF/tiny-engine/sql/manifest.properties` 是 SQL 资源清单。每个数据库使用 `<database>.<三位序号>` 作为键，`TinyEngineSqlScriptManifest` 按键的字典序返回脚本。因此新增脚本必须使用保持执行顺序的三位递增序号，且清单值必须指向模块内的 `classpath:` 资源。
- 模块只提供 SQL 清单读取能力，不执行建表、迁移或初始化数据；调用方负责选择数据库、执行时机、事务与幂等策略。

## 扩展点

集成层应显式提供以下 SPI 实现，并将其注入具体服务：

- `TinyEngineUserContext`：通过 `getCurrentUser()` 提供当前操作者。无认证上下文时可使用 `SystemTinyEngineUserContext` 的系统用户实现。
- `TinyEngineSaveHook`：在页面保存后接收 `afterSave(Page page)` 回调，用于发布领域事件、刷新缓存或衔接外部流程；默认可使用 `NoopTinyEngineSaveHook`。

## 动态 SQL 安全约束

动态模型的表名、字段名、排序字段和排序方向都必须先经过 `TinyEngineSqlIdentifierValidator` 校验。标识符仅允许字母、数字和下划线，且首字符必须为字母或下划线；限定名必须逐段校验，排序方向只能是受控枚举值。动态值必须继续使用 MyBatis 参数绑定，不能把未校验的外部输入拼接为 SQL 标识符或片段。

## 范围边界

本模块不包含 Web 接口、自动配置、启动入口、starter、Servlet 过滤器、API 文档、容器部署、环境 profile 或 AI/RAG 集成。未来的 Boot starter 负责 Mapper 扫描、数据源、事务、认证上下文和扩展点装配；它应依赖本模块，而不应将这些运行时职责回填到 core。
