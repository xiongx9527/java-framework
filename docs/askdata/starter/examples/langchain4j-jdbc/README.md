# LangChain4j Provider + JDBC 顺序问数示例

该示例是独立 Spring Boot 2.7.18 / Java 8 Maven 应用，显式组合 AskData Starter、LangChain4j Adapter、OpenAI-compatible Provider SDK 和 H2 JDBC。`business-chat`、Provider 端点和模型名称全部由业务配置，不是框架默认值。

## 前置条件

1. 使用 Java 8，并先在仓库中安装当前 Core 与 AI 快照。
2. 准备支持 OpenAI Chat Completions 的 Provider 端点；可使用任意兼容服务。
3. Provider 必须按 Prompt 返回 JSON，至少包含 `sql`、`tables`、`chartType`、`brief`。针对示例问题，合法响应类似：

```json
{"sql":"select count(*) as order_count, sum(amount) as total_amount from orders limit 100","tables":["orders"],"chartType":"table","brief":"订单统计"}
```

## 运行

从仓库根目录执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  /usr/local/bin/mvn -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-askdata,fw-plus-ai/fw-plus-ai-model,fw-plus-ai/fw-plus-ai-store-embedding,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter \
  -am install -DskipTests

JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  /usr/local/bin/mvn -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter \
  -am install -DskipTests

cd docs/askdata/starter/examples/langchain4j-jdbc
ASKDATA_PROVIDER_BASE_URL=https://provider.example/v1 \
ASKDATA_PROVIDER_API_KEY=replace-with-secret \
ASKDATA_PROVIDER_MODEL=business-model \
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  /usr/local/bin/mvn spring-boot:run
```

成功时输出最终 SQL 和两行 H2 数据的聚合结果；实际模型 key 仍会写入 AskData 结果内部状态、检查点和脱敏观测事件。Provider 凭据只通过环境变量进入业务应用，不能提交到配置文件或日志。

## 安全边界

- 示例治理 Bean 只允许 `demo-project/demo-operator` 请求中的 `demo.orders.id/amount`；生产实现必须接入真实 IAM 和授权版本源。
- H2 对 `Connection.isReadOnly()` 的报告不适合作为生产证明，因此示例显式关闭连接只读状态检查。生产必须恢复 `read-only-connection-required=true`、使用数据库只读账号，并保留 SQL 最终校验、限行、超时和审计。
- 示例使用单机内存状态和向量 Store，进程退出后丢失。集群必须改用分类 JDBC/Redis/业务共享 Store、租约、幂等和持久向量能力。
- 若模型返回非 JSON、写 SQL、未授权表列、多语句或缺少物理 LIMIT，Flow 将 fail-closed，不执行 JDBC。
