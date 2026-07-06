# 修复 Magic WS 脚本错误响应与协议测试缺口

## 问题描述

Magic WS 插件已经完成标准化和控制台查询修复，但审查发现两个遗留缺口：

1. 业务消息匹配到 `ws-subscribe` 后，如果脚本执行抛出异常，当前 handler 只记录日志，不向客户端返回结构化错误消息。
2. WS 协议层测试主要覆盖上下文变量和控制台接口，缺少对文本 ping、非法 JSON、缺失 type、未知 type、Binary 关闭等关键协议行为的回归测试。

## 根因分析

`BaseDynamicWebSocketHandler#handleSubscribeMessage` 在 catch 分支中仅 `log.error`，没有复用已有 `sendErrorMessage` 结构化错误响应；同时此前测试重点放在控制台 `/ws/execute` 与上下文兼容变量，没有直接驱动 `handleMessage` 的协议分支。

## 修复目标

- 脚本执行失败时向客户端返回结构化 `SCRIPT_ERROR`，保持连接可用。
- 增加 WS handler 协议行为回归测试，覆盖可恢复业务错误和不支持数据类型关闭。
- 更新主规格 Purpose 占位文案，使归档后的规格更完整。
