# Tasks

## 1. 元数据与表单渲染

- [x] 1.1 新增 `src/console/src/metadata/redis-datasource.js`（字段描述符 `{name,type,required}`、敏感字段名单、mode/clientType 枚举），验证：文件存在且字段键与后端 `RedisDataSourceInfo` 及必填集合（name/key/hostname/port）逐一对齐
- [x] 1.2 重构 `magic-redis-datasource-info.vue` 为 `v-for` 按 metadata 渲染（含必填 `*` 标记、敏感字段密码框、select 下拉），验证：`npm run build` 构建成功且打开编辑弹窗字段齐全、顺序与元数据一致
- [x] 1.3 重构 i18n 为 `common/hint/field/extra` 分层结构（zh-cn 与 en 同构），验证：两文件键集合完全一致，切换中英文表单无缺键回退

## 2. 保存前校验拦截

- [x] 2.1 共享控制台 `magic-data-resource.vue` 的 `doSave()` 增加可选 `service.validate` 钩子（抛错→提示→阻断），验证：控制台 `npm run build` 成功，未定义 `validate` 的其它 datasource 插件保存流程与改动前一致
- [x] 2.2 redis service 实现 `validate(info)`（必填非空 + properties 合法 JSON 检查），并清理失效代码（`com.jugui` 的 `setExtensionAttribute`、`nebula-datasource` 笔误），验证：`grep` 不再命中 `com.jugui`/`nebula-datasource`，必填为空或 JSON 非法时点击保存弹提示且网络面板无保存请求

## 3. properties 双向绑定

- [x] 3.1 JSON 编辑框改绑 `info.properties` 的 computed 双向转换（参照 `magic-langchain4j-aj-mcp-client-info.vue` 模板），验证：手工场景——已有 properties 打开回显格式化 JSON、修改后保存重开内容一致、网络请求体包含 `properties` 而非 `propertiesText`

## 4. 测试与文档

- [x] 4.1 新增 `src/console/test/` 交叉校验测试（node --test，解析 `RedisDataSourceInfo.java` 字段与 `BaseService.validateField` 必填集合对比 metadata），并在 `package.json` 加 `test` script，验证：`npm test` 通过；临时改动元数据字段名后 `npm test` 失败并指出漂移字段
- [x] 4.2 新增模块 `README.md`（三元组注册、产物名、metadata 与后端一致性、`npm test` 说明），验证：内容与参照模块 README 结构一致且命令可执行

## 5. 构建与集成验证

- [x] 5.1 重建插件 IIFE 产物与共享控制台产物，验证：`fw-plus-magicapi-plugin-redis` 与 `fw-plus-magicapi-spring-boot-starter` 的 Maven 构建（`./mvnw … -DskipTests`）成功，产物文件名与 `Plugin("redis","MagicRedis","magic-redis.1.0.0.iife.js")` 三元组一致
- [x] 5.2 端到端手测并核对提交范围，验证：打开 redis 弹窗完成「必填拦截→合法保存→重开回显」全流程，再打开至少 1 个其它 datasource 插件确认保存无变化；`git status` 中仅包含本 change 的文件后按 `tweak: <简述变更>` 提交
