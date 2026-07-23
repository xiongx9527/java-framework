## 1. 后端连接器选项

- [x] 1.1 新增 Debezium 连接器下拉选项的数据结构，使用 `text` 和 `value` 字段承载前端选择项。
- [x] 1.2 在后端集中维护默认 Debezium connector class 候选列表，内容与当前控制台已有选项保持一致。
- [x] 1.3 返回默认候选项前按运行时 classpath 过滤不可加载的 connector class，并保持兼容 JDK 1.8。
- [x] 1.4 在 Debezium 控制器中新增只读接口，返回当前可用的连接器选项。

## 2. 控制台服务接入

- [x] 2.1 在 Debezium 控制台服务封装中新增加载连接器选项的方法。
- [x] 2.2 保持服务返回结构与现有 `magic-select` 的选项对象格式兼容。

## 3. Vue 表单接入

- [x] 3.1 将 `magic-debezium-datasource-info.vue` 中硬编码的 `connectorOptions` 数组替换为从服务加载的响应式选项。
- [x] 3.2 当后端返回列表不包含当前 `info.connectorClass` 时，将当前值补充到本次表单的本地选项中。
- [x] 3.3 当连接器选项加载失败时，不清空、不阻断已有数据源表单中的当前连接器值。

## 4. 验证

- [x] 4.1 使用对应模块的 Maven Wrapper 验证受影响的 Debezium 后端模块可以编译通过。
- [x] 4.2 执行 Debezium 控制台插件可用的前端构建或静态检查命令。
- [x] 4.3 确认 `dynamic-debezium-connector-options` 的 OpenSpec 校验通过。

验证备注：`fw-plus-boot/./mvnw` 因缺少 `.mvn/wrapper/maven-wrapper.properties` 和 `.mvn/wrapper/maven-wrapper.jar` 无法启动；已改用系统 Maven 3.6.3 完成同等后端定向测试和 Debezium 插件模块构建。
