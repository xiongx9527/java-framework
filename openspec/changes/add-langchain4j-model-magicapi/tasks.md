## 1. Core 模型模块

- [x] 1.1 新建 `fw-plus-ai-langchain4j-model` Maven 模块，保持父 POM、`main.basedir`、编码和 JDK 1.8 约束与相邻 LangChain4j 模块一致。
- [x] 1.2 实现 `LangChain4jModelDefinition`、`LangChain4jModelType`、`LangChain4jModel`、`LangChain4jModelProviderAdapter`、`LangChain4jModelFactory`、`LangChain4jModelRegistry` 和模型异常类型。
- [x] 1.3 实现默认模型工厂和默认 registry，覆盖 adapter 选择、默认 key 校验、空返回防御和错误上下文。
- [x] 1.4 为 core 模型工厂和 registry 增加单元测试，覆盖成功创建、不支持 provider、adapter 异常、默认模型 key 无效等场景。

## 2. Maven 聚合与 BOM

- [x] 2.1 在 `fw-plus-ai-langchain4j` 聚合 POM 注册 `fw-plus-ai-langchain4j-model`，保留现有用户未提交模块改动。
- [x] 2.2 在 core dependencies BOM 中管理 `fw-plus-ai-langchain4j-model`，版本使用 `${project.version}`。
- [x] 2.3 在 AI Boot starters 聚合 POM 注册 `fw-plus-ai-langchain4j-model-spring-boot-starter`。
- [x] 2.4 在 AI Boot dependencies BOM 中管理 model starter，版本使用 `${project.version}`。

## 3. Spring Boot 自动配置与 starter

- [x] 3.1 新增 model properties，支持 `default-model`、`models.<key>.provider`、`model-type`、`base-url`、`api-key`、`model-name`、`enabled` 和扩展 `properties`。
- [x] 3.2 新增 model 自动配置，创建 `LangChain4jModelFactory`、provider adapters 和 `LangChain4jModelRegistry`。
- [x] 3.3 参考现有 Magic-API LangChain4j 模型工厂，将可复用的 provider 创建逻辑迁移到新的 provider adapter 体系，并移除旧工厂依赖。
- [x] 3.4 新建 `fw-plus-ai-langchain4j-model-spring-boot-starter`，依赖 `fw-plus-ai-boot-starter` 与 `fw-plus-ai-langchain4j-model`，不默认传递全部 provider SDK。
- [x] 3.5 为自动配置增加测试，覆盖条件装配、配置绑定、registry 创建、默认模型 key 校验和缺少 provider SDK 的隔离行为。

## 4. Magic-API 模型插件

- [x] 4.1 新增 Magic model 动态资源信息对象、资源存储和动态注册器，资源类型使用 `model-datasource`，资源定义转换为 `LangChain4jModelDefinition`。
- [x] 4.2 新增 Magic 动态模型数据源，合并 Spring Boot registry 与 Magic 运行态模型，明确 key 冲突和优先级规则。
- [x] 4.3 新增或调整 Magic 脚本模块，使脚本可按 key 获取 Chat、Streaming Chat、Embedding、Image、Language 和 Streaming Language 模型。
- [x] 4.4 新增 Magic 控制器测试接口和资源列表能力，响应与日志过滤 apiKey、secretKey、accessToken 等敏感字段。
- [x] 4.5 注册新的 Magic model 插件配置和前端资源，删除或替换旧 `langchain4j` 插件配置、脚本入口和 `langchain4j-datasource` 前端资源。

## 5. 旧实现迁移与删除

- [x] 5.1 删除或替换旧 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 下重复的模型工厂、动态数据源、资源存储、控制器、脚本模块和配置类。
- [x] 5.2 更新 Magic 插件聚合、AI Boot BOM、starter 依赖和示例项目，使运行入口迁移到新的 model 插件 artifact。
- [x] 5.3 明确旧 `fw-plus-ai-magicapi-plugin-langchain4j` artifact 的处理方式：删除模块或改为指向新 model 插件的空壳迁移模块，避免同一运行态出现两套入口。

## 6. 兼容性与文档

- [x] 6.1 保持现有 `fw-plus-ai-langchain4j-store-embeding`、Magic vector 插件和已有 LangChain4j provider 模块行为不变。
- [x] 6.2 补充 README 或示例配置，说明 model starter 最小依赖、provider SDK 显式引入方式、旧 Magic LangChain4j 插件迁移方式和 `model-datasource` 配置示例。
- [x] 6.3 检查所有新增 Java 代码避免使用 Java 9+ API，并保持包名、缩进和现有命名风格一致。

## 7. 验证

- [x] 7.1 在 `fw-plus-core` 下使用 Maven Wrapper 构建 `fw-plus-ai-langchain4j-model` 及受影响 core 模块。
- [x] 7.2 在 `fw-plus-ai` 下使用 Maven Wrapper 构建受影响的 boot autoconfigure、model starter 和 Magic model 插件模块。
- [x] 7.3 运行新增和受影响测试，至少覆盖 core model、boot autoconfigure、Magic model 插件、旧插件删除/替换迁移、现有 vector 插件兼容场景。
- [x] 7.4 记录验证命令、结果和任何环境限制，避免把依赖解析失败误判为功能失败。
