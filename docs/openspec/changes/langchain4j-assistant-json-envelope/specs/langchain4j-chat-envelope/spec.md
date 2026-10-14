# Spec Delta

## Purpose

定义统一聊天信封的形态与取值语义，使流式与非流式两条对话通道在成功和失败时都输出同一份可编程判断的
JSON 结果，让前端与调用方不必再解析自由文本。

## ADDED Requirements

### Requirement: 信封键位恒定

信封 SHALL 恰好包含 5 个键：`ok`、`business`、`v`、`result`、`text`。成功与失败 SHALL 同形，
任何情况下都不得因为失败而缺少键。

#### Scenario: 成功轮次的键

- **WHEN** 某个业务轮次成功产出结构化结果
- **THEN** 信封包含全部 5 个键

#### Scenario: 失败轮次的键

- **WHEN** 某个轮次因为校验失败、解析失败或模型空回复而失败
- **THEN** 信封仍包含全部 5 个键，与成功轮次的键完全一致

### Requirement: 成功与失败的取值语义

`ok=true` 时 `result` SHALL 为业务结构化结果；`ok=false` 时 `result` SHALL 为 `null`。
`text` SHALL 表示“无结构可用时的可展示文本”：闲聊轮次为模型原文，失败轮次为兜底话术，
结构化成功轮次 SHALL 为空串。外层 SHALL NOT 出现任何业务语义字段。

#### Scenario: 结构化成功

- **WHEN** 业务轮次成功产出结构化结果
- **THEN** `ok=true`、`result` 为业务对象、`text` 为空串

#### Scenario: 失败轮次

- **WHEN** 轮次以失败结束
- **THEN** `ok=false`、`result=null`、`text` 为兜底话术

#### Scenario: 纯闲聊轮次

- **WHEN** 轮次没有对应的业务契约（兜底 `chat`）
- **THEN** `ok=true`、`result=null`、`text` 为模型原文

#### Scenario: 业务语义字段不外泄

- **WHEN** 业务结果中包含追问标记、缺失字段列表、意图或订单号等业务语义
- **THEN** 这些字段只出现在 `result` 内部，信封外层不出现

### Requirement: 收敛过程永不抛异常

任何输入——合法 JSON、夹带解释文字的 JSON、被截断的 JSON、纯文本闲聊、空回复——SHALL 被收敛为合法信封，
收敛过程 SHALL NOT 抛出异常。空回复 SHALL 降级为 `ok=false`，不得被视为成功。

#### Scenario: JSON 被截断

- **WHEN** 模型输出到一半被截断，JSON 不完整
- **THEN** 得到 `ok=false` 的合法信封，调用方不收到解析异常

#### Scenario: 模型空回复

- **WHEN** 模型没有返回任何内容
- **THEN** 得到 `ok=false` 的合法信封，`result=null`

#### Scenario: 收敛件收到异常对象

- **WHEN** 上游把异常信息传给收敛件
- **THEN** 收敛件把异常信息降级为 `ok=false` 的信封，不把异常向外抛出

### Requirement: 帧协议保持不变

`token` 帧 SHALL 保持纯文本。承载信封的 SHALL 只有 `done` 帧的 `data`。`error` 帧的形态 SHALL 与现状一致，
不新增传输概念。`start` 帧在开关关闭时 SHALL 保持现状（`data` 为 requestId 字符串）；开关开启时 `start`
帧的 `data` SHALL 为对象，包含 `requestId`、本轮 `business` 与本轮是否逐字输出（`tokens`），且 SHALL 在
本轮业务判定完成之后发出。

#### Scenario: 结构化轮次的 token 帧

- **WHEN** 帧形态开关开启时处理结构化轮次
- **THEN** `token` 帧仍是纯文本，不含信封

#### Scenario: 结构化轮次的 done 帧

- **WHEN** 帧形态开关开启时处理结构化轮次
- **THEN** `done` 帧的 `data` 是信封对象

#### Scenario: 开关开启时的 start 帧

- **WHEN** 帧形态开关开启，本轮判定为结构化轮次
- **THEN** `start` 帧的 `data` 含 `requestId`、本轮 `business` 与 `tokens=false`
- **AND** 该帧在本轮业务判定完成之后发出

#### Scenario: 开关关闭时的 start 帧

- **WHEN** 帧形态开关未开启
- **THEN** `start` 帧的 `data` 仍是 requestId 字符串，形态与现状一致

### Requirement: 流式与非流式同形

流式通道的 `done` 帧与非流式的返回 SHALL 由同一收敛过程产出，二者 SHALL 输出同一份信封（键与取值语义一致）。

#### Scenario: 同一轮次两种通道

- **WHEN** 同一轮次分别经流式与非流式通道输出
- **THEN** 两条通道给出的信封在键结构、`ok`、`business`、`result` 上一致

### Requirement: 帧形态开关只可全局配置且默认关闭

帧形态开关 SHALL 只能全局配置，默认关闭。关闭时既有输出 SHALL 保持现状（`done` 帧仍是原文文本）。
开关开启后，同一会话内的帧形态 SHALL 恒定，不得逐轮变化。

开关开启后，接管范围 SHALL 限定为既有的、调用方未自带 `done` 数据映射与回调的流式对话入口；调用方自带
映射或回调的入口，以及带检索过滤条件的流式入口与非流式对话入口，SHALL NOT 被接管并 SHALL 保持现状。
非流式信封 SHALL 由新增的独立入口提供，既有方法的返回形态 SHALL NOT 改变。

#### Scenario: 开关关闭

- **WHEN** 帧形态开关未开启
- **THEN** `done` 帧的 `data` 仍是模型原文文本，既有调用方行为一字不变

#### Scenario: 开关开启后会话内恒定

- **WHEN** 帧形态开关已开启，某轮次不想要结构化结果
- **THEN** 该轮次仍然输出信封（只把结构化开关关掉），帧形态不因轮次而改变

#### Scenario: 调用方自带 done 映射

- **WHEN** 帧形态开关已开启，调用方调用自带 `done` 数据映射或完成回调的流式入口
- **THEN** `done` 帧的 `data` 仍由调用方的映射决定，框架不接管该入口

#### Scenario: 带检索过滤的流式入口

- **WHEN** 帧形态开关已开启，调用方调用带检索过滤条件的流式入口
- **THEN** 该入口保持现状，不被帧形态开关接管

#### Scenario: 既有非流式入口

- **WHEN** 帧形态开关已开启，调用方调用既有非流式对话入口
- **THEN** 该入口的返回形态与现状一致，不返回信封

### Requirement: 结构化开关与三层覆盖顺序

是否产出 `result` SHALL 可配置，且 SHALL 支持全局默认、业务契约、单次调用三层。
覆盖顺序 SHALL 为：单次调用 > 业务契约 > 全局默认。全局默认值 SHALL 为 `false`（不产出结构化结果）。

#### Scenario: 三层都未声明

- **WHEN** 全局默认、业务契约、单次调用都没有打开结构化开关
- **THEN** 该轮次不产出 `result`

#### Scenario: 单次调用覆盖业务契约

- **WHEN** 业务契约打开了结构化开关，而某次调用显式关闭
- **THEN** 该次调用不产出 `result`

#### Scenario: 业务契约覆盖全局默认

- **WHEN** 全局默认关闭而某个业务契约打开
- **THEN** 该业务的轮次产出 `result`

#### Scenario: 业务命中但不产出结构化结果

- **WHEN** 本轮 `business` 命中已注册契约，但结构化开关未打开
- **THEN** 信封的 `business` 仍为该契约名、`result=null`、`text` 为可展示正文
- **AND** 该轮次逐字输出，帧形态与其他轮次一致

### Requirement: 非法配置的处置

启动阶段发现非法配置 SHALL 立即失败并给出具体配置项。运行阶段出现非法配置 SHALL 保留旧值并告警，
不得中断既有服务。

#### Scenario: 启动期非法配置

- **WHEN** 应用启动时配置了不可识别的开关取值
- **THEN** 启动失败并指出该项配置

#### Scenario: 运行期非法配置

- **WHEN** 服务运行中配置被改成非法值
- **THEN** 系统沿用旧值并记录告警，既有会话不受影响
