# LangChain4j 模型资源 Magic-API 插件设计

- 日期：2026-09-28
- 状态：已确认待评审
- 作者：xiongxiao（与 Claude Code 协作设计）

## 1. 背景与目标

### 1.1 背景

fw-plus-ai 层已将 LangChain4j 以 magic-api `@MagicModule` 形式接入（`modules/langchain4j/core/Langchain4jModule`），
现有形态是脚本侧 `build*` 编程式工厂 + 内存缓存，模块名暂为占位值 `uid`。模型实例仅能脚本内自建，
缺少可维护、可视化、可热更新的配置通道；同时 langchain4j 生态（20+ 模型供应商、15+ 向量库）
在 magic-api 侧无统一接入面。

core 层 `fw-plus-ai-langchain4j-*`（dev.langchain4j.*.spring 包）反编译重建了各供应商 spring
starter 的 `Properties`/`AutoConfig`，作为各家 builder 参数写法的**参考资料**（本设计零修改）。

### 1.2 目标

新增七类 **datasource 型** magic-api 插件，统一命名带 `langchain4j` 前缀：

| 资源类型（folder = datasource type） | 实例类型 | 脚本取用 |
| --- | --- | --- |
| `langchain4j-chat-model` | `ChatLanguageModel` | `langchain4j.getChatLanguageModel(key)` |
| `langchain4j-streaming-chat-model` | `StreamingChatLanguageModel` | `langchain4j.getStreamingChatLanguageModel(key)` |
| `langchain4j-language-model` | `LanguageModel` | `langchain4j.getLanguageModel(key)` |
| `langchain4j-streaming-language-model` | `StreamingLanguageModel` | `langchain4j.getStreamingLanguageModel(key)` |
| `langchain4j-embedding-model` | `EmbeddingModel` | `langchain4j.getEmbeddingModel(key)` |
| `langchain4j-tokenizer` | `Tokenizer` | `langchain4j.getTokenizer(key)` |
| `langchain4j-embedding-store` | `EmbeddingStore<TextSegment>` | `langchain4j.getEmbeddingStore(key)` |

设计要点：

- **datasource 形态**：每类资源在 magic-api 控制台「数据源」页签注册为一个数据源类型，JSON 定义持久化于
  `{type}/*.json`，保存走 magic-api 通用 `/resource/file/{type}/save` 接口（与官方 `mongo-datasource`、
  `redis-datasource` 同构）。
- **JSON 结构 = 公共字段 + 差异参数对象**：`key/name/provider/enabled`（+ store 的 `embeddingModelKey`）
  为公共字段，提取到实体基类；其余连接与生成参数放入 `options` JSON 对象，
  **不同 provider 的 options 内容天然不一致**（如 qwen 有 `enable-search`，openai 没有）。
- **每类资源有不同 provider 集合**：由该类资源的短名表显式声明；`chat-model` 与 `tokenizer`
  可用的 provider 不同，表外值校验失败。
- **覆盖 langchain4j 全生态**：每家 provider 一个显式构建工厂（参照 core 层 spring `AutoConfig` 的
  builder 链写法），新增供应商只加一个工厂类并在短名表登记。
- **选项读取**：`options` 取值经统一的强类型读取工具（封装风格对齐 `MagicAPIConfigUtils`：
  集中读取、默认值、缺失报错带完整路径），供各工厂 builder 链使用。
- **core 层零修改**：`fw-plus-ai-langchain4j-*` 只做参考，禁止任何改动（含修饰符）；
  供应商模型类直接依赖 langchain4j 官方 jar 使用。
- **注册器持有、getter 只读**：实例由动态注册器按 datasource 生命周期创建/销毁；脚本侧只读获取，
  不提供 `build*` 自建 API。
- **EmbeddingStore 必须搭配 EmbeddingModel**：`embedding-store` 资源必填 `embeddingModelKey`，
  引用一个 `embedding-model` 实例；维度优先取 options 的 `dimension`，缺省取 `embeddingModel.dimension()`。

### 1.3 非目标（YAGNI）

- 不做 AiServices/Assistant 的 datasource 化（`Langchain4jModule.chat(...)` 保留原样，模型入参从 holder 取）。
- 不做 `ContentRetriever`、`RetrievalAugmentor`、`ModerationModel` 的 datasource 化（本次仅七类）。
- 不修改 core 层 `fw-plus-ai-langchain4j-*` 任何代码（含修饰符）。
- 不做敏感参数加密存储（api-key 等与 magic-api 内建 datasource 密码字段行为一致，明文存 JSON）。
- 不做集群同步、不做参数 schema 强校验引擎（options 由前端按 provider 元数据渲染表单约束）。

## 2. 命名规范

所有对外命名统一携带 `langchain4j`：

| 项 | 命名 |
| --- | --- |
| 后端包 | `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.*` |
| 实体 | `Langchain4jChatModelInfo`、`Langchain4jStreamingChatModelInfo`、`Langchain4jLanguageModelInfo`、`Langchain4jStreamingLanguageModelInfo`、`Langchain4jEmbeddingModelInfo`、`Langchain4jTokenizerInfo`、`Langchain4jEmbeddingStoreInfo` |
| 存储 | `Langchain4jChatModelMagicResourceStorage` …（每类一个） |
| 注册器 | `Langchain4jChatModelMagicDynamicRegistry` …（每类一个） |
| 选项读取 | `Langchain4jOptionsReader`（仿 `MagicAPIConfigUtils` 封装风格） |
| 装配 | `Langchain4jDatasourceConfiguration`（实现 `MagicPluginConfiguration`） |
| Controller | `Langchain4jDatasourceController`，路径 `POST /langchain4j/{type}/datasource/test` |
| 前端子工程 | `fw-plus-ai-magicapi-plugin-langchain4j`，产物 `magic-langchain4j.1.0.0.iife.js` |
| 插件标识 | `new Plugin("langchain4j", "MagicLangchain4j", "magic-langchain4j.1.0.0.iife.js")` |
| 脚本模块 | `@MagicModule("langchain4j")`（由占位 `uid` 更名） |

## 3. 总体架构与模块布局

```
fw-plus-ai/fw-plus-ai-project/
├── fw-plus-ai-boot-autoconfigure/                 # 后端 Java（新功能主战场）
│   └── .../magicapi/modules/langchain4j/
│       ├── core/Langchain4jModule.java            # 改造：更名 langchain4j，getter 只读，删 build*
│       ├── model/                                 # 7 实体 + 抽象基类 Langchain4jModelInfo
│       ├── resource/                              # 7 个 MagicResourceStorage
│       ├── registry/                              # 7 个 AbstractMagicDynamicRegistry
│       ├── factory/                               # 构建内核：每家 provider 一个工厂
│       │   ├── ModelFactory.java                  #   SPI：provider() + 每类 build(OptionsReader)
│       │   ├── Langchain4jModelFactoryRegistry.java#  provider 短名 → factory
│       │   ├── openai/Langchain4jOpenAiModelFactory.java
│       │   ├── qwen/…、ollama/…、anthropic/… 等每家一个工厂类
│       │   └── store/（向量库工厂：in-memory/redis/milvus/…）
│       ├── support/                               # Langchain4jModelHolder（key → 实例）
│       └── Langchain4jDatasourceConfiguration     # 统一装配，被 MagicAPIBaseAutoConfiguration @Import
└── fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/
    └── fw-plus-ai-magicapi-plugin-langchain4j/    # 前端 console（pom 仿 mcp 子工程）
        └── src/console/
            ├── src/index.js                       # 注册 datasources: [7 类]
            ├── src/provider-meta.js               # (资源类型 × provider) 的 options 字段描述表
            ├── src/components/magic-langchain4j-*-info.vue   # 7 个组件
            ├── src/service/magic-langchain4j-datasource.js   # test 调用
            └── src/i18n/{zh-cn,en}.js

fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-*/   # 只做参考，零修改
```

## 4. 实体与存储

### 4.1 实体（公共字段提取）

```java
@Data
public abstract class Langchain4jModelInfo extends MagicEntity {
    private String key;                  // 唯一，脚本按 key 取实例；仅 [\w.-]
    private String name;                 // 展示名，缺省 key
    private String provider;             // 短名（命中该类短名表）
    private boolean enabled = true;      // false：销毁实例且不注册
    private Map<String, Object> options; // 差异参数对象：不同 provider 内容不一致
}
```

`Langchain4jEmbeddingStoreInfo` 仅扩展一个公共字段：

```java
private String embeddingModelKey;       // 必填：引用一个 embedding-model 实例
```

### 4.2 存储

每个实体对应一个 `MagicResourceStorage<T>`：

- `folder()` = 对应 datasource type（见 §1.2 表）；`suffix()` = `.json`；
  `requirePath()/requiredScript()/allowRoot()` = false/false/true（与 MCP 存储一致）。
- `read/write` 走 `JsonUtils`；`buildMappingKey` 取 `key-updateTime`（同 MCP）。
- `validate()` 公共逻辑：
  1. `key` 必填、非空、匹配 `^[\w.-]+$`、同类资源内唯一；
  2. `provider` 必填且命中该类短名表（表外值报错并列出可用短名）；
  3. `embedding-store` 额外要求 `embeddingModelKey` 必填；
  4. `options` 可为空 map，但不做 provider 级字段校验（由前端元数据与工厂构建期保证）；
  5. `enabled=false` 允许保存（语义为下线而非删除）。
- JSON 示例（`langchain4j-chat-model/qwen-main.json`，qwen 特有 `enable-search`）：

```json
{
  "key": "qwen-main",
  "name": "通义千问主力对话",
  "provider": "qwen",
  "enabled": true,
  "options": {
    "api-key": "sk-xxx",
    "base-url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
    "model-name": "qwen-plus",
    "temperature": 0.7,
    "top-p": 0.8,
    "top-k": 20,
    "enable-search": false,
    "seed": 1234,
    "repetition-penalty": 1.1,
    "max-tokens": 2048,
    "timeout": "60s",
    "max-retries": 3
  }
}
```

`langchain4j-chat-model/openai-main.json`（openai 的 options 与 qwen 不同，天然不一致）：

```json
{
  "key": "openai-main",
  "provider": "openai",
  "enabled": true,
  "options": {
    "api-key": "sk-xxx",
    "model-name": "gpt-4o",
    "temperature": 0.5,
    "max-tokens": 4096,
    "strict-json-schema": true
  }
}
```

`langchain4j-embedding-store/milvus-main.json`：

```json
{
  "key": "milvus-main",
  "provider": "milvus",
  "enabled": true,
  "embeddingModelKey": "qwen-embed",
  "options": {
    "host": "172.16.1.89",
    "port": 19530,
    "collection-name": "magic_vectors",
    "username": "root",
    "password": "Milvus",
    "consistency-level": "STRONG",
    "auto-flush-on-insert": true
  }
}
```

## 5. 构建内核

### 5.1 选项读取（Langchain4jOptionsReader）

封装风格对齐 `MagicAPIConfigUtils`：集中读取 + 默认值 + 缺失报错带完整路径。

```java
public class Langchain4jOptionsReader {
    private final String type;                  // 资源类型，如 chat-model
    private final String key;                   // datasource key，如 qwen-main
    private final Map<String, Object> options;  // 实体 options

    public String required(String name);                    // 缺失抛中文异常含完整路径
    public String string(String name, String defaultValue);
    public int integer(String name, Integer defaultValue);
    public long longValue(String name, Long defaultValue);
    public double dbl(String name, Double defaultValue);
    public boolean bool(String name, Boolean defaultValue);
    public <E extends Enum<E>> E enumValue(String name, Class<E> type, E defaultValue);
    public Duration duration(String name, Duration defaultValue);   // "60s"/"2m"/"PT1M"/毫秒
    public List<String> stringList(String name, List<String> defaultValue);  // "," 拆分或 JSON 数组
    public boolean has(String name);
    public Set<String> names();                    // 全部已配置键（供错误提示）
}
```

完整路径示例：`langchain4j-chat-model[qwen-main].options.api-key`（异常信息与文档均使用该格式）。

### 5.2 工厂 SPI（参照 core 层 AutoConfig 的 builder 链写法）

```java
public interface ModelFactory {
    /** 该工厂的 provider 短名 */
    String provider();

    default ChatLanguageModel buildChatModel(Langchain4jOptionsReader o) { throw unsupported(); }
    default StreamingChatLanguageModel buildStreamingChatModel(Langchain4jOptionsReader o) { throw unsupported(); }
    default LanguageModel buildLanguageModel(Langchain4jOptionsReader o) { throw unsupported(); }
    default StreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) { throw unsupported(); }
    default EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) { throw unsupported(); }
    default Tokenizer buildTokenizer(Langchain4jOptionsReader o) { throw unsupported(); }
    default EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o,
                                                            EmbeddingModel embeddingModel) { throw unsupported(); }
}
```

每家一个工厂类，方法体**参照 core 层对应 `AutoConfig` 的 builder 链写法**（core 层零修改），
显式从 reader 取值。以 `Langchain4jQwenModelFactory` 为例：

```java
@Override
public ChatLanguageModel buildChatModel(Langchain4jOptionsReader o) {
    return QwenChatModel.builder()
            .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
            .apiKey(o.required("api-key"))
            .modelName(o.string("model-name", DEFAULT_MODEL))
            .temperature(o.dbl("temperature", 0.7))
            .topP(o.dbl("top-p", null))
            .topK(o.integer("top-k", null))
            .enableSearch(o.bool("enable-search", false))
            .seed(o.integer("seed", null))
            .repetitionPenalty(o.dbl("repetition-penalty", null))
            .stops(o.stringList("stops", Collections.emptyList()))
            .maxTokens(o.integer("max-tokens", null))
            .build();
}
```

向量库工厂照 Milvus/Redis 模板的 `Optional.ofNullable(...).orElse(DEFAULT_*)`
默认值 + `@Nullable EmbeddingModel` 维度回退：

```java
@Override
public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
    Integer dimension = o.integer("dimension", null);
    if (dimension == null && embeddingModel != null) {
        dimension = embeddingModel.dimension();
    }
    if (dimension == null) {
        throw new IllegalArgumentException("未配置 dimension，且关联 embedding-model 无法提供维度");
    }
    return RedisEmbeddingStore.builder()
            .host(o.string("host", "localhost"))
            .port(o.integer("port", 6379))
            .user(o.string("user", null))
            .password(o.string("password", null))
            .indexName(o.string("index-name", "magic-langchain4j"))
            .dimension(dimension)
            .metadataKeys(o.stringList("metadata-keys", Collections.emptyList()))
            .build();
}
```

### 5.3 短名表

`Langchain4jModelFactoryRegistry` 按资源类型持有七张短名集合（每个短名即一个已注册工厂）：

| 资源类型 | 短名（首批，实现期逐个 jar 核对） |
| --- | --- |
| chat-model | `openai`、`anthropic`、`ollama`、`qwen`、`qianfan`、`azure-openai`、`github-models`、`vertex-ai-gemini`、`google-ai-gemini`、`zhipu-ai`、`mistral-ai`、`bedrock`、`hugging-face`、`workers-ai`、`local-ai`、`docker`、`jlama` 等 |
| streaming-chat-model | 上表中具备 streaming chat 的子集 |
| language-model / streaming-language-model | `openai`、`ollama`、`qwen`、`qianfan`、`azure-openai` 等 |
| embedding-model | `openai`、`ollama`、`qwen`、`qianfan`、`azure-openai`、`voyage-ai`、`github-models`、`google-ai-gemini`、`hugging-face` 等 |
| tokenizer | `openai`、`qwen` |
| embedding-store | `in-memory`、`redis`、`milvus`、`elasticsearch`、`opensearch`、`qdrant`、`pinecone`、`weaviate`、`neo4j`、`mongodb-atlas`、`pgvector`、`chroma`、`oracle`、`tablestore`、`vearch`、`vespa`、`infinispan`、`cassandra`、`couchbase` 等 |

每家工厂只注册其真实支持的方法；单测断言每张表每个短名都有对应工厂、工厂类可加载、
且工厂实际支持被注册的资源类型。

### 5.4 core 层零修改（硬约束）

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-*/` 下所有源码**只做参考，禁止任何改动**
  （含修饰符、方法、字段、注解）；
- 供应商模型类（`dev.langchain4j.community.dashscope.QwenChatModel`、`dev.langchain4j.model.openai.OpenAiChatModel`、
  `dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore` 等）来自 langchain4j 官方 jar，
  由 `fw-plus-ai-boot-autoconfigure/pom.xml` 以 optional 引入后直接调用其 builder；
- 若某供应商未提供公开 builder 参数或仅 spring 装配可见，则在 autoconfigure 层按 reader 方案
  自行取值构建，**不回退修改 core 层**。

## 6. 注册器与生命周期

每个资源类型一个 `AbstractMagicDynamicRegistry<T>` 子类，遵循项目已验证的
**资源托管规则（规则 A，CLAUDE.md「MQ 插件动态注册器」）**：

```java
@Override
protected boolean register(MappingNode<T> node) {
    Langchain4jModelInfo entity = node.getEntity();
    holder.evict(entity.getKey());            // 先销毁旧实例，避免泄漏连接
    if (!entity.isEnabled()) {
        return true;                          // 停用：仅清理，不构建
    }
    Object instance = factoryRegistry.build(entity);  // options → reader → 工厂构建
    if (instance == null) {
        return false;                         // 构建失败：不注册，控制台保存报错
    }
    holder.put(entity.getKey(), instance);
    return super.register(node);
}

@Override
protected void unregister(MappingNode<T> node) {
    holder.evict(node.getEntity().getKey());
    super.unregister(node);
}
```

- `@EventListener(condition = "#event.type == 'langchain4j-chat-model'")` 监听 `FileEvent`/`GroupEvent`（每类各自类型串）。
- `defaultMappings()`：返回启动期已加载资源的列表（供控制台资源树展示），不触发构建。
- `Langchain4jModelHolder`：每类一张 `ConcurrentHashMap<String, Object>`；对实现 `AutoCloseable`
  的实例（向量库客户端）驱逐时 `close()`，异常仅记日志不阻断。

### 6.1 EmbeddingStore 与 EmbeddingModel 搭配

- `embedding-store` 注册时：按 `embeddingModelKey` 从 holder 取 `EmbeddingModel`；取不到则
  **注册失败**并提示「请先配置并启用 embedding-model: {key}」（register 返回 false，控制台保存可见）。
- dimension 决策链：options `dimension` → `embeddingModel.dimension()` → 报错。
- `unregister`/停用 embedding-model 时，不级联删除 store 实例；store 的后续 register 会按上述规则失败，
  由使用方显式修复依赖（YAGNI，不做级联）。
- 启动顺序：两类资源同批加载，互相无循环依赖（store → model 单向）；若 store 的 `FileEvent`
  先于 model 到达，该次 register 失败并记录，用户保存/刷新 model 后可手动重存 store 触发。
  实施时在启动完成钩子（`SmartLifecycle` 或 `ApplicationRunner`）对启动期失败的 store 做一次
  注册重试，保证重启后自动恢复。

## 7. 脚本 API（Langchain4jModule 改造）

```magicscript
import langchain4j;
var chatModel = langchain4j.getChatLanguageModel('qwen-main');
var reply = chatModel.chat("你好");
var embedding = langchain4j.getEmbeddingModel('qwen-embed').embed("测试文本");
var store = langchain4j.getEmbeddingStore('milvus-main');
var count = langchain4j.getTokenizer('openai').estimateTokenCountInText("统计 token");
```

- `@MagicModule("uid")` → `@MagicModule("langchain4j")`；
- `get*` 委托 `Langchain4jModelHolder`，未配置时抛中文明确异常（列出已配置的 key）；
- 删除全部 `build*` 方法（实例只能来自 datasource 定义）；
- `remove(String type, String key)` 改为 holder 驱逐语义；
- `chat(...)`/`chat(Class, ...)`/`Assistant` 保持不变，模型入参由调用方从 holder 获取；
- `destroy()` 清空全部 holder（含 close）；旧的内嵌 map（languageModels/tokenizers/…）整体迁移到
  holder 后删除。

## 8. Controller（连接测试）

`Langchain4jDatasourceController`，路径 `POST /langchain4j/{type}/datasource/test`：

- `type` 取七类之一；body 为待测的 `Langchain4jModelInfo` JSON（含 options，未保存也可测）；
- embedding-store 的 test 额外接受 `embeddingModelKey`（从 body 或已保存的 holder 取 EmbeddingModel）；
- 构建实例后按类型执行最小探测：
  - chat-model → `chat("ping")`；
  - language-model → `generate("ping")`；
  - embedding-model → `embed("test")`；
  - tokenizer → `estimateTokenCountInText(...)`；
  - embedding-store → 构造 `TextSegment` 做 add + search 探测（探测后 close）；
  - streaming 两类 → 仅构建验证（不发调用）。
- 返回 `ok` 或中文错误信息（对齐 mongo datasource test 的 `res === 'ok'` 协议）。

## 9. 前端（fw-plus-ai-magicapi-plugin-langchain4j）

- `index.js` 注册 `datasources: [7 项]`，每项含 `type`（见 §1.2）、`icon`、`title/name`（i18n key）、
  `service`、`component`；
- 七个 Vue 组件（每类一个）：公共字段区（key/name/provider/enabled）+ options 动态表单区；
  options 表单按 `provider-meta.js` 中 (资源类型 × provider) 的字段清单渲染——
  **各 provider 字段不同（如 qwen 显示 `enable-search`、openai 不显示）正是 JSON 不一致的可视化体现**；
  `embedding-store` 组件多一个 `embeddingModelKey` 下拉（选项来自 `langchain4j-embedding-model` 资源列表）；
- `provider-meta.js`：每 (type, provider) 声明 options 字段（键名 kebab-case、label、类型、必填、默认值、
  占位、帮助文案）；新增供应商只改此表 + 后端工厂与短名表；
- `service/magic-langchain4j-datasource.js`：`doTest(info)` → `POST /langchain4j/{type}/datasource/test`，
  结果 `modal.alert` 提示（协议仿 mongo-datasource service）；
- i18n 中英文文案；构建产物经 maven `exec-maven-plugin`（npm install && npm run build）+
  `maven-resources-plugin` 复制到 `target/classes/magic-editor/plugins/`；
- 后端 `Langchain4jDatasourceConfiguration implements MagicPluginConfiguration`，
  `plugin()` 返回 `new Plugin("langchain4j", "MagicLangchain4j", "magic-langchain4j.1.0.0.iife.js")`，
  `controllerRegister()` 注册 `Langchain4jDatasourceController`。

## 10. 依赖策略

- `fw-plus-ai-boot-dependencies`（AI 层 BOM）新增 langchain4j 全生态供应商版本管理，
  与 langchain4j-bom 对齐（当前 langchain4j 0.35.0 / community-dashscope 1.0.0-beta1 等）；
- `fw-plus-ai-boot-autoconfigure/pom.xml` 以 `optional` 引入全部模型供应商、Tokenizer 与向量库 jar；
- core 层 `fw-plus-ai-langchain4j-*` 零修改，不新增/删除任何依赖；
- 全量 optional 保证任意 provider 开箱即用，同时不传递给下游使用方。

## 11. 测试策略

**单元测试**（JUnit 5 + Mockito）：

1. `Langchain4jOptionsReaderTest`：各类型读取与默认值、`required` 缺失的中文异常含完整路径
   （`langchain4j-chat-model[qwen-main].options.api-key`）、Duration/List/枚举/数值转换与超范围报错；
2. `Langchain4jModelFactoryRegistryTest`：七张短名表每个短名均有工厂、工厂类可加载、表外短名被拒、
   工厂注册的方法集合与资源类型表一致；
3. `Langchain4jQwenModelFactoryTest`（每家至少一个代表）：builder 链参数透传、默认值语义、
   reader 缺参报错；
4. `Langchain4jModelInfoMagicResourceStorageTest`：key 必填/格式/唯一性、provider 校验、
   embeddingModelKey 必填、读写往返（含 options 自由 map）；
5. `Langchain4jXxxMagicDynamicRegistryTest`（×7）：构建失败不落库且不产孤立实例、`enabled=false` 仅清理、
   重复 register 覆盖、unregister 驱逐并 close；
6. `Langchain4jEmbeddingStoreMagicDynamicRegistryTest`：embeddingModel 缺失时注册失败、
   dimension 回退链（options 优先、model 次之、都无报错）、启动重试机制；
7. `Langchain4jModuleTest`：getter 只读委托、未配置异常、`remove` 语义、`chat(...)` 透传；
8. `Langchain4jModelHolderTest`：驱逐 close 语义、异常容错。

**core 层零修改审计**：`git diff` 确认 `fw-plus-ai-langchain4j-*` 无任何改动。

**集成测试**：上下文拉起（`MagicAPIBaseAutoConfiguration` → `Langchain4jDatasourceConfiguration`）→
七个 storage + 七个 registry bean 齐备 → 以 `FileEvent` 触发注册 → holder 出现实例 → delete 后消失；
embedding-model → embedding-store 顺序注册与重启重试场景。扩展现有
`LangChain4jAutoConfigurationRegistrationTest` 的注册表断言风格。

**验收标准（Definition of Done）**：

1. `fw-plus-ai-boot-autoconfigure` 单测与集成测试全绿；
2. `fw-plus-ai-magicapi-plugin-langchain4j` `mvn package`（含 npm build）产物就位于
   `target/classes/magic-editor/plugins/magic-langchain4j.1.0.0.iife.js`；
3. 控制台数据源页签出现七个 `langchain4j-*` 类型，新建/保存/停用/删除全链路可用，test 按钮可用；
4. 样例工程 `fw-plus-ai-chat-sample` 现有静态配置路径（`langchain4j.open-ai.*`）不回归；
5. 命名审计：对外命名（folder/type/插件标识/包名/类名/脚本模块）均含 `langchain4j` 且无 `uid` 残留；
6. core 层 diff 为空（零修改约束）。

## 12. 风险与权衡

| 风险 | 缓解 |
| --- | --- |
| options 为自由 map，拼写错误只在运行期暴露 | reader 缺失报错含完整路径；前端按 provider 元数据渲染表单降低手输；test 接口即时回显 |
| 公共字段与 options 边界模糊 | 基类固定五（+store 一）个公共字段，其余一律入 options；`MagicResourceStorage` 测试固化 |
| 显式工厂数量大（每家一个类） | 模板化代码（参照 core 层 AutoConfig）+ 每家单测覆盖；短名表与工厂一一对应由单测保证 |
| 全量 optional 依赖增大依赖树与构建时间 | 版本统一 BOM 管理；oss 产物不传递 optional |
| embedding-store 依赖 embedding-model 的加载顺序 | 启动完成钩子统一重试注册（§6.1） |
| datasource type 与 folder 不一致 | type 常量与 `folder()` 同源定义，单测断言相等 |
