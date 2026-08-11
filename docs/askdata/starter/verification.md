# AskData 文档与示例验证记录

本文件区分已在当前工作区运行的验证和需要真实依赖/外部测试租户才能运行的验证，不把文档覆盖当成运行成功。

## 已运行

| 验证 | 命令 | 结果 |
| --- | --- | --- |
| Java SDK 示例编译 | `cd docs/askdata/starter && javac -source 8 -target 8 -d .verify-plugin-sdk examples/AskDataPluginSdkExample.java` | 通过，Java 8 源码和目标版本均指定 |
| Java SDK 示例执行 | `java -cp .verify-plugin-sdk AskDataPluginSdkExample` | 通过，输出 `PLUGIN_SDK_EXAMPLE_OK` |
| Core 完整 JDK 8 reactor | `JAVA_HOME=<zulu-8> mvn -f fw-plus-core/fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-askdata,fw-plus-ai/fw-plus-ai-model,fw-plus-ai/fw-plus-ai-store-embedding,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter -am clean install` | 2026-08-11 通过，10 个 reactor 模块成功；Common 7、AskData 119、Model 19、Embedding Store 19、LangChain4j Adapter 37，共 201 项测试，0 failures、0 errors |
| AI 自动配置与 Starter 完整 JDK 8 reactor | `JAVA_HOME=<zulu-8> mvn -f fw-plus-ai/fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure,fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -am clean test` | 2026-08-11 通过，9 个 reactor 模块成功；AI 自动配置 594、AskData Starter 2，共 596 项测试，0 failures、0 errors |
| LangChain4j + JDBC 独立示例 | `JAVA_HOME=<zulu-8> mvn -f docs/askdata/starter/examples/langchain4j-jdbc/pom.xml clean compile` | 2026-08-11 通过；示例 `target` 已清理 |
| Core 运行时依赖边界 | 对 AskData、Model、Embedding Store 执行 `dependency:tree -Dscope=runtime` | 通过，不含 Spring 或 LangChain4j 依赖 |
| AskData Starter 依赖边界 | 对 AskData Starter 执行 `dependency:tree -Dscope=runtime` | 通过，不含 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 依赖 |
| Java 8 API 与字节码 | 扫描 Java 9+ API，并以 JDK 8 `javap -verbose` 检查代表类 | 通过，无 Java 9+ API 命中；Core API、Checkpoint SPI、Model、Embedding Store、LangChain4j Adapter 和 AskData 自动配置代表类均为 major version 52 |
| Java 17 Adapter 文档 | `docs/ai-framework-migration/verify-migration-docs.sh` | 通过，Spring AI、Spring AI Alibaba、AgentScope 三份目标文档满足发布边界；校验同时禁止把 LangChain4j 写成默认实现 |
| 文档完整性 | 检查 TBD/默认厂商表述、功能清单事实源、Markdown 表格列数和本地链接 | 通过；无 TBD，无默认模型或 Provider，`openspec/changes/rework-ai-askdata-core-starter/feature-list.md` 存在，表格和链接有效 |
| Markdown 空白检查 | `git diff --check` | 通过 |

## 需要运行的环境验证

| 范围 | 验证内容 | 当前状态 |
| --- | --- | --- |
| Core | Java 8 Core 契约、SQL 插件链、存储、记忆、推荐、Agent、Remote Runtime 契约 | 定向测试已通过；Java 17 独立适配服务仍需真实服务联调 |
| AI 自动配置 | 业务显式选择的 LangChain4j Adapter、无默认 Provider、条件装配、JDBC SQL 执行超时、只读连接边界和 Starter 测试 | 定向 AskData 测试已通过；真实 Provider 服务联调仍需环境 |
| 集群存储 | JDBC/Redis/向量库双节点读写、租约、幂等、迁移和故障注入 | 需要隔离测试集群 |
| 外部系统 | DataEase、MaxKB、Dify、Coze、n8n、MCP、以及由业务 IAM/JustAuth 对接的企业微信、钉钉和飞书 | 需要测试租户、回调域名和可撤销凭证；AskData 只验证已映射的 `operatorId/projectId` |
| Java 17 运行时 | HTTP/SSE、RPC、消息、MCP 版本协商、错误映射、灰度和回滚 | 需要独立 Java 17 适配服务 |

## 验证口径

文档示例通过不代表外部系统已接通。完成外部联调必须记录请求关联 ID、协议版本、能力 key、错误分类、审计查询结果、脱敏扫描结果和回滚结果；任何真实 token、密码、AES-Key、连接串、原始模型响应和 SQL 参数都不得写入验证记录。
