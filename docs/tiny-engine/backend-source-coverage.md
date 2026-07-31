# TinyEngine 后端源覆盖矩阵

## 来源

- 路径：`/Users/xiongxiao/Workspace/Idea/codes/tiny-engine-backend-java`
- 分支：`release/v2.10.x`
- 提交：`c116fa649122390baf212f53d82358617c63924d`
- 状态：`git status --porcelain --untracked-files=no` 为空

## 覆盖原则

`tiny-engine-backend-java` 是后端能力的完整生产参考源，不能只迁移 Controller，也不能只按 TinyEngine 前端 `mockServer` 补假数据。每一类来源都必须进入以下三种状态之一：

- `migrated`：已迁入 fw-plus core/boot/starter/sample。
- `adapted`：不按原样复制，但已映射为 fw-plus 契约、配置、文档、sample 或可选 Provider。
- `deferred-with-reason`：本次不实现，已写明原因、用户可见行为和后续适配点。

## 覆盖矩阵

| 来源类别 | 来源路径 | 目标位置 | 状态 | 说明 |
|---|---|---|---|---|
| common | `base/src/main/java/com/tinyengine/it/common/**` | `fw-plus-tiny-engine-core/common` 与 Boot 错误处理 | migrated/adapted | 覆盖 `Result`、`BaseEntity`、`HistoryEntity`、`PageQueryVo`、异常、类型处理器、日志注解、Hash、JSON、Schema、文件安全、图片工具；Spring Advice/Aspect 进入 Boot 或显式适配。 |
| config | `base/src/main/java/com/tinyengine/it/config/**` | Boot 可选配置与外部能力 Provider | adapted/deferred-with-reason | `AiChatConfig`、`OpenAIConfig` 不强制 starter 依赖具体 OpenAI/RAG SDK，默认返回未配置。 |
| controller | `base/src/main/java/com/tinyengine/it/controller/**` | Boot MVC/WebFlux Controller 与共享 facade | adapted | 覆盖 app-center、material-center、platform-center、v1 schema、AI/vector、health、history、task、resource、i18n、template、canvas、tenant/user 路由。 |
| dynamic | `base/src/main/java/com/tinyengine/it/dynamic/**` | `fw-plus-tiny-engine-core/dynamic` 与 MVC/WebFlux API | migrated/adapted | 覆盖 `ModelDataController`、`DynamicSqlProvider`、`ModelDataDao`、动态 CRUD DTO 和 `DynamicModelService`/`DynamicService`。 |
| event | `base/src/main/java/com/tinyengine/it/event/**` | Core/Boot 事件或保存 Hook | adapted | `PageDeleteEvent` 映射为 Core 事件或删除流程 Hook。 |
| gateway/ai | `base/src/main/java/com/tinyengine/it/gateway/ai/**` | AI/RAG 可选 Provider 契约 | adapted/deferred-with-reason | `AiChatClient` 不在 starter 中固化具体远程模型实现。 |
| login | `base/src/main/java/com/tinyengine/it/login/**` | Core 用户体系 SPI 与 Boot 可选适配 | adapted | 登录、SSO、密码策略、JWT、SM2、SM3、黑名单服务抽象为 Provider；Core 不直接依赖 Servlet、JWT 或第三方 SSO。 |
| mapper | `base/src/main/java/com/tinyengine/it/mapper/**` | `fw-plus-tiny-engine-core/mapper` | migrated | Java Mapper 集合必须与 Mapper XML 集合一致。 |
| mapper-xml | `base/src/main/resources/mappers/*.xml` | `fw-plus-tiny-engine-core/src/main/resources/mapper/tinyengine/*.xml` | migrated | 28 个 XML 文件见下方清单。 |
| model | `base/src/main/java/com/tinyengine/it/model/**` | `fw-plus-tiny-engine-core/model` | migrated | 全部 entity 和 dto 字段、JSON 结构、类型处理器需要契约覆盖。 |
| rag | `base/src/main/java/com/tinyengine/it/rag/**` | Core 可选 RAG/Vector Provider 契约与 Boot fallback | adapted/deferred-with-reason | RAG 配置、向量文档、搜索请求、批量结果、存储服务都进入接口或未配置响应。 |
| service | `base/src/main/java/com/tinyengine/it/service/**` | `fw-plus-tiny-engine-core/service` 与 Boot facade | migrated/adapted | app/material/platform/extend 均需消项；Controller 不直接复制业务逻辑。 |
| app-runtime | `app/src/main/java/**`, `app/src/main/resources/application*.yml`, `logback.xml`, `checkstyle.xml`, `webapp/**` | sample、Boot 可覆盖 Bean、docs | adapted | 独立启动类、OpenAPI、过滤器、RequestId、WebConfig、定时清理和传统 webapp 不作为通用 starter 强绑定。 |
| sql | `app/src/main/resources/sql/**`, `docker-deploy-data/mysql/init/**` | Core SQL manifest 与 sample docs | migrated/adapted | H2、MySQL、PostgreSQL SQL 文件见下方清单；Docker init 作为部署参考。 |
| deploy | `Dockerfile`, `docker-compose.yml`, `example-docker.yml`, `docker-deploy-data/**` | `docs/tiny-engine/starter.md` 与 sample 部署说明 | adapted | 写明 fw-plus starter 用法、数据库初始化和反向代理路径，而非复制原独立部署拓扑。 |
| documents | `documents/agent/**`, `documents/tinyengine/**` | TinyEngine 文档或后续组件资料 | adapted/deferred-with-reason | Harmony 和 TinyEngine 组件说明不能从清单中消失；默认不打入 starter 运行时。 |
| model-assets | `all-MiniLM-L6-v2/**` | 可选 RAG/Embedding Provider 文档 | deferred-with-reason | ONNX 模型和 tokenizer 体积大且属于可选 AI 能力，starter 默认不内置；无 Provider 时 AI/RAG 接口返回未配置。 |
| settings | `settings/**`, `.codecheck/**`, `.github/**` | docs 或不迁移说明 | adapted/deferred-with-reason | 代码规范、CI 和仓库模板只作为参考，不进入 fw-plus 发布产物。 |

## Java 包路径清单

- `common`
- `common/base`
- `common/context`
- `common/converter`
- `common/enums`
- `common/exception`
- `common/handler`
- `common/log`
- `common/utils`
- `config`
- `controller`
- `controller/v1`
- `dynamic`
- `dynamic/controller`
- `dynamic/dao`
- `dynamic/dto`
- `dynamic/service`
- `event`
- `gateway`
- `gateway/ai`
- `login`
- `login/config`
- `login/controller`
- `login/model`
- `login/service`
- `login/utils`
- `mapper`
- `model`
- `model/dto`
- `model/entity`
- `rag`
- `rag/config`
- `rag/entity`
- `rag/service`
- `service`
- `service/app`
- `service/extend`
- `service/material`
- `service/platform`

## Controller 覆盖清单

- `AppController`, `AppV1Controller`, `AppExtensionController`, `AppTemplateController`
- `PageController`, `PageHistoryController`, `PageTemplateController`, `CanvasController`
- `BlockController`, `BlockGroupController`, `BusinessCategoryController`, `ComponentController`, `ComponentLibraryController`, `MaterialController`
- `ModelController`, `DataSourceController`, `ResourceController`, `ResourceGroupController`, `I18nEntryController`
- `PlatformController`, `PlatformHistoryController`, `TenantController`, `UserController`
- `TaskRecordController`, `TaskRecordMaterialController`, `HomeController`
- `AiChatController`, `VectorStorageController`
- `dynamic/controller/ModelDataController`
- `login/controller/LoginController`

## Mapper XML 清单

- `AppExtensionMapper.xml`
- `AppMapper.xml`
- `AuthUsersUnitsRolesMapper.xml`
- `BlockCarriersRelationMapper.xml`
- `BlockGroupBlockMapper.xml`
- `BlockGroupMapper.xml`
- `BlockHistoryMapper.xml`
- `BlockMapper.xml`
- `BusinessCategoryMapper.xml`
- `ComponentLibraryMapper.xml`
- `ComponentMapper.xml`
- `DatasourceMapper.xml`
- `I18nEntryMapper.xml`
- `I18nLangMapper.xml`
- `MaterialHistoryMapper.xml`
- `MaterialMapper.xml`
- `ModelMapper.xml`
- `PageHistoryMapper.xml`
- `PageMapper.xml`
- `PageTemplateMapper.xml`
- `PlatformHistoryMapper.xml`
- `PlatformMapper.xml`
- `ResourceGroupMapper.xml`
- `ResourceGroupResourceMapper.xml`
- `ResourceMapper.xml`
- `TaskRecordMapper.xml`
- `TenantMapper.xml`
- `UserMapper.xml`

## SQL 清单

- `h2/01_create_all_tables_ddl_v1.0.0.h2.sql`
- `h2/02_create_model_ddl_2025_0717.sql`
- `h2/03_create_resources_ddl_2025_0902.sql`
- `h2/04_create_permission_table_ddl_2025_1029.sql`
- `h2/05_update_all_tables_ddl.sql`
- `h2/06_update_tables_ddl_v1.0.0_2025_0527.sql`
- `h2/07_update_table_ddl_2025_1014.sql`
- `h2/08_update_table_ddl_2025_1217.sql`
- `h2/09_update_table_ddl_2025_1229.sql`
- `h2/init_data_2025_1023.sql`
- `h2/init_data_2025_1125.sql`
- `h2/init_data_for_test_v1.0.0.sql`
- `mysql/01_create_all_tables_ddl_v1.0.0.mysql.sql`
- `mysql/02_create_model_ddl_2025_0717.sql`
- `mysql/03_create_resources_ddl_2025_0902.sql`
- `mysql/04_create_permission_table_ddl_2025_1029.sql`
- `mysql/05_update_all_tables_ddl.sql`
- `mysql/06_update_tables_ddl_v1.0.0_2025_0527.sql`
- `mysql/07_update_table_ddl_2025_1014.sql`
- `mysql/08_update_table_ddl_2025_1217.sql`
- `mysql/09_update_table_ddl_2025_1229.sql`
- `mysql/init_data_2025_1023.sql`
- `mysql/init_data_2025_1125.sql`
- `mysql/init_data_for_test_v1.0.0.sql`
- `postgresql/create_all_tables_ddl_v1.postgresql.sql`

## app/runtime 清单

- `app/src/main/java/com/tinyengine/it/TinyEngineApplication.java`
- `app/src/main/java/com/tinyengine/it/config/OpenApiConfig.java`
- `app/src/main/java/com/tinyengine/it/config/filter/FilterConfig.java`
- `app/src/main/java/com/tinyengine/it/config/filter/RequestIdFilter.java`
- `app/src/main/java/com/tinyengine/it/config/filter/WebConfig.java`
- `app/src/main/java/com/tinyengine/it/task/CleanupProperties.java`
- `app/src/main/java/com/tinyengine/it/task/DatabaseCleanupService.java`
- `app/src/main/java/com/tinyengine/it/task/SchedulerConfig.java`
- `app/src/main/resources/application.yml`
- `app/src/main/resources/application-alpha.yml`
- `app/src/main/resources/application-dev.yml`
- `app/src/main/resources/application-local.yml`
- `app/src/main/resources/logback.xml`
- `app/src/main/resources/checkstyle.xml`
- `app/src/main/webapp/WEB-INF/web.xml`
- `app/src/main/webapp/index.jsp`

## deploy/doc/model/settings 清单

- `Dockerfile`
- `docker-compose.yml`
- `example-docker.yml`
- `docker-deploy-data/nginx.conf`
- `docker-deploy-data/mysql/conf/**`
- `docker-deploy-data/mysql/init/**`
- `documents/agent/**`
- `documents/tinyengine/**`
- `all-MiniLM-L6-v2/model.onnx`
- `all-MiniLM-L6-v2/tokenizer.json`
- `settings/codestyle.xml`
- `settings.xml`
- `.codecheck/check.yml`
- `.github/**`
