# Spec Delta

## Purpose

让 magic-api 脚本以一个只读入口按 key 取用已配置的 LangChain4j 模型与向量库实例，把实例来源统一收敛到数据源配置，脚本自身不再负责构建与缓存模型。

## ADDED Requirements

### Requirement: 脚本模块名与只读取用入口

magic-api 脚本 SHALL 通过模块名 `langchain4j` 引入该能力。模块 SHALL 为七类资源分别提供按 `key` 取用实例的方法：对话模型、流式对话模型、语言模型、流式语言模型、向量模型、分词器、向量库。取用方法 SHALL 是只读的，不产生、不注册、不替换实例。

#### Scenario: 引入模块
- **WHEN** 脚本执行 `import langchain4j`
- **THEN** 可以调用该模块的取用方法

#### Scenario: 模块名为 langchain4j
- **WHEN** 脚本引用该模块
- **THEN** 模块名为 `langchain4j`，旧的占位模块名不再可用

#### Scenario: 按 key 取到已配置实例
- **WHEN** 脚本用某个已存在且已启用的数据源 `key` 调用对应类型的取用方法
- **THEN** 返回该数据源对应的实例

#### Scenario: 取用不改变实例集合
- **WHEN** 脚本反复调用取用方法
- **THEN** 可用实例集合与实例本身保持不变，不产生新的实例

### Requirement: 不再支持脚本内自建模型

模块 SHALL 不提供脚本侧构建或缓存模型的编程式入口。模型实例的唯一来源 SHALL 是通过数据源配置注册进运行时的实例。

#### Scenario: 编程式构建入口不可用
- **WHEN** 脚本调用旧有的 `build*` 形式构建方法
- **THEN** 该调用不可用（方法不存在或调用报错），脚本需改用数据源配置与取用方法

### Requirement: 未配置 key 的错误行为

当取用的 `key` 在该类型下没有可用实例时，模块 SHALL 抛出中文异常，且异常信息 SHALL 便于定位，包含所请求的类型与 `key`，并列出该类型当前已配置的 `key`。

#### Scenario: key 不存在
- **WHEN** 脚本取用一个未配置或不存在的 `key`
- **THEN** 抛出中文异常，信息中包含请求的类型与 `key`

#### Scenario: 已停用的 key
- **WHEN** 脚本取用一个 `enabled=false` 数据源的 `key`
- **THEN** 行为与未配置一致，抛出中文异常

#### Scenario: 异常信息列出可用 key
- **WHEN** 该类型下已有其它可用数据源
- **THEN** 异常信息中列出这些已配置的 `key`

### Requirement: 驱逐语义

模块 SHALL 提供按类型与 `key` 移除实例的入口，其语义为从运行时可用集合中驱逐该实例，而不是删除已保存的数据源配置。被驱逐后再次取用该 `key` SHALL 表现为未配置。

#### Scenario: 驱逐后不可取用
- **WHEN** 调用移除入口移除某类型的某个 `key`
- **THEN** 之后取用该 `key` 抛中文异常

#### Scenario: 驱逐不删除配置
- **WHEN** 驱逐某 `key` 后重新加载其数据源
- **THEN** 该 `key` 可重新取用

### Requirement: 应用关闭时释放实例

模块 SHALL 在应用关闭时清空全部可用实例，并对可关闭的实例执行关闭，释放其占用的连接或资源。

#### Scenario: 关闭时释放
- **WHEN** 应用关闭
- **THEN** 各类可用集合被清空，可关闭的实例被关闭
