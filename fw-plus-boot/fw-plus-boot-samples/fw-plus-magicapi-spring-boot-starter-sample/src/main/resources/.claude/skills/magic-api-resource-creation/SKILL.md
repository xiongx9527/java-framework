---
name: magic-api-resource-creation
description: Magic-API 资源文件与目录创建向导，覆盖 group.json 结构、.ms 文件 JSON 头部模板、命名约定、导入路径解析。适用于在 resources/ 仓库中创建 API、函数、任务、消息消费者等各类资源文件。
origin: fw-plus
reference: resources/CLAUDE.md, magic-api skill
---

# Magic-API 资源文件与目录创建

使用此 skill 在 `resources/` 仓库中创建新的资源文件和目录结构。本 skill 解决"该在哪个目录创建什么文件、JSON 头部填什么字段"的问题。

> **配套阅读**：
> - `CLAUDE.md` — 所有资源类型的完整参考手册
> - `magic-api` skill — magic-script 脚本语法和 API 参考（SQL、HTTP、LINQ、Java 互操作等）

## 何时使用

- 在 `resources/` 中创建新的 **API 接口**（REST 端点）
- 创建新的 **可复用函数**（供其他脚本通过 `import` 调用）
- 创建新的 **定时任务**、**Kafka/Pulsar 消费者**
- 搭建新的 **分组目录结构**（`group.json` + 子目录）
- 不确定某种资源类型的 JSON 头部需要哪些字段

## 核心概念

### .ms 文件格式

所有 `.ms` 文件由 **JSON 头部** + `================================`（单独一行）+ **脚本代码** 组成：

```magicscript
{
  "properties": {},
  "id": "<UUID>",
  "script": null,
  "groupId": "<所属分组的 UUID>",
  "name": "<中文显示名称>",
  "createTime": <毫秒时间戳>,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  // ... 各资源类型特有字段
}
================================
// 分隔符下方是脚本代码，语言因资源类型而异
```

**通用字段说明**（所有 `.ms` 类型共有）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `properties` | `{}` | 始终为空对象 |
| `id` | UUID 字符串 | 32 位十六进制，无连字符，资源唯一标识 |
| `script` | `null` | 始终为 null — 脚本内容在分隔符之后 |
| `groupId` | UUID 字符串 | 所属分组的 id，必须等于对应 `group.json` 的 `id` |
| `name` | 字符串 | UI 中显示的资源名（中文） |
| `createTime` | 长整型 | 创建时间，毫秒时间戳 |
| `updateTime` | 长整型/null | 最后更新时间 |
| `lock` | null | 锁定标记 |
| `createBy` | 字符串/null | 创建者 |
| `updateBy` | 字符串/null | 最后更新者 |

### group.json 结构

每个子目录下都有一个 `group.json`，定义资源分组并形成树形结构：

```json
{
  "properties": {},
  "id": "<UUID>",
  "name": "<显示名称，中文>",
  "type": "api | function | task | kafka-subscribe | pulsar-subscribe | config | freemarker",
  "parentId": "<父分组 UUID，顶级分组用 \"0\">",
  "path": "<英文短标识符，构建 import 路径的基础>",
  "paths": [],
  "options": []
}
```

关键规则：
- `parentId = "0"` 表示顶级分组；否则必须是一个已存在父分组的 `id`
- `type` 必须与所在目录的资源类型匹配
- `path` 的值会**逐级拼接**，构成 import 路径。见下文「导入路径解析」

### 命名约定（关键！）

理解三层命名的差异至关重要。以 `function/服务/公共/回调.ms` 为例：

| 层面 | 值 | 作用 |
|------|-----|------|
| 磁盘目录名 | `服务/`、`公共/` | 仅文件系统层面 |
| 磁盘文件名 | `回调.ms` | 仅文件系统层面，**不影响 import** |
| group.json `name` | `"服务"` | UI 显示（中文） |
| group.json `path` | `"services"` | **import 路径段**（英文短标识符） |
| 子 group.json `path` | `"common"` | 同上，逐级拼接 |
| .ms `name` | `"回调"` | UI 显示（中文） |
| function `path` | `"callback"` | **import 时使用的函数名**（英文 camelCase） |

**命名规范**：
- 目录名 / `name` 字段 → 中文，用于 UI 显示
- group.json `path` → 英文短标识符，全小写（如 `services`、`public`、`evaluation`、`iot`）
- function `path` → 英文 camelCase（如 `getEnv`、`getJyqToken`、`workOrderAdd`）
- .ms 文件名 → 中文描述，仅影响文件系统可读性

### 导入路径解析

import 路径由 group.json 的 `path` 字段**逐级拼接**，最后接上函数的 `path` 字段，与 `.ms` 文件名无关。

```
公式：@/<根group.path>/<子group.path>/.../<函数.path>
```

实际示例：

| import 语句 | 解析到的文件 | path 链 |
|-------------|-------------|---------|
| `import '@/public/getEnv'` | `function/公共/获取环境信息.ms` | root `public` → func `getEnv` |
| `import '@/services/common/callback'` | `function/服务/公共/回调.ms` | `services` → `common` → func `callback` |
| `import '@/iot/getJyqToken'` | `function/物联网/获取jyqopenapi的token.ms` | root `iot` → func `getJyqToken` |
| `import '@/configure/getBykey'` | `function/配置中心/根据key获取数据值.ms` | root `configure` → func `getBykey` |

> **注意**：import 路径必须以 `@/` 开头，与 `.ms` 文件名无关。

---

## 创建工作流

### 步骤 1：创建顶级分组

```bash
mkdir -p <资源类型>/<分组目录名>/
```

在新建目录下创建 `group.json`（`parentId: "0"`）：

```json
{
  "properties": {},
  "id": "<生成新的UUID>",
  "name": "<分组中文名>",
  "type": "<资源类型>",
  "parentId": "0",
  "path": "<英文短标识符>",
  "paths": [],
  "options": []
}
```

示例 — `function/通知/group.json`：

```json
{
  "properties": {},
  "id": "a1b2c3d4e5f67890abcdef1234567890",
  "name": "通知",
  "type": "function",
  "parentId": "0",
  "path": "notify",
  "paths": [],
  "options": []
}
```

### 步骤 2：创建嵌套子分组（可选）

子分组的 `parentId` 必须是父分组 `group.json` 的 `id`。此时 import 路径变为 `@/<父path>/<子path>/<函数path>`。

### 步骤 3：在分组下创建 .ms 文件

根据资源类型选择对应的 JSON 头部模板（见下节），填入正确的 `groupId`（即所在分组的 UUID），编写脚本代码。

---

## JSON 头部模板

以下每种资源类型提供可直接复制的完整模板。将 `<...>` 占位符替换为实际值。

### API 接口（`api/*.ms`）

```json
{
  "properties": {},
  "id": "<UUID>",
  "script": null,
  "groupId": "<所属 API 分组的 UUID>",
  "name": "<中文名称>",
  "createTime": <当前毫秒时间戳>,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  "path": "<URL路径，以 / 开头，如 /save>",
  "method": "<POST | GET>",
  "parameters": [],
  "options": [
    {
      "name": "wrap_request_parameter",
      "value": "<包装变量名，如 data>",
      "description": null,
      "required": false,
      "dataType": "String",
      "type": null,
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    },
    {
      "name": "permission",
      "value": "<权限标识，如 data:test:save>",
      "description": "允许拥有该权限的访问",
      "required": false,
      "dataType": "String",
      "type": null,
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    }
  ],
  "requestBody": null,
  "headers": [],
  "paths": [],
  "responseBody": null,
  "description": null,
  "requestBodyDefinition": null,
  "responseBodyDefinition": null
}
================================
// 脚本语言：magic-script
// 内置变量：data（由 wrap_request_parameter 包装）、request、response、db、log
```

关键字段：
- **`path`**：URL 路径，**以 `/` 开头**
- **`method`**：项目几乎只用 `POST`
- **`options`**：`wrap_request_parameter` 将请求体包装为指定变量名，脚本中直接用该变量名访问参数；`permission` 为权限标识
- **`groupId`**：必须等于所属分组 `group.json` 的 `id`，不能填 `"0"`

### Function（`function/*.ms`）

```json
{
  "properties": {},
  "id": "<UUID>",
  "script": null,
  "groupId": "<所属 function 分组的 UUID>",
  "name": "<中文名称>",
  "createTime": <当前毫秒时间戳>,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  "path": "<函数名，英文 camelCase，不要以 / 开头>",
  "description": null,
  "returnType": null,
  "mappingPath": null,
  "parameters": []
}
================================
// 脚本语言：magic-script
// 通过 import '@/.../<path>' 调用此函数
```

关键字段：
- **`path`**：函数名，**不要以 `/` 开头**。这是 import 路径的最后一段
- **`parameters`**：参数定义数组，其中 `type` 为完全限定 Java 类名（如 `"java.lang.String"`、`"java.util.Collection"`）
- **`mappingPath`**：通常为 `null`

### Task（`task/*.ms`）

```json
{
  "properties": {},
  "id": "<UUID>",
  "script": null,
  "groupId": "<所属 task 分组的 UUID>",
  "name": "<中文名称>",
  "createTime": <当前毫秒时间戳>,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  "path": "<任务标识，英文短名称>",
  "cron": "<Cron 表达式，7段：秒 分 时 日 月 周 [年]>",
  "enabled": false,
  "description": "<任务描述>"
}
================================
// 脚本语言：magic-script
```

关键字段：
- **`cron`**：7 段格式，如 `"0 30 4 * * ?"` = 每天凌晨 4:30
- **`enabled`**：上线前务必设为 `false`
- **`path`**：任务标识符，非 URL 路径

### Kafka Subscribe（`kafka-subscribe/*.ms`）

```json
{
  "properties": {},
  "id": "<UUID>",
  "script": null,
  "groupId": "<所属分组的 UUID>",
  "name": "<中文名称>",
  "createTime": <当前毫秒时间戳>,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  "path": null,
  "key": "<数据源 key>",
  "topic": "<Topic 名称>",
  "broadcast": false,
  "group": "<消费组 ID，如 magic-api>",
  "partition": null,
  "enabled": true,
  "ackMode": "BATCH",
  "ackCount": 100,
  "ackTime": 5000,
  "concurrency": null,
  "other": null,
  "params": null
}
================================
import log;
// 脚本语言：magic-script
// 消息获取：msg.value()
// 正常处理返回 null；返回非 null 表示消费失败
```

关键字段：
- **`path`**：始终为 `null`
- **`key`**：指向对应数据源的 `key`
- **`ackMode`**：`"BATCH"` / `"MANUAL"` / `"MANUAL_IMMEDIATE"`

### Pulsar Subscribe（`pulsar-subscribe/*.ms`）

```json
{
  "properties": {},
  "id": "<UUID>",
  "script": null,
  "groupId": "<所属分组的 UUID>",
  "name": "<中文名称>",
  "createTime": <当前毫秒时间戳>,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  "path": null,
  "key": "<数据源 key>",
  "topic": "<Topic 名称>",
  "broadcast": false,
  "group": "<消费组 ID，如 magic-api>",
  "partition": null,
  "enabled": false,
  "batchIndexAcknowledgmentEnabled": true,
  "other": null,
  "params": null
}
================================
import log;
// 脚本语言：magic-script
// 消息获取：new String(msg.getValue()) — 注意与 Kafka 的区别！
// 正常处理返回 null；返回非 null 表示消费失败
```

关键字段：
- **`path`**：始终为 `null`
- 消息获取：`new String(msg.getValue())` — **不是 `msg.value()`**

### Config（`config/*.ms`）

```json
{
  "properties": {},
  "id": "<UUID>",
  "script": null,
  "groupId": "<所属分组的 UUID>",
  "name": "<中文名称>",
  "createTime": <当前毫秒时间戳>,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  "path": null,
  "enabled": true,
  "appId": "<归属应用 ID，如 report-api>",
  "namespace": "<命名空间，如 db、redis、pulsar、s3>",
  "env": "<环境名，如 dev>",
  "externalFlag": false,
  "commonFlag": false,
  "description": null
}
================================
# 脚本语言：YAML（不是 magic-script！）
magic:
  api:
    <type>:
      <key>:
        <field>:
          <profile1>: <value1>
          <profile2>: <value2>
```

关键字段：
- **分隔符下方是 YAML**，不是 magic-script。按 `magic.api.{类型}.{key}.{字段}` 层级组织
- **`namespace`**：资源类型标识

### Freemarker（`freemarker/*.ms`）

```json
{
  "properties": {},
  "id": "<UUID>",
  "script": null,
  "groupId": "<所属分组的 UUID>",
  "name": "<中文名称>",
  "createTime": <当前毫秒时间戳>,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  "path": "<模板标识符>",
  "description": null,
  "params": null
}
================================
<#-- 脚本语言：FreeMarker FTL 模板语法 -->
<#-- ${variable} 访问变量，<#if> 条件判断，<#list> 遍历 -->
```

关键字段：
- 分隔符下方是 **FreeMarker FTL 语法**，不是 magic-script

---

## 脚本编写快速指引

> 完整语法和 API 请参考 `magic-api` skill。以下仅列出每种资源类型的脚本结构骨架。

### API 脚本标准结构

```magicscript
import '@/notify/sendNotification' as sendNotification;

// 1. 参数校验
if (!data.name || data.name == '') {
    exit 400, '名称不能为空';
}

// 2. 业务逻辑
var result = sendNotification(data);

// 3. 返回结果
return result;
```

### Function 脚本标准结构

```magicscript
import '@/public/getEnv' as getEnv;
import log;

// 业务逻辑
return processData(arg1, arg2);
```

### Task 脚本标准结构

```magicscript
import '@/xxx/syncData' as syncData;
import log;

log.info("定时任务开始执行");
var dataList = db['datasourceKey'].select('select * from t ...');
for (item in dataList) {
    syncData(item);
}
log.info("定时任务执行完成");
```

### 消息消费脚本标准结构

Kafka：
```magicscript
import log;
try {
    log.info("收到消息:{}", msg.value());
    // 处理逻辑
    return null;  // null = 消费成功
} catch(e) {
    log.error("处理失败: {}", e.getMessage());
    return '处理失败';  // 非 null = 消费失败
}
```

Pulsar：
```magicscript
import log;
var msgStr = new String(msg.getValue());  // 与 Kafka 不同！
log.info("收到Pulsar消息:{}", msgStr);
// 处理逻辑
return null;
```

### 常用 import 模式

```magicscript
import log;                                    // 内置模块
import '@/public/getEnv' as getEnv;             // 自定义函数
import '@/services/common/callback' as cb;     // 嵌套路径函数
import com.alibaba.fastjson.JSON;               // Java 类
import 'java.util.UUID' as UUID;               // Java 类（别名）
```

### 环境隔离模式（项目标准做法）

```magicscript
import '@/public/getEnv' as getEnv;
var env = getEnv();  // 生产返回 ""，UAT 返回 "_uat"
var list = db['services' + env].select('select * from user');
```

---

## 常见错误

### JSON 头部

| 错误 | 正确做法 | 原因 |
|------|---------|------|
| `groupId` 填 `"0"` | 填所在分组 `group.json` 的实际 UUID | 引擎需要 UUID 定位分组 |
| `script: ""` 或缺失 | 始终设为 `null` | 引擎期望该字段为 null |
| function `path: "/getEnv"` | `path: "getEnv"`（不以 `/` 开头） | `/` 开头是 API URL 语义 |
| API `path: "save"` | `path: "/save"`（以 `/` 开头） | API 路径应以 `/` 开头 |
| task `cron` 用 6 段 | 用 7 段（秒 分 时 日 月 周 [年]） | Magic-API 使用 7 段格式 |
| 新目录忘记 `group.json` | 始终创建包含正确 `type` 的 group.json | 引擎依赖 group.json 注册资源 |
| subscribe `path` 不为 null | `path` 始终为 `null` | subscribe 类型不使用 path |

### 导入路径

| 错误 | 正确做法 | 原因 |
|------|---------|------|
| `import '@/public/获取环境信息'` | `import '@/public/getEnv'` | 路径基于 `path` 字段，不是文件名 |
| `import 'services/common/callback'` | `import '@/services/common/callback'` | 必须以 `@/` 开头 |
| group path 与 function path 拼写不一致 | 确保 path 链逐级匹配 | 导入解析依赖 path 字段拼接 |

### 脚本语言

| 错误 | 正确做法 | 原因 |
|------|---------|------|
| config 分隔符下方写 magic-script | 写 YAML | config 的脚本引擎是 YAML |
| freemarker 分隔符下方写 magic-script | 写 FreeMarker FTL 语法 | freemarker 渲染引擎是 FreeMarker |
| Pulsar 消费者用 `msg.value()` | `new String(msg.getValue())` | Pulsar 消息体是 byte[] |

---

## 完整示例：从零创建 API 接口

创建 `POST /notification/send` 接口，调用函数发送通知。

### 目标结构

```
api/通知/
├── group.json
└── 发送通知.ms

function/通知/
├── group.json
└── 发送通知消息.ms
```

### 步骤 1：创建 API 分组

`api/通知/group.json`：

```json
{
  "properties": {},
  "id": "a1b2c3d4e5f61111abcdef1234567890",
  "name": "通知",
  "type": "api",
  "parentId": "0",
  "path": "notification",
  "paths": [],
  "options": []
}
```

### 步骤 2：创建函数分组

`function/通知/group.json`：

```json
{
  "properties": {},
  "id": "b2c3d4e5f6a72222bcdef1234567890a",
  "name": "通知",
  "type": "function",
  "parentId": "0",
  "path": "notify",
  "paths": [],
  "options": []
}
```

### 步骤 3：创建函数

`function/通知/发送通知消息.ms`：

```json
{
  "properties": {},
  "id": "c3d4e5f6a7b83333cdef1234567890ab",
  "script": null,
  "groupId": "b2c3d4e5f6a72222bcdef1234567890a",
  "name": "发送通知消息",
  "createTime": 1750000000000,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  "path": "sendNotification",
  "description": "向指定用户发送通知消息",
  "returnType": null,
  "mappingPath": null,
  "parameters": [
    {
      "name": "userId",
      "value": "userId",
      "description": "用户ID",
      "required": true,
      "dataType": "String",
      "type": "java.lang.String",
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    },
    {
      "name": "message",
      "value": "message",
      "description": "消息内容",
      "required": true,
      "dataType": "String",
      "type": "java.lang.String",
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    }
  ]
}
================================
import log;
import com.alibaba.fastjson.JSON;

log.info("发送通知: userId={}, message={}", userId, message);

var insertResult = db.table('t_notification').primary('id', () => uuid()).insert({
    userId: userId,
    message: message,
    status: 0,
    createTime: new java.util.Date()
});

log.info("通知发送成功: {}", JSON.toJSONString(insertResult));
return {success: true, notificationId: insertResult};
```

### 步骤 4：创建 API

`api/通知/发送通知.ms`：

```json
{
  "properties": {},
  "id": "d4e5f6a7b8c94444def1234567890abc",
  "script": null,
  "groupId": "a1b2c3d4e5f61111abcdef1234567890",
  "name": "发送通知",
  "createTime": 1750000000000,
  "updateTime": null,
  "lock": null,
  "createBy": null,
  "updateBy": null,
  "path": "/send",
  "method": "POST",
  "parameters": [],
  "options": [
    {
      "name": "wrap_request_parameter",
      "value": "data",
      "description": null,
      "required": false,
      "dataType": "String",
      "type": null,
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    },
    {
      "name": "permission",
      "value": "notification:send",
      "description": "允许拥有该权限的访问",
      "required": false,
      "dataType": "String",
      "type": null,
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    }
  ],
  "requestBody": null,
  "headers": [],
  "paths": [],
  "responseBody": null,
  "description": null,
  "requestBodyDefinition": null,
  "responseBodyDefinition": null
}
================================
import '@/notify/sendNotification' as sendNotification;

if (!data.userId || data.userId == '') {
    exit 400, '用户ID不能为空';
}
if (!data.message || data.message == '') {
    exit 400, '消息内容不能为空';
}

return sendNotification(data.userId, data.message);
```

### 验证清单

- [ ] 所有 `group.json` 的 UUID 唯一？
- [ ] `.ms` 文件的 `groupId` 指向所在分组 UUID？
- [ ] function `path` 与 import 路径匹配？（`sendNotification` ↔ `@/notify/sendNotification`）
- [ ] API `options` 中 `wrap_request_parameter` 的值与脚本变量名一致？（`"data"` ↔ `data.xxx`）
- [ ] API `path` 以 `/` 开头？function `path` 不以 `/` 开头？
- [ ] config / freemarker 的脚本语言正确？
- [ ] task `enabled` = `false`？
- [ ] subscribe `path` = `null`？
- [ ] 脚本以 `return` 或 `exit` 结束？

---

## 参考

- **`CLAUDE.md`** — 所有资源类型目录的完整表格和字段列表
- **`magic-api` skill** — magic-script 完整语法和 API 参考
- **上游 prompt 文件** — `/Users/xiongxiao/Workspaces/Idea/codes/JuGui/magic-api/magic-api-plugins/magic-api-plugin-ai/src/main/resources/prompt/`
