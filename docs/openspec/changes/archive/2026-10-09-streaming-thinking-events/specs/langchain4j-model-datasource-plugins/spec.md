# Spec Delta

## MODIFIED Requirements

### Requirement: 控制台前端资源与插件标识

插件 SHALL 声明插件标识并向前端提供控制台资源，使每个数据源类型在控制台有对应的配置组件；`options` 表单字段 SHALL 依据「数据源类型 × provider」的元数据渲染，因此不同 provider 呈现不同字段。对于已核实支持独立思考输出的 streaming-chat-model provider，其 provider 专属思考选项 SHALL 在控制台中可配置，并与后端工厂读取的选项一致；不支持独立思考输出的 provider SHALL 不展示伪造的通用思考选项。

#### Scenario: 插件资源可加载
- **WHEN** 插件前端产物存在于约定资源路径
- **THEN** 控制台加载该产物，并以插件标识注册该插件的页面能力

#### Scenario: 按类型与 provider 渲染表单
- **WHEN** 在控制台新建某类型数据源并切换 `provider`
- **THEN** `options` 表单字段随所选 provider 变化，只显示该 provider 声明的字段

#### Scenario: embedding-store 选择 embedding-model
- **WHEN** 在控制台新建 `langchain4j-embedding-store`
- **THEN** 存在一个可选择已配置 `langchain4j-embedding-model` 的 `embeddingModelKey` 输入项

#### Scenario: 插件标识三元组恒定上报
- **WHEN** 读取任意一个类型插件的 `Plugin` 声明
- **THEN** 无论前端产物是否落在 classpath 上，都同时上报插件标识、全局名 `globalName` 与前端产物名 `javascriptFilename`；后端不按产物缺失做降级注册

#### Scenario: 产物模块缺失导致控制台加载失败
- **WHEN** 使用方未把承载前端产物的 Maven 模块声明为依赖
- **THEN** 后端仍正常上报三元组且服务端装配不受影响，但控制台请求该前端产物时得到 404，对应配置面板无法渲染

#### Scenario: thinking 字段与后端能力一致
- **WHEN** 在控制台配置 streaming-chat-model 的 provider
- **THEN** 只显示该 provider 经确认可用于独立思考输出的真实 provider 专属选项，且每个显示字段均由对应后端工厂读取
