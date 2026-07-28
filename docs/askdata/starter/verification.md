# AskData 文档与示例验证记录

本文件区分已在当前工作区运行的验证和需要真实依赖/外部测试租户才能运行的验证，不把文档覆盖当成运行成功。

## 已运行

| 验证 | 命令 | 结果 |
| --- | --- | --- |
| Java SDK 示例编译 | `cd docs/askdata/starter && javac -source 8 -target 8 -d .verify-plugin-sdk examples/AskDataPluginSdkExample.java` | 通过，Java 8 源码和目标版本均指定 |
| Java SDK 示例执行 | `java -cp .verify-plugin-sdk AskDataPluginSdkExample` | 通过，输出 `PLUGIN_SDK_EXAMPLE_OK` |
| 未完成标记扫描 | 对目标目录执行精确模式扫描 | 通过，无匹配 |
| Markdown 空白检查 | `git diff --check` | 通过 |
| 目标文件范围 | `git status --short` 与 `git diff --name-only` | 仅本目录文件发生变更 |

## 需要运行的环境验证

| 范围 | 验证内容 | 当前状态 |
| --- | --- | --- |
| Core | AskData 上下文、能力注册、SQL 插件、存储、路由和多问题定向测试 | 需要在完整 Maven 依赖可解析的环境运行 |
| AI 自动配置 | 默认 LangChain4j/Ollama、条件装配、Java 8 编译和 Starter 测试 | 需要在完整 Maven 依赖可解析的环境运行 |
| 集群存储 | JDBC/Redis/向量库双节点读写、租约、幂等、迁移和故障注入 | 需要隔离测试集群 |
| 外部系统 | DataEase、MaxKB、Dify、Coze、n8n、MCP、企业微信、钉钉和飞书联调 | 需要测试租户、回调域名和可撤销凭证 |
| Java 17 运行时 | HTTP/SSE、RPC、消息、MCP 版本协商、错误映射、灰度和回滚 | 需要独立 Java 17 适配服务 |

## 验证口径

文档示例通过不代表外部系统已接通。完成外部联调必须记录请求关联 ID、协议版本、能力 key、错误分类、审计查询结果、脱敏扫描结果和回滚结果；任何真实 token、密码、AES-Key、连接串、原始模型响应和 SQL 参数都不得写入验证记录。
