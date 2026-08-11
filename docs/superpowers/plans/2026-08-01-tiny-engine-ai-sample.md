# TinyEngine AI Sample Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增并启动一个使用 MySQL 和 OpenAI 兼容服务的 TinyEngine AI 示例应用。

**Architecture:** 在 AI 样例聚合器下新增独立 Spring Boot 模块，通过 `fw-plus-ai-tiny-engine-plugin-langchain4j` 组合 TinyEngine 与中立 AI 能力，并显式引入 OpenAI provider SDK。按用户要求在样例配置中保存运行凭据，测试配置使用 H2 与独立占位凭据保证可重复执行。

**Tech Stack:** Java 8、Spring Boot 2.7.18、MyBatis-Plus、MySQL 5.7、H2、LangChain4j 0.35.0、TinyEngine

## Global Constraints

- Core 与 Starter 代码保持 Java 8 兼容，不使用 Java 9+ API。
- OpenAI API Key 与数据库密码按用户明确要求写入该样例配置，不复制到其他文档或日志。
- 新模块归属 `fw-plus-ai-samples`，不得让 Boot 样例反向依赖 AI 层。
- 只启用 Chat，RAG 保持关闭，不增加向量数据库依赖。
- 保留用户现有 `.gitignore` 与 `.superpowers/sdd` 改动，不纳入本任务提交。

---

### Task 1: 新增可测试的 TinyEngine AI 样例模块

**Files:**
- Modify: `fw-plus-sample/fw-plus-ai-samples/pom.xml`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/main/java/com/xiongx9527/fw/plus/ai/tinyengine/TinyEngineAiSampleApplication.java`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/main/resources/application.yml`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/TinyEngineAiSampleApplicationTest.java`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/test/resources/application.yml`

**Interfaces:**
- Consumes: `fw-plus-ai-tiny-engine-plugin-langchain4j`、`fw-plus-ai-langchain4j-openai`、MySQL JDBC。
- Produces: `TinyEngineAiSampleApplication.main(String[])`，端口 `18083`，TinyEngine 页面 `/tiny-engine/`，AI Chat 路由 `/tiny-engine/ai/chat`。

- [ ] **Step 1: 编写失败的上下文测试**

```java
@SpringBootTest
class TinyEngineAiSampleApplicationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void startsWithTinyEngineAndAiWebEndpoints() {
		assertThat(context.getBean(ResourceMapper.class)).isNotNull();
		assertThat(context.getBean(TinyEngineAiController.class)).isNotNull();
	}
}
```

- [ ] **Step 2: 运行测试确认新模块尚不存在**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-sample/fw-plus-ai-samples/pom.xml -pl fw-plus-ai-tiny-engine-sample test`

Expected: FAIL，Maven 报告找不到 `fw-plus-ai-tiny-engine-sample` 模块。

- [ ] **Step 3: 添加聚合模块、POM、启动类和环境变量配置**

```yaml
server:
  port: ${TINY_ENGINE_PORT:18083}
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/tiny_engine?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: tiny_engine
    password: <configured database password>
fw:
  plus:
    tiny-engine:
      enabled: true
      security:
        enabled: false
      ai:
        enabled: true
        provider: langchain4j
        model:
          chat-key: chat-default
          streaming-chat-key: chat-default
        chat:
          enabled: true
        rag:
          enabled: false
        web:
          enabled: true
fw-plus:
  ai:
    model:
      enabled: true
      default-model: chat-default
      models:
        chat-default:
          framework: langchain4j
          provider: openai
          type: CHAT
          enabled: true
          properties:
            baseUrl: <configured OpenAI base URL>
            apiKey: <configured OpenAI API key>
            modelName: gpt-5.5
```

- [ ] **Step 4: 添加 H2 测试配置并运行模块测试**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-sample/fw-plus-ai-samples/pom.xml -pl fw-plus-ai-tiny-engine-sample test -Dmaven.test.skip=false`

Expected: `BUILD SUCCESS`，`TinyEngineAiSampleApplicationTest` 通过。

- [ ] **Step 5: 提交样例模块**

```bash
git add fw-plus-sample/fw-plus-ai-samples/pom.xml fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample
git commit -m "fw-plus-sample: 新增 TinyEngine AI 示例"
```

### Task 2: 启动并验证 MySQL、example 应用与 AI

**Files:**
- Runtime data: MySQL schema `tiny_engine`
- Runtime process: `TinyEngineAiSampleApplication`

**Interfaces:**
- Consumes: Task 1 的可执行样例、环境变量、已初始化 MySQL schema。
- Produces: 可访问的 TinyEngine 页面、数据库中的 `example` 应用和可调用的 AI Chat 路由。

- [ ] **Step 1: 构建所需 Core、Boot、AI artifacts 与样例**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-plugin-langchain4j -am install -DskipTests`

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 使用运行时环境变量启动样例**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml spring-boot:run`

Expected: Tomcat 在 `18083` 监听，应用启动日志不打印凭据。

- [ ] **Step 3: 验证页面与应用 API**

Run: `curl -f http://127.0.0.1:18083/tiny-engine/` 和 `curl -f http://127.0.0.1:18083/app-center/api/apps/page?currentPage=1&pageSize=10`

Expected: 页面返回 HTML，应用列表返回成功响应。

- [ ] **Step 4: 创建并核对 example 应用**

Run: `POST /app-center/api/apps/create` 后查询 MySQL `t_app`。

Expected: API 返回新应用 ID，`t_app.name = 'example'`，再次查询列表包含该应用。

- [ ] **Step 5: 验证 AI Chat 路由**

Run: `POST /tiny-engine/ai/chat`，请求正文包含一条最小用户消息。

Expected: HTTP 200 且返回非空模型回复；任何输出都不包含 API Key。
