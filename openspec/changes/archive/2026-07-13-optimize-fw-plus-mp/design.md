## Context

`fw-plus-mp` 为框架提供 MyBatis-Plus 基础 mapper/entity、数据变更追踪、数据权限、字段加密和 MP Join 增强。当前可从轻量 tweak 收敛的优化点主要集中在：配置默认值是否准确、工具方法格式化兜底是否稳定、核心路径是否具备最小测试。

约束：
- 仅修改 `fw-plus-core/fw-plus-project/fw-plus-mp` 下模块。
- 保持公开 API、配置 key、注解语义和返回结构兼容。
- 不修改 Boot 自动配置、sample 或 product。
- 不新增运行时依赖，不引入新架构。

## Goals / Non-Goals

**Goals:**
- 修复 `fw-plus-mp` 内部可确认的配置默认值、空值兜底或格式化边界问题。
- 为改动路径补最小测试或可运行验证。
- 保持下游模块无需修改源码即可继续编译。

**Non-Goals:**
- 不重写 MyBatis-Plus 增强架构。
- 不改变 MP Join、字段加密、数据权限等公开使用方式。
- 不做跨 Boot 自动配置的大规模协调改造。

## Implementation Notes

- 优先从 `fw-plus-mp-datachange` 中的 `DataChangeProperties` 默认方法集合和 `DataChangeHelper.formatValue` 兜底路径开始，处理低风险、可测试问题。
- 若发现字段命名拼写已被外部配置绑定依赖，不直接删除或重命名；通过兼容 getter/setter、别名或内部默认值修正方式保持兼容。
- 测试尽量使用纯单元测试，不启动数据库或完整 Spring 容器。
- 若某个优化点触及 public API 或跨模块 Boot 配置协调，应停止并触发 tweak 升级判定。

## Validation

优先运行：

```bash
cd fw-plus-core && mvn test -pl fw-plus-project/fw-plus-mp/fw-plus-mp-datachange -Dtest=DataChangeHelperTest,DataChangePropertiesTest
cd fw-plus-core && mvn compile -pl fw-plus-project/fw-plus-mp -am -DskipTests
```

如具体测试类名称按实现调整，以实际新增测试为准；如 Maven 提示依赖缺失，补充 `-am`。
