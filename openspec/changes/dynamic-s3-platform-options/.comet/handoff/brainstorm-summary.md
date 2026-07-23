# Brainstorm Summary

- Change: dynamic-s3-platform-options
- Date: 2026-07-23

## 确认的技术方案

采用“后端 Provider + 登录接口 + 前端运行时加载/兜底”的方案。Provider 以 `MagicDynamicS3DataSource` 当前明确支持的平台分支为支持表，平台候选绑定 `SpringFileStorageProperties` 对应配置类，并按 classpath 过滤。S3 Controller 新增登录后访问的候选接口，前端组件初始化后加载动态候选，失败或返回空时保留默认候选。

## 关键取舍与风险

- 以 `amazon-s3` 作为动态候选中的真实后端平台值；历史 `aws-s3` 仅在编辑已有 datasource 时保留当前值，不作为默认动态候选。
- Controller 当前由 `MagicAPIS3Configuration#controllerRegister()` 手动注册，推荐让配置类构造 Controller 时传入 Provider，提高可测试性。
- 只展示后端已有构建分支的平台，不展示 `SpringFileStorageProperties` 中存在但 `MagicDynamicS3DataSource` 未构建的平台。

## 测试策略

- Provider 单元测试覆盖默认候选、缺失类过滤、不可构建平台过滤、配置追加候选过滤。
- Controller 测试覆盖路由、返回类型、`@Valid(requireLogin = true)` 和无 VIEW 权限拒绝。
- 前端做最小构建或 smoke，确认动态加载成功、接口失败兜底、当前值不在候选时仍展示。

## Spec Patch

无。
