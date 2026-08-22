# TinyEngine AI Sample 设计

## 目标

在 `fw-plus-sample/fw-plus-ai-samples` 下新增一个可独立启动的 TinyEngine AI 样例，使用本机 MySQL 持久化 TinyEngine 数据，并通过环境变量连接 OpenAI 兼容服务。

## 模块边界

- 新样例命名为 `fw-plus-ai-tiny-engine-sample`，归属 AI 样例聚合器。
- 样例依赖 `fw-plus-ai-tiny-engine-plugin-langchain4j`，由它聚合 TinyEngine Boot Starter 与中立 AI Starter，并显式选择 LangChain4j adapter。
- 样例显式依赖 `fw-plus-ai-langchain4j-openai` 提供 OpenAI SDK，不把 provider SDK 传递责任下沉到中立 Starter。
- 样例显式依赖 MySQL JDBC 驱动；测试使用 H2，不依赖本机服务。

## 运行配置

- 服务端口默认为 `18083`。
- MySQL 默认连接 `127.0.0.1:3306/tiny_engine`，用户名和密码均允许通过环境变量覆盖。
- OpenAI Base URL、API Key、`gpt-5.5` 模型名与数据库连接信息按用户要求直接写入样例配置文件。
- 关闭样例不使用的 Magic-API 与通用 MP 增强自动配置；TinyEngine 自身的 MyBatis-Plus Mapper 仍由 TinyEngine Starter 注册。
- TinyEngine AI Chat 与兼容 REST 入口启用，RAG 关闭，避免引入当前任务不需要的向量数据库依赖。
- TinyEngine 安全开关保持关闭，使用系统用户上下文，不要求登录账号。

## 验证

- 模块测试验证 Spring Boot 上下文、TinyEngine 资源路由、数据库 Mapper 与 AI Chat 路由可以注册。
- Maven 校验验证新模块已加入 AI 样例聚合器且依赖边界正确。
- 运行时验证 `/tiny-engine/` 页面、应用列表 API、`example` 应用创建与 MySQL 落库。
- AI 验证通过 TinyEngine AI Chat REST 入口发起最小请求，验证输出不回显 API Key。
