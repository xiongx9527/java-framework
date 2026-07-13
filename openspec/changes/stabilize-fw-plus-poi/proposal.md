## Why

`fw-plus-poi` 当前对 EasyPoi 的导入、普通导出、大批量导出和模板导出做了 FwPlus 扩展，但稳定性边界缺少明确约束和测试覆盖。空参数、普通 EasyPoi 参数、空集合、翻译开关和字段过滤组合如果处理不一致，容易在业务导入导出场景中触发非预期异常或行为漂移。

## What Changes

- 增强 `fw-plus-poi-easypoi` 在导入/导出链路中的防御性处理，优先覆盖空参数、空集合、非 FwPlus 参数和翻译配置边界。
- 保持现有公开 API、依赖版本和默认行为不变，不升级 EasyPoi/POI。
- 为核心稳定性场景补充可执行测试，允许新增必要测试依赖和测试辅助类。
- 若实现过程中发现需要跨模块修改或扩大到 EasyTrans/EasyPoi 基础能力，暂停并重新确认范围。

## Capabilities

### New Capabilities

- `poi-easypoi-stability`: 约束 FwPlus EasyPoi 扩展在导入、普通导出、大批量导出和模板导出边界场景下的稳定性行为。

### Modified Capabilities

- 无。

## Impact

- 主要影响 `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi` 下的 FwPlus EasyPoi 扩展类和测试代码。
- 可能涉及测试 POM 依赖，但不改变生产依赖版本。
- 不改变公开 API、不引入数据库 schema 变更、不触碰 `fw-plus-mp`、`fw-plus-nosql` 或其他当前未提交改动。
