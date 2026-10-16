# langchain4j-model-datasource-plugins Specification

## Purpose
让 LangChain4j 的七类模型与向量库实例能够作为 magic-api 数据源在控制台可视化配置、热更新与停用，实例由插件按数据源生命周期创建和销毁，调用方按数据源 key 取用。

## Requirements

### Requirement: 七类 LangChain4j 数据源类型注册

插件 SHALL 向 magic-api 控制台的数据源页签注册七个数据源类型：`langchain4j-chat-model`、`langchain4j-streaming-chat-model`、`langchain4j-language-model`、`langchain4j-streaming-language-model`、`langchain4j-embedding-model`、`langchain4j-tokenizer`、`langchain4j-embedding-store`。

#### Scenario: 控制台列出七类数据源类型
- **WHEN** 插件已装配且打开 magic-api 控制台数据源页签
- **THEN** 页签中出现七个 `langchain4j-*` 类型，每个类型有对应的新建入口

#### Scenario: 每个类型独立的持久化目录
- **WHEN** 在某一类型下保存一个数据源
- **THEN** 该数据源的 JSON 持久化到以该数据源类型命名的目录中，扩展名为 `.json`

### Requirement: 数据源 JSON 的公共字段与 options 分离

每个数据源 JSON SHALL 由公共字段与 `options` 对象两部分组成。公共字段为 `key`、`name`、`provider`、`enabled`；`langchain4j-embedding-store` 额外包含必填公共字段 `embeddingModelKey`。除公共字段外的全部参数 SHALL 放在 `options` 对象中，且不同 provider 的 `options` 键集合允许不同。

#### Scenario: options 携带 provider 差异参数
- **WHEN** 保存一个 `provider` 为通义千问的 chat-model 数据源，其 `options` 含 `enable-search`
- **THEN** 该键被完整持久化，读取时原样返回

#### Scenario: options 允许为空
- **WHEN** 保存的数据源不含 `options` 或 `options` 为空对象
- **THEN** 保存成功，最小依赖默认值构建实例

#### Scenario: name 缺省
- **WHEN** 保存的数据源未提供 `name`
- **THEN** 该数据源的展示名回退为 `key`

#### Scenario: 实例在生命周期中唯一
- **WHEN** 同一数据源被多次保存或重载
- **THEN** 同一时刻只有一个同 `key` 的实例对外可用

### Requirement: key 校验

数据源 `key` SHALL 为必填、非空、仅由字母、数字、下划线、点、连字符组成，且在同一数据源类型内唯一。不同数据源类型之间允许使用相同 `key`。

#### Scenario: 缺少 key 被拒绝
- **WHEN** 保存的数据源 `key` 为空或未提供
- **THEN** 保存被拒绝并返回中文错误信息

#### Scenario: 非法字符被拒绝
- **WHEN** 保存的数据源 `key` 含约定字符集之外的字符（例如空格或斜杠）
- **THEN** 保存被拒绝并返回中文错误信息

#### Scenario: 同类型重复 key 被拒绝
- **WHEN** 在同一数据源类型下保存已存在 `key` 的数据源
- **THEN** 保存被拒绝并返回中文错误信息

#### Scenario: 跨类型同名 key 允许
- **WHEN** 在两个不同数据源类型下分别保存相同 `key`
- **THEN** 两次保存都成功，且各自取用时互不影响

### Requirement: provider 短名表校验

`provider` SHALL 为必填，且必须命中所属数据源类型的可用短名集合。每个数据源类型 SHALL 拥有独立的短名集合，表外取值 SHALL 被拒绝，错误信息 SHALL 列出该类型的全部可用短名。

#### Scenario: 表内 provider 通过
- **WHEN** 保存的数据源 `provider` 属于所属类型的短名集合
- **THEN** 保存通过

#### Scenario: 表外 provider 被拒绝
- **WHEN** 保存的数据源 `provider` 不属于所属类型的短名集合
- **THEN** 保存被拒绝，且错误信息列出该类型全部可用短名

#### Scenario: 各类型短名集合互相独立
- **WHEN** 使用某 provider 短名保存 chat-model 成功，而该短名不在 tokenizer 的短名集合内，则用同一短名保存 tokenizer
- **THEN** tokenizer 的保存被拒绝

#### Scenario: 每个已声明短名都有对应构建能力
- **WHEN** 遍历某一类型短名集合中的每个短名
- **THEN** 都能为该类型构建出实例，不存在声明了短名却无法构建的取值

### Requirement: embedding-store 与 embedding-model 的依赖

`langchain4j-embedding-store` SHALL 要求 `embeddingModelKey` 必填，并引用一个已存在且已启用的 `langchain4j-embedding-model` 数据源的 `key`。向量维度的决策顺序 SHALL 为：先取 `options.dimension`，缺省时取被引用 embedding-model 的维度，两者都不可得时注册失败。

#### Scenario: 缺少 embeddingModelKey 被拒绝
- **WHEN** 保存的 embedding-store 数据源未提供 `embeddingModelKey`
- **THEN** 保存被拒绝并返回中文错误信息

#### Scenario: 引用的 embedding-model 不可用
- **WHEN** 注册 embedding-store 时其 `embeddingModelKey` 指向的 embedding-model 不存在或未启用
- **THEN** 该次注册失败，并提示需要先配置并启用对应的 embedding-model

#### Scenario: 维度取 options 优先
- **WHEN** embedding-store 的 `options.dimension` 有值
- **THEN** 使用该值，忽略被引用 embedding-model 的维度

#### Scenario: 维度回退到 embedding-model
- **WHEN** embedding-store 未提供 `options.dimension`
- **THEN** 使用被引用 embedding-model 的维度

#### Scenario: 维度无法确定
- **WHEN** 既未提供 `options.dimension`，被引用 embedding-model 也无法提供维度
- **THEN** 注册失败并返回中文错误信息

### Requirement: 数据源的动态注册与生命周期

数据源的注册 SHALL 遵循固定顺序：先驱逐同 `key` 的旧实例，再检查 `enabled`，然后构建新实例，仅在构建成功后才纳入可用集合。`enabled=false` 的数据源 SHALL 只做清理，不构建、不纳入可用集合并视为保存成功。构建失败 SHALL 不产生可用实例，并向控制台返回失败反馈。删除数据源 SHALL 驱逐实例，对可关闭的实例执行关闭，关闭异常 SHALL 仅记录日志且不阻断其它数据源的注销。

#### Scenario: 保存后实例可用
- **WHEN** 保存一个 `enabled=true` 且能成功构建的数据源
- **THEN** 该 `key` 对应实例进入可用集合并可被取用

#### Scenario: 同 key 覆盖时先驱逐旧实例
- **WHEN** 对已存在的 `key` 重新保存且新配置构建失败
- **THEN** 旧实例已被驱逐，该 `key` 没有可用实例

#### Scenario: 构建失败不落库
- **WHEN** 某个数据源的配置无法构建出实例
- **THEN** 注册返回失败，不产生孤立实例，控制台保存得到错误反馈

#### Scenario: 停用只清理
- **WHEN** 保存一个 `enabled=false` 的数据源，或把已启用数据源改为 `enabled=false`
- **THEN** 该 `key` 的实例被驱逐，不执行构建，且保存被视为成功

#### Scenario: 删除后实例消失
- **WHEN** 删除一个数据源
- **THEN** 该 `key` 不再可用，且可关闭的实例被关闭

#### Scenario: 关闭异常不阻断后续注销
- **WHEN** 驱逐某个可关闭实例时其关闭方法抛出异常
- **THEN** 异常只被记录日志，其它数据源的驱逐与注销继续进行

#### Scenario: 启动期加载不触发构建副作用以外的工作
- **WHEN** 应用启动过程中加载已保存的数据源
- **THEN** 注册器按上述顺序处理每个数据源，不因列举已加载资源而重复构建实例

### Requirement: 依赖顺序与启动恢复

当 `langchain4j-embedding-store` 的加载事件先于其引用的 `langchain4j-embedding-model` 到达时，该次注册 SHALL 失败并记录，且在应用启动完成后 SHALL 对启动期失败的数据源统一重试一次注册，使重启后无需人工干预即可恢复。

#### Scenario: 启动顺序不利时的自动恢复
- **WHEN** 启动过程中 embedding-store 先于其引用的 embedding-model 被加载
- **THEN** 该次注册失败，启动完成后重试注册成功，该 embedding-store 最终可用

#### Scenario: 重试不改变已成功状态
- **WHEN** 启动期已成功注册的数据源在启动完成重试时被再次处理
- **THEN** 其结果保持可用，不产生重复实例

### Requirement: 连接测试

插件 SHALL 提供按数据源类型区分的连接测试入口，接受尚未保存的完整数据源配置并即时构建实例，按类型执行最小探测后返回成功或中文错误信息。测试 SHALL 不改变已保存的数据源与其可用实例。`langchain4j-embedding-store` 的测试 SHALL 在探测结束后关闭其临时创建的实例。

#### Scenario: 对话类测试成功
- **WHEN** 对配置正确的 chat-model 或 language-model 发起连接测试
- **THEN** 返回成功，且该测试触发了对应模型的一次最小调用

#### Scenario: 配置错误返回中文错误
- **WHEN** 对缺少必填 `options` 的数据源发起连接测试
- **THEN** 返回中文错误信息，错误信息包含导致失败的数据源类型、`key` 与选项路径

#### Scenario: 未保存的配置也可测试
- **WHEN** 对一份尚未保存的数据源配置发起连接测试
- **THEN** 测试正常执行，且该配置不会被持久化或被纳入可用集合

#### Scenario: 流式类型只做构建验证
- **WHEN** 对 streaming-chat-model 或 streaming-language-model 发起连接测试
- **THEN** 测试只验证实例能否构建成功，不发起生成调用

#### Scenario: 向量库测试后关闭探测实例
- **WHEN** 对 embedding-store 发起连接测试
- **THEN** 测试执行一次写入与检索探测，并在结束后关闭该次探测创建的实例

### Requirement: 装配条件与按类型独立装配

插件装配 SHALL 以 magic-api 已启用且其资源服务可用为条件，并提供总开关（默认开启）。七个数据源类型 SHALL 按类型独立装配；单个类型的装配或构建失败不得阻止其它类型可用。

#### Scenario: magic-api 未启用时不装配
- **WHEN** 应用未启用 magic-api 或其资源服务 bean 缺失
- **THEN** 插件不注册任何数据源类型，且不影响应用启动

#### Scenario: 总开关关闭时不装配
- **WHEN** 插件总开关配置为 `false`
- **THEN** 不注册任何数据源类型与相关 bean

#### Scenario: 单类型故障隔离
- **WHEN** 七个类型中某一个的装配过程抛出异常
- **THEN** 其余六个类型仍完成装配并可正常保存与取用数据源

### Requirement: 控制台前端资源与插件标识

插件 SHALL 声明插件标识并向前端提供控制台资源，使每个数据源类型在控制台有对应的配置组件；`options` 表单字段 SHALL 依据「数据源类型 × provider」的元数据渲染，因此不同 provider 呈现不同字段。

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
