# Tasks

## 1. 结构对齐：运行时持有者与装配

- [x] 1.1 新增七个 `MagicDynamic<Type>DataSource`（`implements DisposableBean`，按 `key` 持有/驱逐/列举实例，`destroy()` 统一关闭），并让七个 `*MagicDynamicRegistry` 注入各自类型的持有者而非共享 holder；验证：新增 `MagicDynamicLangchain4j*DataSourceTest` 覆盖驱逐即关闭、关闭异常仅记日志不阻断、`destroy()` 清空，全部通过 <!-- comet-task:7c931e83-14a9-44a0-96d4-0b71a17409eb -->
- [x] 1.2 删除共享 `Langchain4jModelHolder` 及其全部引用，确认无残留；验证：`grep -r "Langchain4jModelHolder" fw-plus-ai` 无结果，且 `./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure compile -nsu` 通过 <!-- comet-task:bb60100e-53ab-46ac-8c00-8bd6426f84b1 -->
- [x] 1.3 把 `Langchain4jDatasourceConfiguration` 拆为「父装配 + 七个按资源类型的顶层子 Configuration」：父类只保留 `plugin()` / `controllerRegister()` 与共享 bean，通过 `@Import` 引入七个子配置，每个子配置只声明本类型的持有者、存储与注册器；验证：新增 `Langchain4jDatasourceConfigurationTest` 断言七类 bean 齐备，并通过单类型故障隔离用例 <!-- comet-task:9711f454-4605-4a31-a66d-0c354cd91916 -->
- [x] 1.4 核对并修正目录分层：datasource 插件位于 `plugin/langchain4j/**`，脚本模块与工具桥位于 `modules/langchain4j/**`（修正设计初稿统一放在 `modules` 的偏差）；验证：目录清单与 design.md D7 一致，且模块编译通过 <!-- comet-task:e63751b7-027a-4532-a41d-2b63c11cff8b -->

## 2. 脚本模块只读化（BREAKING）

- [x] 2.1 `Langchain4jModule` 改为 `@MagicModule("langchain4j")`，删除全部 `build*` 与内部实例 map，七个 `get*` 只读委托对应类型持有者，未配置时抛中文异常并列出该类型已配置 `key`；验证：新增 `Langchain4jModuleTest` 覆盖取用委托、未配置异常含类型与 `key`、异常列出可用 `key` <!-- comet-task:0fa0f29a-16ed-479d-9843-5839d349d565 -->
- [x] 2.2 `remove(type, key)` 改为持有者驱逐语义、`destroy()` 清空全部持有者并关闭可关闭实例；验证：`Langchain4jModuleTest` 中驱逐后可重新加载、关闭时清空用例通过 <!-- comet-task:4f517478-07a8-4956-98c3-d647a85fb675 -->

## 3. provider 工厂与依赖

- [x] 3.1 AI 层 BOM 补齐 langchain4j 全生态供应商版本管理，`fw-plus-ai-boot-autoconfigure` 以 `optional` 引入模型、分词器与向量库 jar（含内置向量模型）；验证：`./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-dependencies install -DskipTests -nsu` 与 autoconfigure 编译通过 <!-- comet-task:9d6b483c-fe1c-458b-8521-ad725e654262 -->
- [x] 3.2 短名表改为由已注册工厂的能力探测生成，使存储校验与工厂注册表同源；验证：新增 `Langchain4jModelFactoryRegistryTest` 覆盖「每个短名都有工厂」「工厂类可加载」「表外短名被拒」「工厂支持的方法集合与资源类型表一致」 <!-- comet-task:ef795f81-e186-42b7-bcab-e3d84189025d -->
- [x] 3.3 为每个资源类型的构建方法补代表工厂单测（builder 链参数透传、默认值语义、读取器缺参报错）；验证：`Langchain4jOpenAiModelFactoryTest`、`Langchain4jQwenModelFactoryTest` 通过 <!-- comet-task:733eaa78-07a3-4c71-b272-1ee357739f43 -->
- [x] 3.4 强类型选项读取器的完整路径报错；验证：新增 `Langchain4jOptionsReaderTest`，断言缺失必填项时异常信息含 `langchain4j-chat-model[qwen-main].options.api-key` 形式路径 <!-- comet-task:d3edaff6-4f2e-4411-846c-5b34edd6de4e -->

## 4. embedding-store 依赖与启动恢复

- [x] 4.1 实现 `embeddingModelKey` 必填校验与注册期依赖解析，引用不可用时注册失败并提示需先配置并启用对应 embedding-model；验证：`Langchain4jEmbeddingStoreMagicDynamicRegistryTest` 覆盖缺失/未启用依赖的失败用例 <!-- comet-task:c0ff527c-c590-47f7-86bf-f8f9cfc0f1fc -->
- [x] 4.2 实现维度决策链：`options.dimension` → 被引用模型维度 → 中文报错；验证：同上测试覆盖三条分支 <!-- comet-task:04164d3b-6afd-4331-ac53-6688534784bf -->
- [x] 4.3 启动完成钩子对启动期失败的 embedding-store 统一重试一次注册；验证：`Langchain4jEmbeddingStoreStartupRetryTest` 覆盖「store 先于 model 加载后自动恢复」与「已成功的不产生重复实例」 <!-- comet-task:cfcadf67-5b71-4009-ad99-14127edd096a -->

## 5. 连接测试 Controller

- [x] 5.1 实现 `POST /langchain4j/{type}/datasource/test`：七类各自最小探测、未保存配置可测、streaming 两类只做构建验证、embedding-store 探测结束后关闭临时实例、返回 `ok` 或含类型/`key`/选项路径的中文错误；验证：新增 `Langchain4jDatasourceControllerTest` 覆盖七类与错误/未保存/关闭分支 <!-- comet-task:0bb739d2-45e7-4b94-8353-4c16c14ae71d -->

## 6. 前端 console 子工程

- [x] 6.1 新建 `fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j`（pom 仿 mcp 子工程，`exec-maven-plugin` 执行 npm 构建，`maven-resources-plugin` 复制产物）；验证：`./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j package -nsu` 后 `target/classes/magic-editor/plugins/magic-langchain4j.1.0.0.iife.js` 存在且非空 <!-- comet-task:0a8cecea-c192-47c0-b704-b20936305de1 -->
- [x] 6.2 实现 console 源码：`index` 注册七类 datasource、七个 Vue 组件、`(资源类型 × provider)` 字段元数据表、连接测试 service、中英文 i18n；验证：元数据表覆盖七类且每个已声明短名都有字段定义，`embedding-store` 组件含 `embeddingModelKey` 选择项 <!-- comet-task:141e9aec-cead-4f4a-a489-e9806fe6648f -->
- [x] 6.3 前端产物缺失时后端只按基础插件标识注册且不抛异常；验证：`Langchain4jDatasourceConfigurationTest` 中断言资源缺失的降级路径 <!-- comet-task:459d3315-bd41-44ad-bb45-2a9dd4c799bf -->

## 7. 工具桥（api / function → LangChain4j ToolProvider）

- [x] 7.1 工具快照注册表：两类资源独立启停、排除优先于包含、分组模式覆盖子分组、`api` 方法白名单与 opt-in（默认 `mcpTool`）、入参 schema（默认 `mcpInputSchema`，支持对象与 JSON 字符串并回退无参）；验证：`MagicApiToolRegistryTest` 全部用例通过 <!-- comet-task:b8e1540b-aa96-459f-a67f-cfeb09ae486f -->
- [x] 7.2 `ToolProvider` 与 `ToolExecutor`：工具名生成（`METHOD /path` 与函数名）、同名时函数优先、参数原样透传、映射/集合按 JSON 序列化、脚本异常与未知工具返回错误文本而不抛异常；验证：`MagicApiToolProviderTest`、`MagicApiToolExecutorTest` 全部用例通过 <!-- comet-task:a8c96d4c-3811-4556-84e3-64475222d7c2 -->
- [x] 7.3 资源变更监听使快照失效并按需重建（不使用 TTL），装配受工具开关与 magic-api 资源服务可用性约束；验证：`MagicApiToolChangeListenerTest`、`Langchain4jToolAutoConfigurationTest` 全部用例通过 <!-- comet-task:678cf728-124b-4228-b6c0-731b21687191 -->

## 8. 集成验收与审计

- [x] 8.1 集成测试：上下文拉起后七类存储/持有者/注册器 bean 齐备，以文件事件触发注册后按 `key` 可取、删除后消失；验证：扩展 `LangChain4jAutoConfigurationRegistrationTest` 并全绿 <!-- comet-task:a4873971-bad9-489b-9dfd-44ba509d5438 -->
- [x] 8.2 core 层零修改审计；验证：`git diff --name-only -- fw-plus-core` 输出为空 <!-- comet-task:769f981a-99ac-4ff3-83a9-f508432d0623 -->
- [x] 8.3 样例工程静态配置路径不回归（`langchain4j.open-ai.*`）；验证：`fw-plus-ai-chat-sample` 测试通过 <!-- comet-task:905777f1-c7d2-432e-ab03-88b2a42c4816 -->
- [x] 8.4 命名审计：folder、数据源类型、插件标识、包名、类名、脚本模块名均含 `langchain4j` 且无 `uid` 残留；验证：全仓检索无旧的 `uid` 模块名与旧命名 <!-- comet-task:29014e55-455a-4757-8b19-2cd9f090c771 -->
- [x] 8.5 控制台端到端验收：七类数据源的新建/保存/停用/删除/连接测试全链路可用，`options` 表单字段随 `provider` 变化；验证：手工操作记录（含所保存的 `{type}/*.json` 与测试返回） <!-- comet-task:1d5dc780-2200-4d85-826c-2bc9be97a8f7 -->
