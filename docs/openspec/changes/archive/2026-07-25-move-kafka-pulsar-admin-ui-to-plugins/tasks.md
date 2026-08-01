## 1. Kafka 插件 UI 迁移

- [x] 1.1 将 Kafka 管理页面、业务文案和运行时依赖适配迁入 Kafka 插件 console，通过 `adminOnly` 管理 toolbar 注册入口，并增加插件边界契约测试与前端构建验证。

## 2. Pulsar 插件 UI 迁移

- [x] 2.1 将 Pulsar 管理页面、业务文案和运行时依赖适配迁入 Pulsar 插件 console，通过 `adminOnly` 管理 toolbar 注册入口，并增加插件边界契约测试与前端构建验证。

## 3. 宿主 UI 解耦

- [x] 3.1 删除宿主中的 Kafka/Pulsar 管理页面、业务文案、专属工具栏和状态栏判断，保留通用插件 toolbar 追加机制并增加宿主边界契约验证。

## 4. 集成验证

- [x] 4.1 集成三组分区改动，运行 Kafka/Pulsar 插件测试与构建、宿主控制台构建、Maven 资源处理、OpenSpec 严格校验及差异检查。

## 5. 状态栏入口迁移

- [x] 5.1 在 Kafka、Pulsar 插件中注册 `statusBarItems`，保持原点击行为，并重新执行插件、宿主、Maven 资源与 OpenSpec 验证。
