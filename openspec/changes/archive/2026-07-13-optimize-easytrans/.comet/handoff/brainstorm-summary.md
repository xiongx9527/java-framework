# Brainstorm Summary

- Change: optimize-easytrans
- Date: 2026-07-12

## 已确认事实

- 产物语言：zh-CN。
- 范围仅限 `fw-plus-core/fw-plus-project/fw-plus-easytrans` 的 annotation/common/service 三个子模块。
- 必须保持公开 API、注解、配置 key、返回结构兼容。
- 优先稳定性与明确热点性能；不新增依赖，不新增未立即使用的抽象。
- 用户已确认允许将递归已翻译对象判重从 `ArrayList` 内部调整为按对象身份判重的集合，只要保持引用身份判重语义。

## 代码上下文摘录

- `MateEasyTransUtils.transOne` 是共享入口，被 AOP、ResponseBodyAdvice、参数翻译和递归字段翻译调用。
- `transOne` 当前使用 `ArrayList<Object> hasTransObjs` + 线性 `contains` 做引用判重。
- `transBatch` 当前对集合多次调用 `iterator().next()`，并在首元素为 null 时直接跳过整批集合。
- `transFields` 对字段递归调用 `transOne`，反射异常会把类加入 `notTransClassNamesMap` 并停止该类后续翻译。
- `createProxyVo`/`genNewClass` 依赖 `VO.getTransMap()` 和 ByteBuddy 动态类，风险较高，不宜大改。

## 候选方案

### 方案 A：共享链路最小修复（推荐）

- 聚焦 `MateEasyTransUtils`：空集合/首元素 null、重复首元素读取、递归判重结构、代理创建 null 兜底。
- 保持方法签名兼容；必要时增加私有辅助方法或内部重载适配，不暴露新 API。
- 测试覆盖 `transBatch`/`transOne` 的 null、空集合、首元素 null、循环引用/重复对象路径。

### 方案 B：各翻译服务分别修补

- 在 Boolean/Enum/Dictionary/RPC 各自服务中补 guard 和测试。
- 优点是局部清晰；缺点是无法覆盖 AOP/ResponseBodyAdvice/递归字段共享问题，容易漏 sibling caller。

### 方案 C：缓存/代理层重构

- 重做反射元数据缓存、动态代理缓存和转换上下文。
- 潜在性能收益更大；但风险高、范围大，违背保持兼容和最小改动目标。

## 推荐方案

采用方案 A：共享链路最小修复。

## 关键取舍与风险

- 不重构 ByteBuddy 动态代理：降低兼容风险。
- 判重结构从线性列表调整为引用身份集合：减少递归判重开销，保持语义。
- 不新增独立缓存层：避免缓存失效复杂度。
- 风险：`hasTransObjs` 当前是公开方法参数类型的一部分，直接改签名会破坏兼容；应通过内部适配或新增私有上下文方法避免对外签名变更。

## 测试策略

- 在 `fw-plus-easytrans-service` 增加最小单元测试，优先直接覆盖 `MateEasyTransUtils`。
- 测试不启动完整 Spring 容器，尽量使用简单 VO 和 stub `TransService`。
- 必测路径：null、空集合、首元素 null 但后续元素可翻译、重复对象/循环引用不重复递归、代理创建失败返回原对象。
- 验证命令优先运行 easytrans 模块测试/编译。

## Spec Patch

无。当前 delta spec 已覆盖兼容、失败兜底、重复工作和最小验证。
