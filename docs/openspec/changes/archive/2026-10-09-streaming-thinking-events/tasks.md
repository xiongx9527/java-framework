# Tasks

## 1. 核实 Provider 思考能力

- [x] 1.1 审核全部 18 个流式模型工厂及其锁定版本 LangChain4j builder/响应映射，逐 provider 记录独立 `PartialThinking` 能力、原生配置、默认值和限制；通过源码/字节码核对及可执行适配器测试验证分类，并将结果交付到流式插件 README 能力表 <!-- comet-task:b419703f-6bc1-4bac-8029-051869cb0fb3 -->

## 2. Provider 工厂与控制台配置

- [x] 2.1 为审核确认支持独立 thinking 回调的 provider 增加真实 provider-specific 选项透传，保持未配置时 SDK 默认行为；通过工厂单测验证 options 值、缺省值及 builder 调用 <!-- comet-task:0531124c-6de3-4f06-93b5-d23d6d048ad5 -->
- [x] 2.2 同步更新 streaming-chat-model provider 元数据、中英文 i18n 和配置说明，只呈现审核确认的原生选项；运行既有 Node 元数据一致性测试并验证每个 UI 字段均由对应工厂读取 <!-- comet-task:70bdee51-cba2-4b80-9e13-dd72458b6f22 -->
- [x] 2.3 为不支持独立 thinking 回调的 provider 保持无通用 thinking 开关，在 README 能力表注明依据及限制；通过元数据测试确认未引入猜测式配置项 <!-- comet-task:90db1ab6-0735-4ad7-84e2-19241743bcbf -->

## 3. SSE 桥接与 Ollama 示例

- [x] 3.1 修正 `SimpleAssistant` 两条流式路径，使活跃思考轮在 `done` 之前恰好发送一次 `thinking-done`，并让 DirectTokenStream 转发公开的 `PartialThinking` 回调；以 TokenStream 与 StreamingChatModel 替身覆盖 thinking-only、thinking 转回答/工具、无 thinking 和事件顺序 <!-- comet-task:21e7058b-10e9-4de0-831e-b9d4f8a01033 -->
- [x] 3.2 在 Ollama streaming-chat-model 工厂透传 `think` 与 `return-thinking`，同步配置流式插件控制台字段和选项测试；确认未配置时不覆盖 Ollama SDK 默认值 <!-- comet-task:6a5cda20-6cf7-4e07-84e0-93fedc3414ad -->
- [x] 3.3 在 AI Chat 样例中提供 Ollama 思考参数配置，并仅对流式模型显式启用所需参数；以样例配置测试验证参数绑定和 builder 透传，更新 README 中关于流式模型的说明 <!-- comet-task:18cb7164-b7af-4891-a6bd-403d393a7595 -->

## 4. 集成验收

- [x] 4.1 运行所有变更涉及的工厂、SSE、样例和控制台元数据测试，并验证既有 `thinking`、`thinking-done`、`token`、`tool-call`、`tool-result`、`done` SSE 协议及顺序保持兼容 <!-- comet-task:934ee20a-738b-4a52-b235-88dc6c326b01 -->
