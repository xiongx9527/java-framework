# magicapi-langchain4j-tool-provider Specification

## Purpose
让 magic-api 中已存在的 `api` 与 `function` 资源可以作为一个 LangChain4j 本地工具提供者交给大模型调用，无需经过 MCP，并允许按分组、路径、HTTP 方法与显式 opt-in 控制曝光范围。

## Requirements

### Requirement: 以本地工具提供者暴露 magic-api 资源

插件 SHALL 提供一个 LangChain4j 工具提供者，把 magic-api 的 `function` 与 `api` 资源作为工具规格与执行器成对提供，执行时不经过网络或 MCP 通道。工具名 SHALL 为：`function` 使用函数名，`api` 使用「HTTP 方法 + 空格 + 以斜杠开头的路径」。

#### Scenario: 提供函数工具
- **WHEN** 存在一个已保存的 `function` 资源
- **THEN** 工具提供者返回以该函数名为工具名的工具规格

#### Scenario: 提供 api 工具
- **WHEN** 存在一个已保存的 `POST` 类型 `api` 资源，路径为 `/order/create`
- **THEN** 工具提供者返回工具名为 `POST /order/create` 的工具规格

#### Scenario: 同名冲突时函数优先
- **WHEN** 一个 `function` 与一个 `api` 派生出的工具名相同
- **THEN** 结果中只保留一个该名称的工具，且其行为为函数

#### Scenario: 路径规范化
- **WHEN** `api` 资源的路径未以斜杠开头
- **THEN** 生成的工具名中路径以斜杠开头

### Requirement: 按资源类型独立启停

`function` 与 `api` 两类资源的曝光 SHALL 可以分别启用或停用，默认均启用。

#### Scenario: 默认两类都曝光
- **WHEN** 未配置任何范围相关选项且两类资源都存在
- **THEN** 两类资源都出现在工具集合中

#### Scenario: 关闭一类
- **WHEN** 将 `function` 类的启用开关设为 `false`
- **THEN** 工具集合中不包含任何函数工具，`api` 工具不受影响

### Requirement: 分组与路径过滤

每类资源 SHALL 支持按分组与路径的包含和排除过滤，模式使用 Ant 风格匹配。排除规则优先于包含规则；包含列表为空表示不限制。分组模式不带通配符时 SHALL 同时覆盖该分组本身与其子分组。

#### Scenario: 包含分组只保留匹配项
- **WHEN** 配置只包含某个分组
- **THEN** 工具集合只包含落在该分组（及其子分组）下的资源

#### Scenario: 排除优先于包含
- **WHEN** 同一资源同时命中包含分组与排除分组
- **THEN** 该资源不出现在工具集合中

#### Scenario: 分组模式覆盖子分组
- **WHEN** 包含分组写为不带通配符的分组路径，且存在该分组下的子分组资源
- **THEN** 子分组下的资源也被保留

#### Scenario: 空列表表示不限
- **WHEN** 包含列表与排除列表均为空
- **THEN** 该范围内全部资源都被保留

### Requirement: api 的 HTTP 方法与显式 opt-in

`api` 类资源 SHALL 支持 HTTP 方法白名单，白名单为空表示不限制；还 SHALL 支持 opt-in 模式，启用后只有显式声明了 opt-in 选项的 `api` 资源才被曝光。opt-in 选项名 SHALL 可配置，默认与 MCP 插件共用同一选项名，使同一处元数据同时控制两侧曝光。

#### Scenario: 方法白名单限制曝光
- **WHEN** 方法白名单只包含 `GET`
- **THEN** 工具集合中不包含 `POST` 类型的 api 工具

#### Scenario: 未启用 opt-in 时不额外限制
- **WHEN** opt-in 模式关闭
- **THEN** 通过其它过滤条件的 `api` 资源全部曝光

#### Scenario: 启用 opt-in 后只曝光已声明项
- **WHEN** opt-in 模式开启且某 `api` 资源未携带 opt-in 选项（或该选项为假值）
- **THEN** 该 `api` 资源不曝光，携带真值的 `api` 资源正常曝光

#### Scenario: opt-in 选项来自资源元数据
- **WHEN** 某 `api` 资源在元数据中以属性形式声明 opt-in 选项为真
- **THEN** 该资源被判定为已 opt-in

### Requirement: 工具入参 schema

工具入参 SHALL 由资源元数据中指定的 schema 属性生成，属性名可配置且默认与 MCP 插件共用同一属性名，使同一处元数据同时驱动两侧工具参数。属性值为对象时 SHALL 直接采用；为 JSON 字符串且其 `type` 为 `object` 时 SHALL 解析后采用；缺失或无效时 SHALL 回退为「无参对象」schema。

#### Scenario: 采用对象形式的 schema
- **WHEN** 资源元数据中的 schema 属性为对象
- **THEN** 该对象被作为工具入参定义

#### Scenario: 采用 JSON 字符串形式的 schema
- **WHEN** 资源元数据中的 schema 属性为 JSON 字符串且 `type` 为 `object`
- **THEN** 该字符串被解析并作为工具入参定义

#### Scenario: 回退为无参 schema
- **WHEN** 资源未声明 schema 属性，或声明的值无效
- **THEN** 工具入参为空参数表，工具仍可被提供

#### Scenario: 自定义 schema 属性名
- **WHEN** 将 schema 属性名改为自定义名称
- **THEN** 插件只读取该自定义属性，未声明时回退为无参 schema

### Requirement: 工具执行的入参与结果语义

执行 SHALL 把模型传来的 JSON 参数解析后原样透传给对应的 magic-api 脚本；返回值 SHALL 序列化为字符串，其中映射、集合与数组按 JSON 序列化，其它类型按字符串形式返回。工具不存在或脚本执行异常时 SHALL 返回错误文本而不是把异常抛给模型调用循环。

#### Scenario: 参数透传
- **WHEN** 模型以 JSON 参数调用一个函数工具
- **THEN** 脚本收到解析后的参数映射

#### Scenario: 结构化返回值序列化
- **WHEN** 脚本返回映射或集合
- **THEN** 执行结果为其 JSON 字符串

#### Scenario: 标量返回值
- **WHEN** 脚本返回字符串或数字等标量
- **THEN** 执行结果为其字符串形式

#### Scenario: 脚本异常返回错误文本
- **WHEN** 脚本执行过程中抛出异常
- **THEN** 执行返回描述失败的文本，调用方不收到异常

#### Scenario: 未知工具
- **WHEN** 模型请求一个已不在当前工具集合中的工具名
- **THEN** 执行返回未知工具的错误文本

### Requirement: 工具集合随资源变更同步

工具集合 SHALL 在首次读取时构建并缓存，且在 magic-api 的 `api` / `function` 资源发生增删改后失效并重建，使变更对后续模型调用可见；插件 SHALL 不使用固定存活时间的缓存策略。

#### Scenario: 新增资源后可见
- **WHEN** 在控制台新增一个符合过滤条件的资源
- **THEN** 后续模型调用可看到并使用该工具

#### Scenario: 修改资源后生效
- **WHEN** 修改某个资源的路径、方法或分组，使其不再符合过滤条件
- **THEN** 该工具从后续工具集合中消失

#### Scenario: 删除资源后消失
- **WHEN** 删除某个资源
- **THEN** 该工具从后续工具集合中消失

#### Scenario: 缓存不引入可见性延迟
- **WHEN** 资源刚保存完成并立即读取工具集合
- **THEN** 结果已反映该变更，无需等待固定的过期时间

### Requirement: 工具提供者装配条件

工具提供者相关装配 SHALL 以 LangChain4j 工具能力与 magic-api 资源服务同时可用为条件，并提供独立开关（默认开启）。

#### Scenario: 缺少 magic-api 资源服务时不装配
- **WHEN** magic-api 未启用或其资源服务 bean 缺失
- **THEN** 不装配工具提供者相关 bean，且不影响应用启动

#### Scenario: 开关关闭时不装配
- **WHEN** 工具提供者开关配置为 `false`
- **THEN** 不装配工具提供者相关 bean

#### Scenario: 作为工具提供者注入使用
- **WHEN** 使用方把该工具提供者注入到 `AiServices` 的 `toolProvider` 位置
- **THEN** 模型可调用 magic-api 资源对应的工具
