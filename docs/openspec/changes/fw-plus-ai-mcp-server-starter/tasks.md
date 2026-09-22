# Tasks: fw-plus-ai-mcp-server-starter

- [x] T1 修复 core 层 `fw-plus-ai-mcp` 编译问题，确保 JDK 1.8 下 `mvn compile` 与 `mvn test` 通过，并恢复 BOM 管理。 <!-- comet-task:8860089e-5dd7-4257-946a-6627f646361d -->
- [x] T2 完成 MCP 2025-11-25 协议核心能力：initialize 版本协商、`Mcp-Session-Id` 会话、Tools、Resources、Prompts、Completion、Logging、listChanged 通知与错误码。 <!-- comet-task:5352fc39-095b-4e91-babc-c531d83866fe -->
- [x] T3 完成 Spring MVC 与 WebFlux 两套 Streamable HTTP 传输实现，覆盖 POST / GET / DELETE 与 SSE。 <!-- comet-task:aeb53cfe-b03d-4557-a7b7-956818e7be1f -->
- [x] T4 完成 `fw-plus-ai-boot-autoconfigure` 自动配置：默认实例、`fw-plus.mcp.*` 配置、多实例、能力开关、MVC / WebFlux 互斥装配。 <!-- comet-task:fb678b13-4b1f-4b3c-a4f2-12089aba6af3 -->
- [x] T5 完成 SPI 扩展与 `@McpTool` 注解扫描，支持 Provider Bean 与方法注解两种扩展方式。 <!-- comet-task:1ba27ebd-45ed-4ce6-820d-427e915410a9 -->
- [x] T6 删除 `fw-plus-boot` 层重复空壳目录，并确认 AI 层以正确坐标引用 core 构件。 <!-- comet-task:cccbe50f-76e9-48c9-9041-58ffca3062a0 -->
- [x] T7 新增 `fw-plus-ai-mcp-server-sample` 样例，完成握手与主能力调用验证。 <!-- comet-task:c667da31-ad5b-4192-af16-eb54486e724a7 -->
- [x] T8 补充 Starter 使用文档，覆盖坐标、依赖、配置项、扩展方式与传输栈选择说明。 <!-- comet-task:221a6c3f-3a9e-4790-82fc-189c75a621e7 -->
- [x] T9 完成构建链验证：core -> ai -> sample 编译通过，集成测试与样例可运行。 <!-- comet-task:4d221737-38bf-43e5-bb3e-7f673e9887ef -->
