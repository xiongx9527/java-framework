# Brainstorm Summary

- Change: add-magicapi-repeat-submit-lock
- Date: 2026-07-23

## 确认的技术方案

新增独立 `RepeatSubmitRequestInterceptor`，由 Spring Bean 自动注册进 Magic-API 2.2.2 的请求生命周期。拦截器在 `preHandle` 中读取 `Options.AVOID_REPEAT_SUBMIT_*` 配置、生成 repeat key、调用现有 `Lock` Bean 获取锁；在 `afterCompletion` 中释放当前请求成功获取的锁。

key 后缀扩展采用框架接口加 Spring Bean：框架定义 `RepeatSubmitKeySuffixProvider`，并提供 `DefaultRepeatSubmitKeySuffixProvider implements RepeatSubmitKeySuffixProvider` 默认实现；业务 Bean 实现该接口后可通过 `avoid_repeat_submit_key_subfix_spring_bean` 配置 beanName 覆盖默认实现，未配置或不可用时走默认实现。

## 关键取舍与风险

- repeat key 解析采用 SpEL + 字面值兼容：`#{...}` 按表达式解析；默认 `#{magicapi:repeat:submit}` 这种历史值需要兼容为普通字面片段，避免默认配置直接解析失败。
- 最终锁 key 必须包含固定前缀、HTTP method、API path 或 id、表达式/指纹片段，避免默认值导致所有 API 串行。
- `avoid_repeat_submit_timeout` 按毫秒处理，因为 Magic lock `acquireTimeout` 为毫秒且默认值是 `1000`。
- 重复提交响应使用 Magic-API 统一结果包装，推荐 `ResultProvider.buildResult(requestEntity, Constants.RESPONSE_CODE_INVALID, msg, null)`，并显式把 `JsonBean.success=false`，不抛 BizException，避免正常重复提交场景刷错误日志。
- 后缀 Bean 只按 beanName 查找并校验 `RepeatSubmitKeySuffixProvider` 接口，不猜测方法名；未配置、Bean 不存在或类型不匹配时走默认实现，不中断请求。
- 本地锁只保护单实例；存在 `LockTemplate` 时沿用已有分布式 `LockModule`。

## 测试策略

- 单测 key resolver：SpEL、默认字面兼容、空 key fallback、请求参数/body 摘要、业务后缀 provider、默认后缀实现回退。
- 单测拦截器：开关关闭不拿锁，首请求拿锁放行，同 key 拿锁失败返回失败态 JsonBean，不同 key 隔离，成功/异常 afterCompletion 释放锁，未持锁请求不释放他人锁。
- 自动配置/编译：确认 `RepeatSubmitRequestInterceptor` Bean 注册，`LockTemplate` 存在时分布式 lock 生效，不存在时本地 lock 兜底；编译 fw-plus-boot-autoconfigure 指定模块。

## Spec Patch

已回写：补充无可用业务 Spring Bean 时必须使用 `DefaultRepeatSubmitKeySuffixProvider` 默认实现的验收场景。
