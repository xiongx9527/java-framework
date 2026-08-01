## ADDED Requirements

### Requirement: Repeat submit validation can be configured per Magic-API interface
系统 SHALL 支持通过 Magic-API 接口 options 和 `magic.api.<path>.options.*` 配置每个 `api` 类型接口的重复提交校验行为。

#### Scenario: Repeat submit validation is disabled
- **WHEN** 某个 Magic-API 接口的 `avoid_repeat_submit_enable` 解析结果为 `false`
- **THEN** 系统 MUST 不尝试获取重复提交锁
- **AND** 系统 MUST 按原有流程执行该接口脚本

#### Scenario: Repeat submit validation is enabled
- **WHEN** 某个 Magic-API 接口的 `avoid_repeat_submit_enable` 解析结果为 `true`
- **THEN** 系统 MUST 在接口脚本执行前执行重复提交锁校验

#### Scenario: External config overrides interface option
- **WHEN** 接口 option 和 `magic.api.<path>.options.avoid_repeat_submit_enable` 同时存在
- **THEN** 系统 MUST 使用 `magic.api.<path>.options.avoid_repeat_submit_enable` 的配置值参与本次请求判断

### Requirement: Repeat submit key is generated from request context
系统 SHALL 为每次受保护的 Magic-API 请求生成稳定 repeat key，并允许通过配置表达式动态生成 key 片段。

#### Scenario: SpEL key expression uses request data
- **WHEN** `avoid_repeat_submit_key` 配置为可解析的 SpEL 表达式并引用请求 header、参数、path variable、body 或接口信息
- **THEN** 系统 MUST 使用表达式结果参与生成本次请求的重复提交锁 key

#### Scenario: Different keys do not block each other
- **WHEN** 两个并发请求解析出的重复提交锁 key 不相同
- **THEN** 系统 MUST 允许两个请求分别获取锁并进入各自接口脚本执行

#### Scenario: Empty key falls back to request fingerprint
- **WHEN** `avoid_repeat_submit_key` 未配置或解析结果为空
- **THEN** 系统 MUST 基于接口路径、HTTP 方法、请求参数和请求体生成稳定 fallback key

#### Scenario: Configured suffix bean contributes to key
- **WHEN** `avoid_repeat_submit_key_subfix_spring_bean` 指定的 Spring Bean 存在且实现框架定义的重复提交 key 后缀接口
- **THEN** 系统 MUST 调用该 Spring Bean 获取后缀
- **AND** 系统 MUST 将非空后缀纳入最终重复提交锁 key

#### Scenario: Default suffix provider is used when spring bean is unavailable
- **WHEN** `avoid_repeat_submit_key_subfix_spring_bean` 未配置、指定 Bean 不存在或指定 Bean 未实现框架定义的重复提交 key 后缀接口
- **THEN** 系统 MUST 使用框架默认后缀实现 `DefaultRepeatSubmitKeySuffixProvider` 生成后缀
- **AND** 系统 MUST NOT 因后缀 Bean 不可用而中断接口请求

### Requirement: Magic lock prevents duplicate script execution
系统 SHALL 使用 Magic `lock` 模块能力在接口脚本执行期间保护同一个 repeat key，阻止重复请求进入脚本。

#### Scenario: First request obtains lock and executes script
- **WHEN** 重复提交校验开启且当前 repeat key 未被占用
- **THEN** 系统 MUST 使用 Magic `lock` 模块获取该 key 的锁
- **AND** 系统 MUST 继续执行接口脚本

#### Scenario: Duplicate request fails to obtain lock
- **WHEN** 重复提交校验开启且另一个请求已持有相同 repeat key 的锁
- **THEN** 系统 MUST NOT 执行重复请求对应的接口脚本
- **AND** 系统 MUST 返回 `avoid_repeat_submit_msg` 配置的提示信息

#### Scenario: Acquire timeout is honored
- **WHEN** `avoid_repeat_submit_timeout` 配置为有效毫秒值
- **THEN** 系统 MUST 将该值作为获取锁等待超时时间传递给 Magic `lock` 模块

### Requirement: Repeat submit lock is released after request completion
系统 SHALL 在受保护请求完成时释放本次获取的重复提交锁。

#### Scenario: Successful script releases lock
- **WHEN** 受保护接口脚本正常执行完成
- **THEN** 系统 MUST 在请求完成后释放本次获取的锁
- **AND** 后续相同 repeat key 的请求 MUST 能再次进入接口脚本执行

#### Scenario: Failed script releases lock
- **WHEN** 受保护接口脚本执行过程中抛出异常
- **THEN** 系统 MUST 在异常处理完成后释放本次获取的锁
- **AND** 后续相同 repeat key 的请求 MUST 能再次进入接口脚本执行

#### Scenario: Duplicate request does not release another request lock
- **WHEN** 重复请求未成功获取相同 repeat key 的锁
- **THEN** 系统 MUST NOT 释放正在由其他请求持有的锁
