## 1. 测试准备

- [x] 1.1 为重复提交拦截器设计可单测的锁实现 stub，覆盖获取锁成功、获取锁失败、释放锁场景
- [x] 1.2 补充配置读取测试，验证接口 options 与 `magic.api.<path>.options.*` 覆盖顺序
- [x] 1.3 补充 key 生成测试，覆盖 SpEL、空 key fallback、不同请求 key 隔离、业务后缀 Bean 和默认后缀实现

## 2. 核心实现

- [x] 2.1 新增专用 Magic-API 重复提交 `RequestInterceptor`，在 `preHandle` 中按开关决定是否启用校验
- [x] 2.2 使用现有 `Lock` Bean 调用 `lock(key, expire, acquireTimeout)`，获取成功后把锁信息绑定到本次请求上下文
- [x] 2.3 获取锁失败或超时时使用 Magic-API `ResultProvider` 返回 `avoid_repeat_submit_msg`，并阻止接口脚本执行
- [x] 2.4 在 `afterCompletion` 中仅释放本请求成功获取的锁，并记录释放失败日志

## 3. 配置与 Key 生成

- [x] 3.1 复用 `Options.AVOID_REPEAT_SUBMIT_*` 与 `ParamConfig` 读取接口级配置
- [x] 3.2 实现 `avoid_repeat_submit_key` SpEL 解析，暴露 request、headers、params、pathVariables、body、api、requestEntity 等必要上下文
- [x] 3.3 实现空 key fallback，基于接口路径、HTTP 方法、请求参数和 request body 生成稳定摘要
- [x] 3.4 定义 `RepeatSubmitKeySuffixProvider` 接口和 `DefaultRepeatSubmitKeySuffixProvider` 默认实现，并实现 `avoid_repeat_submit_key_subfix_spring_bean` 按 beanName 选择业务实现或回退默认实现
- [x] 3.5 将 `avoid_repeat_submit_timeout` 按毫秒解释，并修正代码注释或文档中的单位说明

## 4. 自动配置接入

- [x] 4.1 在 `MagicAPIBaseAutoConfiguration` 中注册重复提交拦截器 Bean，并保持与现有 `ParamProvider`、`ResultProvider` 协作
- [x] 4.2 确认存在 `LockTemplate` 时使用分布式 `LockModule`，不存在时使用 `LocalLockModule` 兜底
- [x] 4.3 确认控制台 `optionList` 继续暴露已有重复提交配置项且键名不变

## 5. 验证

- [x] 5.1 运行新增单元测试，确认同 key 重复请求短路、不同 key 放行、异常后释放锁
- [x] 5.2 编译 `fw-plus-boot-autoconfigure` 指定模块，确保 JDK 1.8 与 Magic-API 2.2.2 兼容
- [x] 5.3 对照 `openspec/changes/add-magicapi-repeat-submit-lock/specs/magicapi-repeat-submit-lock/spec.md` 验证所有场景均有实现或测试证据
