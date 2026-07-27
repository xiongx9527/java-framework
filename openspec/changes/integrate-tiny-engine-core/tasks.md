## 1. 模块与依赖准备

- [x] 1.1 确认 `fw-plus-core/fw-plus-project` 现有模块命名、父 POM、聚合 POM、BOM 和 Java 8 编译配置。
- [x] 1.2 新增 TinyEngine core Maven 模块，并接入 fw-plus-core 聚合 POM、父 POM和 BOM，版本遵循 `${revision}` / `0.1.1-SNAPSHOT`。
- [x] 1.3 收敛 TinyEngine core 依赖到 Spring Boot 2.7.18 与 fw-plus-core 兼容版本，排除 Spring Boot 3、Java 17、Jakarta EE 9+、LangChain4j 1.x、AI/RAG 专属依赖。
- [x] 1.4 确认没有修改 Magic-API 2.2.2 相关依赖版本。

## 2. 基础类型与工具迁移

- [x] 2.1 从 `tiny-engine-backend-java/base` 迁移公共基础类型，包括 Result、BaseEntity、HistoryEntity、PageQueryVo、异常枚举、ServiceException 和必要上下文抽象。
- [x] 2.2 迁移 TinyEngine 所需 TypeHandler、JSON/Schema/Hash/文件安全等工具类，并改造为 Java 8 可编译写法。
- [x] 2.3 将 `jakarta.*`、Spring Boot 3-only API 和 Java 17 写法替换为 Spring Boot 2.7 / Java 8 兼容实现。
- [x] 2.4 为迁移后的公共基础类型补充最小单元测试或编译验证覆盖。

## 3. 领域模型、DTO 与持久化资源

- [x] 3.1 迁移 TinyEngine 主要实体和 DTO，覆盖 app、page、page history、page template、canvas、block、block group、material、material history、component、component library、resource、resource group、business category、i18n、platform、tenant、user、extension、task record、model、datasource 和关联关系。
- [x] 3.2 迁移对应 Mapper 接口，并按 fw-plus-core 包名和 MyBatis/MyBatis-Plus 版本调整 import、泛型和注解。
- [x] 3.3 迁移对应 Mapper XML 到 TinyEngine core 模块资源目录，确保 namespace、resultMap、typeHandler 和 SQL 片段与迁移后包名一致。
- [x] 3.4 迁移 MySQL、H2、PostgreSQL 初始化 SQL 资源到模块 classpath 下的稳定位置，并记录资源路径，不在 core 中自动执行。
- [x] 3.5 建立实体、Mapper、XML、SQL 资源迁移核对清单，避免主领域对象遗漏。

## 4. 核心服务迁移

- [ ] 4.1 迁移 app/page/material/resource/i18n/platform/tenant/user/task/model/datasource 等核心服务接口。
- [ ] 4.2 迁移对应服务实现，并剥离 Controller、OpenAPI、启动类、过滤器、定时任务和 profile 配置依赖。
- [ ] 4.3 保留必要的扩展点，例如保存后 hook 或上下文读取接口，并避免引入上层 starter 自动配置职责。
- [ ] 4.4 为核心服务编写可编译的单元测试或轻量集成测试，验证服务契约可被上层模块引用。

## 5. 动态模型与 SQL 安全边界

- [ ] 5.1 迁移动态模型 DTO、DAO、动态 CRUD 服务和动态表生命周期服务。
- [ ] 5.2 新增集中式 SQL 标识符校验工具，覆盖表名、模型英文名、字段名、条件键、排序字段、排序方向、DDL 列名和批量写入字段。
- [ ] 5.3 改造动态查询、插入、更新、删除、建表、改表、删表、初始化和批量插入路径，确保所有值使用参数绑定，所有 SQL 标识符先校验后拼接。
- [ ] 5.4 增加危险输入测试，覆盖分号、注释、空白注入、函数调用、表达式、关键字拼接和非法排序方向，并确认执行 SQL 前被拒绝。
- [ ] 5.5 增加合法动态模型操作测试，验证动态查询、分页、写入、表结构读取和表生命周期操作仍可工作。

## 6. 范围排除与验证

- [ ] 6.1 检查 TinyEngine core 模块不包含 AI Chat、RAG、VectorStorage、LangChain4j 1.x、Controller、OpenAPI、启动应用、Docker 和 Web 过滤器。
- [ ] 6.2 运行 TinyEngine core 模块 Maven 编译，确认 Java 8 source/target 下通过。
- [ ] 6.3 运行新增或受影响测试，确认主要领域映射、服务契约、SQL 资源引用和动态 SQL 安全边界通过验证。
- [ ] 6.4 运行必要的 fw-plus-core 聚合层验证，确认新增模块注册没有破坏现有 core 构建。
- [ ] 6.5 更新实现说明或模块 README，记录 TinyEngine core artifact、资源路径、范围排除项和后续 starter 接入边界。
