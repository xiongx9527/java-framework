# fw-plus TinyEngine Spring Boot Starter

`fw-plus-tiny-engine-spring-boot-starter` 提供 TinyEngine 后端能力，并内置 TinyEngine 前端工程源码作为可选构建输入。

这个 starter 的职责边界是：

- 提供 `fw-plus-tiny-engine-core` 的应用、页面、物料、模型、用户等后端接口能力。
- 提供 Spring MVC 与 Spring WebFlux 两套 Controller 自动配置入口。
- 提供中立用户体系 SPI，由宿主应用或 Magic-API、Sa-Token、Spring Security、IAM 等外部工具实现登录态与权限。
- 在 `META-INF/tiny-engine/sql/` 中统一提供 MySQL、H2 与 PostgreSQL 的初始化、迁移脚本及 `manifest.properties` 清单。
- 在源码目录 `src/console` 中按 Magic-API 插件的模式携带 TinyEngine 前端工程源码。
- 在 Maven `prepare-package` 阶段执行 `pnpm install` 与 `pnpm run build:prod`，并把 `designer-demo/dist` 复制到 jar 的 `META-INF/resources/tiny-engine/`，对应运行入口 `/tiny-engine/`。

内置前端源码保留 `designer-demo/`、`packages/`、`scripts/`、`patches/` 以及根目录必要构建配置。以下内容不会放入 starter：

- `mockServer/`：本地开发 mock 后端。
- `docs/`：上游文档体积较大，不属于运行或构建 TinyEngine 前端的必要内容。
- `node_modules/`、`dist/`、`coverage/`、`.env.local`、`.DS_Store` 等本机、构建或生成产物。

宿主应用必须按运行模式显式添加以下依赖之一：

- Spring MVC：`org.springframework.boot:spring-boot-starter-web`
- Spring WebFlux：`org.springframework.boot:spring-boot-starter-webflux`

Magic-API 的 TinyEngine 插件只负责在 Magic 控制台 `statusBarItems` 增加入口。默认会跳转到宿主同源的 `/tiny-engine/`；业务也可以通过插件配置覆盖为独立域名或网关地址。

Maven `package` 需要 Node 与 pnpm；`compile` 不触发前端构建。构建不会读取 `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine` 或 `/Users/xiongxiao/Downloads/TinyEngine-develop` 等外部前端源码路径；这些源码已经作为 starter 内部资源保存。

## 快速接入

业务后端接入 TinyEngine 至少需要：

- 引入 `fw-plus-tiny-engine-spring-boot-starter`。
- 选择 Spring MVC 或 Spring WebFlux，并显式引入对应 Web starter。
- 配置数据源，推荐生产环境使用 MySQL。
- 开启 `fw.plus.tiny-engine.enabled=true`。
- 提供真实登录态与权限实现，不能依赖免登。
- 如启用 AI，需要额外引入 AI Boot Starter、模型/向量中立 starter、Core Adapter 和 provider SDK，并通过环境变量注入密钥。

Maven 依赖示例：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.boot</groupId>
	<artifactId>fw-plus-tiny-engine-spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
	<groupId>mysql</groupId>
	<artifactId>mysql-connector-java</artifactId>
	<scope>runtime</scope>
</dependency>
```

启用 AI 时增加：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.ai</groupId>
	<artifactId>fw-plus-ai-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>com.xiongx9527.fw.plus.ai</groupId>
	<artifactId>fw-plus-ai-model-spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>com.xiongx9527.fw.plus.ai</groupId>
	<artifactId>fw-plus-ai-store-embedding-spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-ai-langchain4j-adapter</artifactId>
</dependency>
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-ai-langchain4j-openai</artifactId>
</dependency>
```

## 后端配置

MySQL + 登录安全 + AI 的 `application.yml` 示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/tiny_engine?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${TINY_ENGINE_DB_USERNAME}
    password: ${TINY_ENGINE_DB_PASSWORD}

fw:
  plus:
    tiny-engine:
      enabled: true
      web:
        enabled: true
        base-path: ""
      mapper:
        enabled: true
      resources:
        enabled: true
      service:
        enabled: true
      security:
        enabled: true
      sql:
        enabled: true
        database: mysql
        auto-initialize: false
        manifest-location: classpath:META-INF/tiny-engine/sql/manifest.properties
      ai:
        enabled: true
        provider: langchain4j
        missing-dependency-policy: degrade
        model:
          chat-key: tinyengine-openai-chat
          streaming-chat-key: tinyengine-openai-streaming-chat
        chat:
          enabled: true
        rag:
          enabled: false
        web:
          enabled: true
          base-path: /tiny-engine/ai

fw-plus:
  ai:
    model:
      enabled: true
      default-model: tinyengine-openai-chat
      models:
        tinyengine-openai-chat:
          key: tinyengine-openai-chat
          framework: langchain4j
          provider: openai
          type: CHAT
          properties:
            baseUrl: ${OPENAI_BASE_URL:https://api.openai.com/v1}
            apiKey: ${OPENAI_API_KEY}
            modelName: ${OPENAI_MODEL:gpt-5.5}
            timeout: PT2M
        tinyengine-openai-streaming-chat:
          key: tinyengine-openai-streaming-chat
          framework: langchain4j
          provider: openai
          type: STREAMING_CHAT
          properties:
            baseUrl: ${OPENAI_BASE_URL:https://api.openai.com/v1}
            apiKey: ${OPENAI_API_KEY}
            modelName: ${OPENAI_MODEL:gpt-5.5}
            timeout: PT2M
```

配置说明：

- `fw.plus.tiny-engine.enabled`：总开关，默认关闭。
- `fw.plus.tiny-engine.web.enabled`：启用 MVC Controller；WebFlux 环境下由 WebFlux 自动配置接管。
- `fw.plus.tiny-engine.web.base-path`：接口基础路径前缀，默认空字符串；前端默认访问同源 `/app-center/api/**`、`/platform-center/api/**`、`/material-center/api/**`。
- `fw.plus.tiny-engine.security.enabled`：启用安全拦截。生产环境必须开启，未登录接口返回 401，前端应退回登录页。
- `fw.plus.tiny-engine.sql.auto-initialize`：是否按 manifest 自动初始化 SQL。开发环境可开启；生产环境建议由 Flyway/Liquibase/DBA 执行脚本后关闭。
- `fw.plus.tiny-engine.ai.*`：TinyEngine 官方 AI 接口适配层配置。
- `fw-plus.ai.model.*`：fw-plus AI 模型注册表配置，`chat-key`、`streaming-chat-key` 必须能匹配这里注册的模型 key。

不要把真实 `OPENAI_API_KEY`、数据库密码写进仓库配置文件；使用环境变量、配置中心或密钥管理系统注入。

## 数据库初始化

starter 在 `META-INF/tiny-engine/sql/` 下提供 MySQL、H2、PostgreSQL 脚本，并通过 `META-INF/tiny-engine/sql/manifest.properties` 管理初始化顺序。

推荐生产初始化方式：

1. 创建业务库，例如 `tiny_engine`。
2. 按 manifest 中 MySQL 脚本顺序执行 DDL/DML。
3. 确认基础租户、角色、管理员关系数据存在。
4. 设置 `fw.plus.tiny-engine.sql.auto-initialize=false`，避免运行期重复改库。

开发或本地 sample 可以临时开启：

```yaml
fw:
  plus:
    tiny-engine:
      sql:
        database: mysql
        auto-initialize: true
```

基础数据要求：

- `t_tenant` 至少需要一个默认租户，当前 starter/sample 约定 `id=1`、`name_en=public`。
- `t_permission_role` 需要初始化 TinyEngine 官方角色。
- `r_auth_users_units_roles` 记录用户属于哪个租户、业务单元以及角色。
- `t_user.role` 只作为兼容/展示字段，不作为官方权限关系主来源；真实授权以 `r_auth_users_units_roles.role_id` 为准。

## 登录、用户与租户 SPI

starter 不内置业务账号体系，宿主应用需要实现 TinyEngine 用户体系 SPI，负责：

- 从请求中解析当前用户。
- 未登录时返回 401。
- 判断用户是否具备指定权限。
- 返回前端需要的用户信息，包括 `id/userId`、`username/userName`、`tenant/tenants`、`role/roles`。
- 注册用户时写入默认租户关系。
- 登出时清理宿主登录态。

sample 使用 JWT 演示登录流程；JWT 由前端保存并随请求携带，后端不保存 token 会话。生产环境如需要集群会话，可在宿主应用中改为网关统一认证、Spring Security、Spring Session + Redis，或企业 IAM；starter 不强制引入 Sa-Token，也不强制引入 Redis。

默认租户策略建议：

- 系统初始租户使用 `public`，数据库 `t_tenant.id=1`。
- 新注册用户默认加入 `public`，角色为 `Tinybuilder_App_Developer`。
- 超级管理员 `admin` 应关联 `public`，角色为 `Tinybuilder_Admin`。
- 旧 URL 中 `tenant=system` 可在宿主层兼容映射到 `public/1`，但数据库应有真实租户记录。

## 官方角色与关系表

TinyEngine 官方表结构中包含 6 个基础角色：

| role_id | role_code | 名称 |
| --- | --- | --- |
| 1 | `Tinybuilder_Admin` | 超级管理员 |
| 2 | `Tinybuilder_Tenant_Admin` | 组织管理员 |
| 3 | `Tinybuilder_Platform_Admin` | 设计器管理员 |
| 4 | `Tinybuilder_App_Admin` | 应用管理员 |
| 5 | `Tinybuilder_App_Developer` | 应用开发者 |
| 6 | `Guest` | 游客 |

用户、租户、角色的主关系在 `r_auth_users_units_roles`：

- `user_id`：用户 ID。
- `tenant_id`：租户 ID。
- `unit_id`：业务单元 ID；组织级关系通常等于租户 ID。
- `unit_type`：业务单元类型，例如 `tenant`。
- `role_id`：关联 `t_permission_role.id`。

前端“组织下拉”“当前用户”“应用创建人”等字段依赖 `/platform-center/api/user/me` 和应用列表接口返回可展示字段；后端应返回名称，不应只返回 id。

## 页面操作权限

页面按钮隐藏只能改善体验，不是安全边界；所有写操作必须由后端接口校验。

当前 starter 建议按下面的默认策略实现页面操作权限。官方源码提供角色、关系表和用户租户查询基础，但没有发现可直接照搬的完整“角色 -> 接口/按钮”矩阵；因此宿主应用可以按自身 IAM 替换该策略。

| 操作 | 建议允许角色 |
| --- | --- |
| 查看应用、查看页面、预览页面/应用 | `Tinybuilder_Admin`、`Tinybuilder_Tenant_Admin`、`Tinybuilder_Platform_Admin`、`Tinybuilder_App_Admin`、`Tinybuilder_App_Developer`、`Guest` |
| 进入设计器、读取物料/模型/i18n | `Tinybuilder_Admin`、`Tinybuilder_Tenant_Admin`、`Tinybuilder_Platform_Admin`、`Tinybuilder_App_Admin`、`Tinybuilder_App_Developer` |
| 锁定页面、解锁自己锁定的页面、编辑、保存、生成代码 | `Tinybuilder_Admin`、`Tinybuilder_Platform_Admin`、`Tinybuilder_App_Admin`、`Tinybuilder_App_Developer` |
| 强制解锁他人页面、删除页面、删除应用、发布应用 | `Tinybuilder_Admin`、`Tinybuilder_Platform_Admin`、`Tinybuilder_App_Admin` |
| 创建/删除组织、维护组织成员 | `Tinybuilder_Admin`；组织内成员维护可下放给 `Tinybuilder_Tenant_Admin` |
| 平台配置、物料中心、模型中心、资源组管理 | `Tinybuilder_Admin`、`Tinybuilder_Platform_Admin` |

接口侧建议：

- 未登录：返回 401，前端清理本地 token 并跳转登录页。
- 已登录但无权限：返回 403，前端提示权限不足。
- 锁定接口必须校验 `id` 有效，不能请求 `id=undefined`。
- 新建应用必须创建默认首页，避免进入设计器后没有 `pageid` 导致锁定失败。
- 锁定/解锁成功后，前端应同步刷新页面状态或更新布局状态，不能要求用户手动刷新。

## AI 功能

TinyEngine AI 功能由 `fw.plus.tiny-engine.ai.*` 开启，并依赖 `fw-plus.ai.model.*` 注册模型。

推荐最小配置：

```yaml
fw:
  plus:
    tiny-engine:
      ai:
        enabled: true
        provider: langchain4j
        model:
          chat-key: tinyengine-openai-chat
          streaming-chat-key: tinyengine-openai-streaming-chat
        chat:
          enabled: true
        web:
          enabled: true

fw-plus:
  ai:
    model:
      enabled: true
      default-model: tinyengine-openai-chat
      models:
        tinyengine-openai-chat:
          key: tinyengine-openai-chat
          framework: langchain4j
          provider: openai
          type: CHAT
          properties:
            baseUrl: ${OPENAI_BASE_URL}
            apiKey: ${OPENAI_API_KEY}
            modelName: ${OPENAI_MODEL}
        tinyengine-openai-streaming-chat:
          key: tinyengine-openai-streaming-chat
          framework: langchain4j
          provider: openai
          type: STREAMING_CHAT
          properties:
            baseUrl: ${OPENAI_BASE_URL}
            apiKey: ${OPENAI_API_KEY}
            modelName: ${OPENAI_MODEL}
```

RAG 如暂未接入向量库，保持：

```yaml
fw:
  plus:
    tiny-engine:
      ai:
        rag:
          enabled: false
```

启用 RAG 时，需要额外配置向量存储，并设置：

```yaml
fw:
  plus:
    tiny-engine:
      ai:
        rag:
          enabled: true
          max-results: 5
          min-score: 0.0
        vector:
          enabled: true
          store-key: default
        store:
          store-key: default
```

## 运行与验证

sample 模块可作为接入参考：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
mvn -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml spring-boot:run
```

访问地址：

- 设计器入口：`http://127.0.0.1:18083/tiny-engine/index.html`
- 应用设计页：`http://127.0.0.1:18083/tiny-engine/index.html?type=app&id=1&tenant=public&pageid=2`
- 页面预览：`http://127.0.0.1:18083/tiny-engine/preview.html?...`

sample 默认管理员仅用于本地开发：

- 用户名：`admin`
- 密码：`TinyEngine@123456`

生产环境必须修改默认密码，并接入企业账号体系。

建议验证接口：

```bash
curl -i http://127.0.0.1:18083/platform-center/api/user/me
curl -i http://127.0.0.1:18083/app-center/api/apps/page
curl -i "http://127.0.0.1:18083/app-center/api/apps/canvas/lock?id=2&state=occupy&type=page"
```

未登录时这些业务接口应返回 401；登录后 `/platform-center/api/user/me` 应返回当前用户、角色和租户列表。

## 常见问题

- 没登录也能进设计器：检查 `fw.plus.tiny-engine.security.enabled=true`，并确认宿主安全过滤器没有放行 TinyEngine API。
- 401 后仍停在当前页：前端需要在统一请求拦截器里处理 401，清 token 并跳转登录页。
- 新增组织成功后选不到：确认 `t_tenant` 写入成功，并且当前用户在 `r_auth_users_units_roles` 中有对应 `tenant_id` 关系。
- 应用列表创建人显示 id：应用列表接口应关联 `t_user` 返回用户名或昵称，前端再做展示字段归一化。
- 页面锁定请求出现 `id=undefined`：新建应用应自动创建默认首页；前端在没有 page/block id 时不能发锁定请求。
- 预览加载远程 npm 资源 CORS/502：生产应使用本地打包资源或可跨域访问的静态资源地址，不依赖不稳定的 registry 文件 URL。
