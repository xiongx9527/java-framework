# fw-plus-magicapi-plugin-redis

Magic-API **Redis 数据源**控制台前端，对应 datasource type `redis-datasource`。

- 后端实现：`fw-plus-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis`。
- 前端产物：`magic-redis.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`，并打进 jar；后端以「插件标识 + 全局名 + 产物名」三元组 `Plugin("redis", "MagicRedis", "magic-redis.1.0.0.iife.js")` 注册（`MagicAPIRedisConfiguration`），控制台据此加载本插件面板，因此使用方必须把本模块声明为依赖（版本由 `fw-plus-boot-dependencies` 纳管），否则控制台请求该产物会 404。
- 表单字段按元数据渲染（`src/console/src/metadata/redis-datasource.js`，字段与后端 `RedisDataSourceInfo` 一致，必填口径与 `BaseService.validateField` 一致），必填字段带 `*` 标记。
- 保存前校验：控制台 `doSave` 调用插件 service 的可选 `validate` 钩子——必填为空或 `properties` 不是合法 JSON 对象时弹窗提示并阻止保存请求；`properties` 以 JSON 文本编辑，保存时解析写回 `properties`。
- 连接测试：`POST /redis/datasource/test`。

```bash
npm install && npm run build   # dist/magic-redis.1.0.0.iife.js
npm test                       # 元数据与后端口径交叉校验
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。
