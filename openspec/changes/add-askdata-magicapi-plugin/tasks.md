## 1. 模块接入

- [ ] 1.1 在 Magic-API AI 插件聚合下新增 `fw-plus-ai-magicapi-plugin-askdata` 模块。
- [ ] 1.2 将新模块加入 `fw-plus-ai-magicapi-plugin/pom.xml`。
- [ ] 1.3 增加对 `fw-plus-ai-askdata-spring-boot-starter` 和 Magic-API 的依赖，不重复引入模型或向量客户端。

## 2. Magic-API 模块 API

- [ ] 2.1 实现带有 `@MagicModule("askdata")` 的 `AskDataModule`。
- [ ] 2.2 增加 `ask(Map request)` 作为第一版脚本 API。
- [ ] 2.3 将 Magic-API 的 Map/JSON 参数转换为 `AskDataRequest`，并返回脚本友好的 Map 结果。
- [ ] 2.4 对缺少问题、非法数据源上下文或不支持的选项值返回清晰参数错误。

## 3. 自动配置

- [ ] 3.1 增加 `MagicAPIAskDataProperties`，配置前缀为 `magic-api.plugin.askdata`。
- [ ] 3.2 增加 `MagicAPIAskDataConfiguration`，注册条件化插件和 `AskDataModule` Bean。
- [ ] 3.3 使用 Spring Boot 2.7 兼容的元数据注册自动配置。

## 4. 复用现有 AI 模块

- [ ] 4.1 确认插件把模型选择和向量召回委托给 `AskDataService` 以及现有 `fw-plus-ai` 适配器。
- [ ] 4.2 记录脚本参数如何映射到现有模型 key、向量存储 key 或 AskData 召回选项。
- [ ] 4.3 除非 Starter 设计证明现有模块不足，否则不新增模型或向量配置专用的 Magic-API 数据源资源目录。

## 5. 向量管理模块

- [ ] 5.1 参考 `magic-s3.vue` 和 S3 独立插件 console 结构，新增 AskData 向量管理前端组件。
- [ ] 5.2 增加前端 service、i18n、图标和插件注册入口，用于加载 AskData 向量管理模块。
- [ ] 5.3 新增 AskData 向量资源模型和资源存储，覆盖表结构、术语、训练样例或向量集合引用。
- [ ] 5.4 新增 AskData 向量管理 Controller，支持列表、保存、测试和删除接口。
- [ ] 5.5 确认不实现项目列表、项目成员或项目权限管理界面。

## 6. 测试与验证

- [ ] 6.1 增加请求转换和结果转换单元测试。
- [ ] 6.2 增加 Spring 上下文测试，覆盖插件启用和禁用行为。
- [ ] 6.3 增加 Magic-API 模块冒烟测试，或等价的 Bean 级测试，验证 `import askdata` 注册。
- [ ] 6.4 增加向量管理 Controller 测试或等价服务测试。
- [ ] 6.5 构建 AskData 插件前端资源并验证产物可被插件加载。
- [ ] 6.6 使用相关 Maven Wrapper 构建插件模块和聚合模块。
