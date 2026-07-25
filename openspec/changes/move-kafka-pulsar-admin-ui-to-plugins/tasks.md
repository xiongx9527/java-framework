## 1. Kafka 插件 UI 迁移

- [x] 1.1 将 Kafka 管理页面、业务文案和运行时依赖适配迁入 Kafka 插件 console，通过 `adminOnly` 管理 toolbar 注册入口，并增加插件边界契约测试与前端构建验证。

## 2. Pulsar 插件 UI 迁移

- [ ] 2.1 将 Pulsar 管理页面、业务文案和运行时依赖适配迁入 Pulsar 插件 console，通过 `adminOnly` 管理 toolbar 注册入口，并增加插件边界契约测试与前端构建验证。

## 3. 宿主 UI 解耦

- [ ] 3.1 删除宿主中的 Kafka/Pulsar 管理页面、业务文案、专属工具栏和状态栏判断，保留通用插件 toolbar 追加机制并增加宿主边界契约验证。

## 4. 集成验证

- [ ] 4.1 集成三组分区改动，运行 Kafka/Pulsar 插件测试与构建、宿主控制台构建、Maven 资源处理、OpenSpec 严格校验及差异检查。
