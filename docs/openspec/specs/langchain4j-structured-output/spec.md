# langchain4j-structured-output Specification

## Purpose
让调用方以一份结构声明（JSON Schema 或 Java 类型）加若干校验规则作为契约，从大模型取得结构化结果；
输出不符合契约时自动把错误回喂给模型并重试，最终给出调用方可编程判断的成功或失败。

## Requirements

### Requirement: 结构声明同时驱动指令与本地校验

调用方 SHALL 能以 JSON Schema 文本或 Java 类型声明期望的输出结构，并可附加零到多条校验规则。
系统 SHALL 由同一份声明生成给模型的指令与本地校验依据，二者不得由两处配置分别给出。

#### Scenario: 用 JSON Schema 声明结构

- **WHEN** 调用方给出一份 JSON Schema
- **THEN** 发给模型的提示词包含该结构要求
- **AND** 模型返回后按同一份 schema 做本地校验

#### Scenario: 用 Java 类型声明结构

- **WHEN** 调用方给出一个 Java 类型而不是 schema 文本
- **THEN** 系统按该类型的属性生成等价的指令与本地校验依据

### Requirement: 宽容解析模型输出

模型输出夹带解释文字、Markdown 代码块标记或其他前后缀文本时，系统 SHALL 仍提取其中的 JSON 完成解析。
无法提取到可用 JSON 时 SHALL 判为失败，不得把解析异常抛给调用方。

#### Scenario: 输出含解释文字

- **WHEN** 模型返回“好的，结果如下：{...}”这类文本
- **THEN** 系统提取其中的 JSON 并正常解析

#### Scenario: 输出无法解析

- **WHEN** 模型返回的内容中不含可用 JSON
- **THEN** 该次尝试判为失败并进入重试或最终失败，调用方不收到解析异常

### Requirement: 双层校验与全量错误清单

系统 SHALL 先做 JSON Schema 结构校验，再执行调用方提供的校验规则。Schema 校验 SHALL 报告其发现的不符合项，
校验规则 SHALL 全部执行并汇总错误清单，不得因首条错误而提前停止。

#### Scenario: 结构不符合

- **WHEN** 模型返回的 JSON 缺少必填字段或字段类型不符
- **THEN** 该次尝试判为失败并产生结构类错误条目

#### Scenario: 多条规则同时失败

- **WHEN** 校验规则中有两条以上同时不成立
- **THEN** 错误清单包含全部这些条目的错误文本

### Requirement: 错误回喂重试

校验失败时系统 SHALL 把错误清单作为下一轮提示的一部分重新请求模型。最大尝试次数 SHALL 可配置，
未配置时 SHALL 为 3 次。重试 SHALL NOT 把错误原文写入业务会话的历史消息。

#### Scenario: 首轮失败后重试成功

- **WHEN** 首轮输出不符合契约，且模型在被提示错误后给出了符合契约的输出
- **THEN** 调用方得到成功结果，不感知中间失败轮次

#### Scenario: 达到最大尝试次数

- **WHEN** 连续 3 次尝试都不符合契约
- **THEN** 系统以失败结束，并给出可供排查的错误信息

#### Scenario: 重试不污染业务历史

- **WHEN** 同一业务会话内发生多轮重试
- **THEN** 该会话的历史消息中不出现校验错误原文

### Requirement: 支持嵌套路径的校验规则

内置校验规则 SHALL 覆盖必填、类型、枚举、数值范围与字符串模式五类，并 SHALL 接受点号路径（如 `a.b.c`）
定位嵌套字段；不通过时的错误文本 SHALL 包含该路径。

#### Scenario: 嵌套字段缺失

- **WHEN** 规则要求 `order.buyer.name` 必填而输出中该字段缺失
- **THEN** 错误文本包含路径 `order.buyer.name`

#### Scenario: 顶层字段规则保持可用

- **WHEN** 规则作用于不含点号的顶层字段
- **THEN** 行为与点号路径规则一致，错误文本包含该字段名

### Requirement: 从结构声明派生校验规则

系统 SHALL 能从 JSON Schema 派生等效的校验规则，覆盖必填、类型、枚举、数值范围与字符串模式，
并 SHALL 递归处理嵌套对象、为嵌套字段生成点号路径。派生规则与手写规则 SHALL 使用同一套错误文本格式。

#### Scenario: 派生规则覆盖嵌套必填

- **WHEN** schema 要求 `order.buyer.name` 必填而输出缺失该字段
- **THEN** 派生出的规则报错，且错误文本包含路径 `order.buyer.name`

#### Scenario: 派生规则与手写规则并用

- **WHEN** 调用方同时提供从 schema 派生的规则与手写规则
- **THEN** 两类规则都被执行，错误清单包含两者产生的错误文本
