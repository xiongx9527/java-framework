---
comet_change: optimize-easytrans
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-13-optimize-easytrans
status: final
---

# optimize-easytrans 技术设计

## 目标与边界

本设计细化 `fw-plus-easytrans` 的稳定性与性能优化方案。实现范围仅限：

- `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-annotation`
- `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-common`
- `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service`

本次优化保持现有公开 API、注解、配置 key 和响应结构兼容；不新增第三方依赖，不引入独立缓存框架，不修改 Boot、sample 或 product 层代码。

## 现状与问题

`MateEasyTransUtils.transOne` 是当前结果翻译的共享入口，被 AOP、`ResponseBodyAdvice`、参数翻译和递归字段翻译复用。核心链路包含：

1. 判断对象类型：`null`、`Map`、`VO`、`Collection`、Java 标准类型、普通对象。
2. 对 `VO` 调用 `TransService` 执行翻译。
3. 递归遍历字段并继续调用 `transOne`。
4. 可选创建代理 VO，实现翻译字段平铺。

已确认的主要风险集中在共享链路：

- `transBatch` 对集合多次调用 `iterator().next()`，存在重复首元素读取。
- 集合首元素为 `null` 时，当前逻辑会直接返回整个集合，后续有效元素不会被处理。
- 已翻译对象判重使用 `ArrayList<Object>` 加引用比较，深层对象图下是线性扫描。
- 代理 VO 依赖 `VO.getTransMap()`、ByteBuddy 动态类和反射赋值，风险较高，应只补兜底，不做重构。
- 反射异常当前会记录并将类加入跳过表，该兼容行为应保留，不能改成对外抛出。

## 采用方案

采用“共享链路最小修复”方案：优先修复 `MateEasyTransUtils` 中所有调用方都会经过的根因路径，不在 Boolean、Enum、Dictionary、RPC 翻译服务中分别打补丁。

### 1. 批量集合翻译

在 `transBatch` 内只获取一次首个非空元素，用局部变量复用判断结果：

- `null` 集合仍返回 `null`。
- 空集合仍返回原集合。
- 集合中允许前置 `null` 元素；应跳过 `null`，继续处理后续有效元素。
- 如果不存在非空元素，返回原集合。
- 如果首个非空元素是 `VO`，按现有批量 `transMore(new ArrayList(param), ...)` 路径处理，并继续递归字段。
- 如果首个非空元素不是 `VO`，逐个处理非空元素，保持当前返回集合语义。

这样同时覆盖稳定性和性能目标：避免首元素为 `null` 时整批跳过，也避免重复读取首元素。

### 2. 递归判重

保持现有 public 方法签名中 `ArrayList<Object> hasTransObjs` 不变，避免破坏外部调用兼容。内部增加最小适配层：

- 对外入口仍接收 `ArrayList<Object>`。
- 内部转换为按对象身份判重的集合，例如 `Collections.newSetFromMap(new IdentityHashMap<>())`。
- 判重语义仍是引用身份，不使用 `equals`。
- 对已有传入列表中的对象先导入身份集合，保证历史调用方式仍可工作。
- 新增私有重载或私有上下文方法承载身份集合，避免把新类型暴露为公开 API。

该改动将递归判重从线性 `contains` 降低为身份集合查找，同时不改变对外方法签名。

### 3. 翻译失败与反射兜底

保持现有尽力而为语义：

- `ResponseBodyAdvice` 和 AOP 层捕获异常后返回原对象的行为不变。
- `transFields` 中反射异常记录日志并加入 `notTransClassNamesMap` 的行为不变。
- 字段级普通异常继续记录日志，不中断整个对象返回。
- 不把原本宽容的翻译失败改成对外抛异常。

如需要补强，只在共享工具方法内增加局部 null guard 或异常保护，不扩大到各翻译服务。

### 4. 代理 VO 创建

`createProxyVo` 继续保持“失败返回原 VO”的兼容行为：

- 不重构 ByteBuddy 动态类生成。
- 不新增代理缓存层。
- 仅在必要时补充空值兜底，确保 `vo == null`、`getTransMap() == null` 或代理生成失败时返回原对象。

## 数据流

```text
Controller/AOP/ResponseBodyAdvice
        │
        ▼
MateEasyTransUtils.transOne
        │
        ├─ null / java.* ───────────────▶ 原样返回
        │
        ├─ Map ─────────────────────────▶ 递归 value，失败跳过单项
        │
        ├─ Collection ──────────────────▶ transBatch
        │                                   ├─ 找首个非空元素
        │                                   ├─ VO：transMore + transFields
        │                                   └─ 非 VO：逐项 transOne
        │
        ├─ VO ──────────────────────────▶ transService.transOne + transFields
        │                                   └─ 可选 createProxyVo
        │
        └─ 普通对象 ─────────────────────▶ transFields
```

## 兼容性要求

实现时必须保持：

- `MateEasyTransUtils.transOne`、`transBatch`、`transFields` 等现有 public/static 方法签名兼容。
- `@Trans`、`@TransMethodResult`、`@IgnoreTrans`、`@TransSett` 注解语义不变。
- Boolean、Enum、Dictionary、RPC 翻译输出字段名和结构不变。
- 代理平铺开启时的返回结构不变；代理失败仍返回原对象。
- 下游模块不需要因为本次优化修改源码。

## 风险与控制

| 风险 | 控制方式 |
| --- | --- |
| 集合首个非空元素判断改变处理范围 | 用测试覆盖“首元素 null，后续 VO 有效”场景，确认后续元素被翻译 |
| 身份判重内部结构变化影响递归行为 | 保持引用身份语义，保留 public 参数签名，用循环引用测试验证不重复递归 |
| ByteBuddy 代理生成路径脆弱 | 不重构，仅保留失败返回原 VO 的兜底 |
| 翻译服务输出被误改 | 不在 Boolean/Enum/Dictionary/RPC 服务做非必要改动，并用兼容测试锁定输出 |
| 反射异常处理变严导致接口失败 | 保持记录日志并返回原对象/原字段的宽容行为 |

## 测试策略

优先在 `fw-plus-easytrans-service` 增加最小单元测试，不启动完整 Spring 容器。测试对象使用简单 VO、普通对象和 stub `TransService`。

必测场景：

1. `transOne(null, ...)` 返回 `null`，不抛异常。
2. `transBatch` 处理空集合返回原集合。
3. `transBatch` 处理首元素为 `null`、后续元素为可翻译 VO 的集合时，后续元素仍被翻译。
4. 重复对象或循环引用不会导致重复递归或栈溢出。
5. `createProxyVo` 代理失败时返回原 VO。
6. Boolean、Enum、Dictionary、RPC 至少通过现有行为或最小验证确认输出兼容。

验证命令优先使用 easytrans 模块范围：

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service
cd fw-plus-core && ./mvnw compile -pl fw-plus-project/fw-plus-easytrans -DskipTests
```

如模块依赖要求完整 reactor，再按 Maven 提示补充 `-am`，但不扩大源码修改范围。

## 实施顺序

1. 先为 `MateEasyTransUtils` 当前缺陷补最小失败用例。
2. 修改 `transBatch`，复用首个非空元素并跳过集合内 `null` 项。
3. 增加内部身份判重适配，保留公开签名。
4. 补必要的 null/代理兜底，不重构代理生成。
5. 运行 easytrans 测试和模块编译。

## Spec Patch

无。当前 OpenSpec delta spec 已覆盖兼容、失败兜底、重复工作和最小验证要求。
