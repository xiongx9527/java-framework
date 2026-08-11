# AskData 文档与示例验证记录

本文件区分已在当前工作区运行的验证和需要真实依赖/外部测试租户才能运行的验证，不把文档覆盖当成运行成功。

## 已运行

| 验证 | 命令 | 结果 |
| --- | --- | --- |
| Java SDK 示例编译 | `cd docs/askdata/starter && javac -source 8 -target 8 -d .verify-plugin-sdk examples/AskDataPluginSdkExample.java` | 通过，Java 8 源码和目标版本均指定 |
| Java SDK 示例执行 | `java -cp .verify-plugin-sdk AskDataPluginSdkExample` | 通过，输出 `PLUGIN_SDK_EXAMPLE_OK` |
| Core 定向测试 | `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test` | 2026-07-28 17:56 通过，71 项测试、0 failures、0 errors、0 skipped |
| AI 自动配置定向测试 | `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure clean test -Dtest='*AskData*Test'` | 2026-07-28 17:57 通过，133 项测试、0 failures、0 errors、0 skipped；包含集群拒绝本地状态、SQL 安全、监控日志、模型路由、JDBC 查询超时和只读连接边界 |
| Starter 依赖链测试 | `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -am test` | 2026-07-28 18:03 通过，11 个 reactor 模块成功；AI 自动配置 196 项测试、0 failures、0 errors、0 skipped；AskData Starter 空源码模块按当前结构构建通过 |
| OpenSpec 严格校验 | `openspec validate rework-ai-askdata-core-starter --strict` | 2026-07-28 18:02 通过 |
| 未完成标记扫描 | 对目标目录执行精确模式扫描 | 通过，无匹配 |
| Markdown 空白检查 | `git diff --check -- docs/askdata/starter` | 通过；全仓库检查另有既有 `.superpowers/sdd` EOF 空白 |
| 提交文件范围 | `git diff-tree --no-commit-id --name-only -r HEAD` | 本次提交清单仅包含 `docs/askdata/starter/**` |

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
