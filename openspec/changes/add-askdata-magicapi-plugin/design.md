## Context

`fw-plus-ai` 已有 `fw-plus-ai-magicapi-plugin` 聚合，以及 LangChain4j、Milvus、Qdrant、Weaviate 等 Magic-API AI 插件子模块。现有插件通常通过 Spring Boot 自动配置注册 `MagicPluginConfiguration`，再提供 `@MagicModule` 标注的脚本模块供 Magic-API 使用。

AskData Magic-API 插件应作为这个体系的同级插件出现。核心问数流程、模型调用、多 agent 编排、向量召回、SQL 校验和 SQL 执行均来自 `fw-plus-ai-askdata-spring-boot-starter` 及其对现有 AI 模块的复用；插件只负责把 Java 服务转换为 Magic-API 脚本 API。

## Goals / Non-Goals

**Goals:**

- 在 `fw-plus-ai-magicapi-plugin` 下新增 `fw-plus-ai-magicapi-plugin-askdata` 子模块。
- 注册 Magic-API 插件元信息和 `@MagicModule("askdata")` 模块。
- 提供脚本侧 API，使 `.ms` 脚本可以调用 AskData 服务完成智能问数。
- 参考 S3 插件的 `magic-s3.vue`、独立插件 console、service、i18n 和 Controller 结构，提供 AskData 向量管理界面与后端管理接口。
- 保持 Java 8、Spring Boot 2.7.18 和 Magic-API 2.2.2 兼容。
- 复用 AskData Starter、现有模型管理模块和向量模块，不在插件内重复维护模型/向量配置。

**Non-Goals:**

- 不实现 AskData 核心 Text-to-SQL 流程。
- 不新增独立模型管理、独立向量库客户端或独立 RAG 配置。
- 不实现项目管理模块、SQLBot UI 或外部 MCP Server。
- 不使用 SQLBot 品牌名称或资源。

## Decisions

### Decision: 插件模块作为 Magic-API AI 插件同级子模块

新增模块路径为 `fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-askdata`，父 POM 使用 `fw-plus-ai-magicapi-plugin`，并在插件聚合 POM 中加入 module。

替代方案是把 askdata 模块放进 LangChain4j 插件。该方案文件更少，但会把智能问数业务能力绑定到具体模型插件，和 AskData Starter 的独立服务边界冲突。

### Decision: 使用 `@MagicModule("askdata")` 暴露脚本模块

插件提供 `AskDataModule`，标注 `@MagicModule("askdata")`。脚本调用形态倾向：

```magic-script
import askdata;
return askdata.ask({ question: '统计本月订单金额', datasource: 'order_ds', execute: true });
```

替代方案是把能力挂到现有 `langchain4j` 模块下。该方案能复用模型脚本入口，但用户语义不清晰，也不利于后续增加问数专用参数。

### Decision: 插件只注入 `AskDataService`

`AskDataModule` 通过构造器注入 `AskDataService`，负责 Map/JSON 参数与 `AskDataRequest` 的转换，再把 `AskDataResult` 转回 Magic-API 可消费的 Map 或对象。

替代方案是在插件内直接调用 LangChain4j、向量库和 JDBC。该方案绕开 Starter，导致脚本插件和 Java 服务接口行为不一致。

### Decision: 配置分两层

AskData 核心配置继续使用 `fw.plus.ai.askdata`；Magic-API 插件启用、管理页等插件行为使用 `magic-api.plugin.askdata`。

替代方案是所有配置都放在 `magic-api.plugin.askdata` 下。该方案对 Magic-API 项目直观，但普通 Spring Boot 项目引入 Starter 时会出现 Magic-API 命名泄漏。

### Decision: 模型和向量能力由现有模块提供

脚本侧可以传入模型 key、向量集合 key 或召回策略等参数，但这些参数只转发给 AskData Starter 的模型/向量适配层。模型 key 是可选参数：传入时使用指定模型，不传时由 Starter 使用默认模型。若现有 `fw-plus-ai` 模型管理或向量模块无法支撑多模型和默认模型语义，需优先改造现有公共接口。

替代方案是在 AskData 插件中新增自己的模型和向量数据源资源目录。该方案短期自洽，但会与已有 LangChain4j/Milvus/Qdrant/Weaviate 插件重复。

### Decision: 只做向量管理，不做项目管理

AskData 插件前端只新增向量管理模块，参考 `magic-s3.vue` 的控制台交互、资源表单、service 封装、i18n 和插件注册方式。向量管理用于维护智能问数召回所需的表结构、术语、训练样例或向量集合引用，并通过后端 Controller 写入 Magic-API 资源存储或等价配置。该管理模块不引入“项目”概念，不提供项目列表、项目成员、项目权限或项目级编排。

替代方案是同时做项目管理和向量管理。该方案更像完整产品控制台，但会扩大范围，且用户已明确不需要项目管理。

## Risks / Trade-offs

- [Risk] Magic-API 脚本参数类型松散，容易传错字段 -> 提供明确的 Map 参数解析和错误提示。
- [Risk] 插件先于 Starter 完成会缺少接口依赖 -> build 阶段必须先完成 `add-askdata-spring-boot-starter` 或至少稳定其 API。
- [Risk] AskData 结果对象过复杂，脚本难以消费 -> 插件返回 Map 结构，保留 `success`、`sql`、`data`、`steps`、`error` 等稳定顶层字段。
- [Risk] 现有模型/向量模块能力不足 -> 插件不私自补一套实现，而是推动 Starter 或公共 AI 模块补适配接口。
- [Risk] 向量管理 UI 与现有 Magic-API 控制台风格不一致 -> 参考 `magic-s3.vue` 和 S3 独立插件的组件、service、i18n、图标注入和 Controller 结构。

## Migration Plan

1. 新增 AskData Magic-API 插件 Maven 子模块。
2. 依赖 `fw-plus-ai-askdata-spring-boot-starter` 和 Magic-API 基础依赖。
3. 新增 `AskDataModule`、`MagicAPIAskDataConfiguration` 和配置属性。
4. 新增 AskData 向量管理资源模型、资源存储、Controller 和控制台组件。
5. 注册 Spring Boot 2.7 兼容自动配置。
6. 添加 Spring 上下文测试、脚本模块参数转换测试和向量管理 Controller/前端构建验证。

回滚策略：删除新增插件子模块，从聚合 POM 和自动配置注册中移除 AskData 插件引用。核心 Starter 不受影响。

## Open Questions

- 脚本 API 第一版是否只提供 `ask(Map request)` 一个入口，还是同时提供 `generateSql`、`execute` 等细粒度方法。
- 向量管理第一版的数据类型是否先覆盖表结构、术语、训练样例和向量集合引用；当前设计倾向覆盖这些核心问数召回资源。
- Magic-API 脚本中数据源标识如何映射到 AskData 的数据源上下文，需要结合现有动态数据源插件实现细化。
