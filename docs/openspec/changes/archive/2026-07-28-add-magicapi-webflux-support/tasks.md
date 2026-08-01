## 1. 现状梳理与范围收敛

- [x] 1.1 盘点 `MagicAPIController` 中 option、resource reload、single resource reload、datasource options、datasource test 等管理接口的依赖和返回结构。
- [x] 1.2 盘点 `AuthConfiguration`、`AuthFilter`、Servlet filter registration、MVC controller registration 等 Servlet-only 依赖点。
- [x] 1.3 盘点 Boot、Cloud、AI 层 Magic-API 自动配置条件，确认 Servlet 环境继续走目前路径、WebFlux 环境进入新路径所需的边界。

## 2. Controller 与 WebFlux 管理接口

- [x] 2.1 提取或复用 Magic-API 管理接口中的公共服务逻辑，避免 Servlet 与 WebFlux 两套入口复制业务处理。
- [x] 2.2 新增 WebFlux 可用的 Magic-API 管理接口入口，覆盖 option list、resource reload、single resource reload、datasource options 和 datasource test。
- [x] 2.3 为 WebFlux 管理接口补齐必要的请求/授权适配，使权限检查可以使用现有 Magic-API authorization interceptor。
- [x] 2.4 确认 WebFlux 管理接口返回 `JsonBean`、`MagicJdbcClassOptions` 等 payload 形状与 Servlet controller 保持一致。

## 3. Servlet 相关隔离与兼容

- [x] 3.1 保持 Servlet/MVC 环境继续进入目前已有 Magic-API 自动配置、controller、filter 和 plugin controller registration 路径。
- [x] 3.2 为 Magic-API Servlet-only controller、filter、MVC mapping 和 plugin controller registration 补充明确 Servlet 条件。
- [x] 3.3 确保 WebFlux-only 应用上下文不会创建 `FilterRegistrationBean`、Servlet request/response 或 Spring MVC mapping 依赖的 Magic-API bean。
- [x] 3.4 仅在必要处调整 Cloud/AI Magic-API 自动配置条件，避免复制 Boot 层 controller 适配实现。

## 4. 测试与验证

- [x] 4.1 新增 Servlet 环境测试，断言仍进入当前 Magic-API Servlet/MVC 路径，且 controller/filter/plugin registration 行为不变。
- [x] 4.2 新增 WebFlux 应用上下文测试，覆盖 Magic-API enabled/disabled 下管理接口 handler 注册情况。
- [x] 4.3 新增 WebFlux 管理接口测试，覆盖 option list、datasource options、datasource test 和资源 reload 授权边界。
- [x] 4.4 新增 WebFlux-only 上下文测试，断言不创建 Servlet filter registration、MVC mapping 依赖和 Servlet-only controller registration bean。
- [x] 4.5 运行受影响 Maven 模块定向测试，并记录完整动态 API runtime 不属于本 change 验收范围。

## 5. 收尾

- [x] 5.1 检查新增代码保持 JDK 1.8 兼容，不使用 Java 9+ API。
- [x] 5.2 检查 Magic-API 2.2.2 与内部 Nexus 依赖版本未被修改。
- [x] 5.3 更新必要文档或注释，说明 Servlet 进入目前路径、WebFlux 进入新改造路径，且本 change 不包含完整 WebFlux 动态 API runtime。
