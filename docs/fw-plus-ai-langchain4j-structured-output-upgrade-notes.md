# fw-plus AI 结构化输出：0.35.0 → 1.22.0 升级说明

**模块路径**：`fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`（脚本模块 `Langchain4jModule` + 模型工厂 `langchain4j/factory`）

> **文档定位**：本工作区 = `springboot3` 分支（langchain4j `1.22.0`）。本文说明从 `dev`（langchain4j `0.35.0`）
> 升级到本分支时，结构化输出（JSON / POJO）这块的**能力变化、可直接落地的写法、仍需注意的坑**。
> 仓库级升级说明见本仓库 `docs/springboot3-upgrade-notes.md`，本文是它在 AI / 结构化输出方向上的补充。
>
> dev 侧的「用法指南」不在本仓库，位于 dev 工作区 `java-framework/docs/fw-plus-ai-langchain4j-structured-output.md`。

| 项 | 本工作区（springboot3，目标） | 对照：dev 分支 |
|---|---|---|
| langchain4j | `1.22.0`（字节码 `major=61`） | `0.35.0`（字节码 `major=52`） |
| **JDK** | **17**（父 POM 显式 `<jdk.version>17</jdk.version>`；`spring-boot-starter-parent 3.5.8` 默认 `java.version=17`） | **1.8**（继承 `spring-boot-starter-parent 2.7.18` 默认值） |
| Spring Boot | `3.5.8` | `2.7.18` |
| 工作区 | `.../New/java-framework3` | `.../New/java-framework` |

---

## 0. 结论速览

**升级不会"自动"获得原生 schema**：能力仍要 provider 支持 **且** 显式开启（1.x 默认关闭）。升级真正带来三件事：

1. **顶层 `List<Pojo>` / `Set<Pojo>` 可用了**（0.35 的"包一层 POJO"绕法可以撤掉）。
2. **能力开关公开化**：多家 provider 的 builder 上出现 `supportedCapabilities(...)`（0.35 只有 openai 能靠 `responseFormat("json_schema")` 间接开启）。
3. **出现"按调用下发 schema"的钩子** `AiServices.chatRequestTransformer(...)`，以及公开的 `Type → schema` / `Type → 对象` 工具（`ServiceOutputParser#jsonSchema(Type)` / `#parseText(Type, String)`）。

⚠️ **一条硬约束**：`langchain4j 1.22.0` 是 **Java 17 字节码**，升级到 1.22.0 = **JDK 8 → 17+**（不是换个 jar 那么简单）。

---

## 1. 能力对照表（实测）

| 能力 | 0.35.0（dev） | 1.22.0（本分支，实测） |
|---|---|---|
| 顶层 `List<Pojo>` 生成 schema | ❌ `IllegalArgumentException: properties cannot be null or empty` | ✅ 包成 `{values: JsonArraySchema[...]}`（`name="List_of_Tag"`，外层 `required=[values]`；`Set` 同理 → `Set_of_Tag`） |
| 顶层 `List<Pojo>` 解析 | ⚠️ 退化为 `List<Map>` | ✅ 真 `List<Tag>`（`PojoListOutputParser` / `PojoSetOutputParser` / `PojoCollectionOutputParser`；0.35 一个都没有） |
| 顶层 `List<Pojo>` 的提示词降级 | ❌ `IllegalConfigurationException: Illegal method return type` | ❌ `IllegalStateException`（message 为 `null`；官方类型表同样标 ❌）→ **只能走 schema** |
| `Type → schema` 公开入口 | ❌ 仅包私有 `JsonSchemas` | ✅ `ServiceOutputParser#jsonSchema(Type)` → `Optional<JsonSchema>` |
| `Type → 对象` 公开入口 | `ServiceOutputParser#parse(Response, Type)` | ✅ `ServiceOutputParser#parseText(Type, String)`、`#parse(ChatResponse, Type)` |
| 按调用下发 schema | ❌ 无钩子 | ✅ `AiServices#chatRequestTransformer(UnaryOperator/BiFunction)` |
| 模型声明能力 | 隐式：openai `responseFormat("json_schema")` / gemini `responseFormat(JSON)` | ✅ 显式：`builder.supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)` |
| `strictJsonSchema` | 仅 openai，默认 `false` | openai / azure / mistral（含 streaming / responses），默认 `false` |
| schema 的 `required` 默认 | **全字段 required**、`additionalProperties=false` | **默认全可选**（`required=[]`）→ 用 `@JsonProperty(required = true)` 补 |
| ` ```json ` 代码块剥离 | POJO ✅；`String` ❌（原样返回） | POJO ✅；**`List<Pojo>` ❌**（`OutputParsingException`）；`String` ❌ |
| 框架模型工厂是否暴露开关 | ❌ | ❌（仍未暴露） |
| 脚本入口返回类型 | `String` | `String`（`SimpleAssistant#chat(...)` 仍返回 `String`） |

实测原文（`ServiceOutputParser` 直接调用，非推理）：

```text
# 1.22.0
jsonSchema(List<Tag>) -> Optional[JsonSchema { name = "List_of_Tag", rootElement = JsonObjectSchema {
    properties = {values=JsonArraySchema { items = JsonObjectSchema {
        properties = {name=JsonStringSchema, weight=JsonIntegerSchema}, required = [], additionalProperties = null } }},
    required = [values], additionalProperties = null } }]
outputFormatInstructions(List<Tag>) -> IllegalStateException: null
parseText(List<Tag>, '[{"name":"a","weight":1}]')       -> [Tag@64cd705f]        # 真 POJO
parseText(List<Tag>, '```json\n[...]\n```')             -> OutputParsingException
parseText(String,   '```json\n{"a":1}\n```')            -> 原样返回（含代码块）

# 0.35.0
jsonSchemaFrom(List<Tag>) -> IllegalArgumentException: properties cannot be null or empty
outputFormatInstructions(List<Tag>) -> IllegalConfigurationException: Illegal method return type
parse(Response, List<Tag>) [{"name":"a","weight":1}]    -> [{name=a, weight=1.0}]  # List<Map>，类型退化
parse(Response, Tag)  '```json\n{...}\n```'             -> Tag（能剥代码块）
parse(Response, String) '```json\n{...}\n```'           -> 原样返回（含代码块）
```

> 完整复现命令见 §6。

---

## 2. 官方支持面 vs 本仓库已有工厂

官方《Structured Outputs》列出的 JSON Schema provider：**Amazon Bedrock、Azure OpenAI、Google AI Gemini、Mistral、Ollama、OpenAI**（+ watsonx）。
**必须显式开启**：只有 `supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)` 后模型才会真正下发 schema。

本工作区 1.22.0 已装配的工厂（`langchain4j/factory/`，共 28 个子目录 = 26 个 chat provider + `embedding` / `store`）：
`anthropic`、`azure`、`bedrock`、`chatglm`、`cohere`、`github`、`googleai`、`googlegenai`、`huggingface`、`jina`、`localai`、`mistralai`、`nomic`、`ocigenai`、`ollama`、`openai`、`openaiofficial`、`ovhai`、`qianfan`、`qwen`、`vertexai`、`voyageai`、`watsonx`、`workersai`、`xinference`、`zhipu`（另含 `embedding`、`store` 两个非 chat 目录）。

| provider（工厂名） | 官方列为支持 | 1.22.0 jar 实测有 `supportedCapabilities(` | 实测有 `strictJsonSchema(` |
|---|---|---|---|
| `openai` / `openaiofficial` | ✅ | ✅ | ✅ |
| `azure` | ✅ | ✅ | ✅ |
| `bedrock` | ✅ | ✅ | —（不需要） |
| `googleai`（gemini） | ✅ | ✅ | —（不需要） |
| `mistralai` | ✅ | ✅ | ✅ |
| `ollama` | ✅ | ✅ | —（不需要） |
| `watsonx` | ✅（文档提及） | 未验证（m2 无 1.22.0 jar） | 未验证 |
| `anthropic` | ❌（不在清单） | ✅（开关存在，但是否支持结构化需自测） | — |
| 其余（`qwen` / `qianfan` / `zhipu` / `chatglm` / `github` / `localai` / `huggingface` / `googlegenai` / `ocigenai` / `ovhai` / `workersai` / `xinference` / `nomic` / `vertexai` / `voyageai` / `jina` / `cohere`） | ❌ | 本轮未扫（m2 无 1.22.0 jar） | 本轮未扫 |

> 扫描口径：只统计**本机 `~/.m2` 里存在的 1.22.0 jar**（anthropic / azure-open-ai / bedrock / google-ai-gemini / mistral-ai / ollama / open-ai + core），所以"其余未扫"是缺 jar 而非缺能力——需要时按 §6 的命令自行补扫。

---

## 3. 升级后怎么写（1.22.0）

### 3.1 原生 schema（首选）

```java
import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;

ChatModel model = OpenAiChatModel.builder()
        .apiKey("sk-...")
        .modelName("gpt-4o-mini")
        .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)   // 必须显式声明
        .strictJsonSchema(true)                               // openai / azure / mistral 需要
        .build();

public interface TagsExtractor {
    Tags extract(String text);   // 顶层 List<Tag> 也可以直接返回
}

TagsExtractor extractor = AiServices.create(TagsExtractor.class, model);
Tags tags = extractor.extract(text);
```

注意（1.x 官方限制 + 实测）：

- **不支持 streaming**（流式下 schema 不生效）。
- schema 生成的字段**默认全 optional**（实测 `required=[]`）；要必填就加 `@JsonProperty(required = true)`。
- `@Description` 会进入 schema（`dev.langchain4j.model.output.structured.Description`）。
- `Map`、`Date`、`LocalDate`、`LocalTime`、`LocalDateTime` **不支持** schema，会自动降级为提示词。
- `List<Pojo>` 底层是 `{values: [...]}` 包装（实测），别按"裸数组"去对齐。

### 3.2 低层：显式构造 schema（可精确控制）

```java
JsonSchema schema = JsonSchema.builder()
        .name("Person")
        .rootElement(JsonObjectSchema.builder()
                .addStringProperty("name")
                .addIntegerProperty("age")
                .addEnumProperty("profession", Arrays.asList("engineer", "lawyer", "doctor"))
                .required("name", "age")
                .build())
        .build();

ChatRequest request = ChatRequest.builder()
        .messages(UserMessage.from("JOHN DOE, 42 years old, engineer"))
        .responseFormat(ResponseFormat.builder().type(ResponseFormatType.JSON).jsonSchema(schema).build())
        .build();

ChatResponse response = model.chat(request);
```

`JsonRawSchema.from(String)` 可直接塞原始 JSON Schema 文本。

### 3.3 按调用下发 schema（`chatRequestTransformer`）——本仓库"不加新入口"的关键

官方文档给出的用途就是"**按调用**指定 schema"：

```java
TagExtractor extractor = AiServices.builder(TagExtractor.class)
        .chatModel(model)
        .chatRequestTransformer((request, memoryId) -> request.toBuilder()
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormatType.JSON)
                        .jsonSchema(schemaFor(memoryId))
                        .build())
                .build())
        .build();
```

- 这是 0.35 **结构上做不到**的事（那一代根本没有这个钩子）。
- ⚠️ **未验证点**：`chatRequestTransformer` 与"提示词降级指令"的先后顺序，本轮未逐条反编译确认（1.22.0 无 sources jar）。因此若你打算**保持方法返回 `String`**、纯靠 transformer 强制 schema（脚本入口式用法），请先做一次实测再依赖；稳妥写法是**返回类型用 POJO**，让内建解析器负责解析。

### 3.4 只要 JSON 文本：解析工具

```java
ServiceOutputParser parser = new ServiceOutputParser();
Object o = parser.parseText(listOfTagType, rawText);   // Type → 对象
Optional<JsonSchema> s = parser.jsonSchema(type);      // Type → schema
```

实测坑：`parseText(List<Tag>, ...)` 只吃**裸 JSON**，带 ` ```json ` 代码块会抛 `OutputParsingException`；而 POJO 返回类型那条路能剥代码块。所以 `List<Pojo>` 更推荐直接用 AiServices 接口，而不是自己 `parseText`。

---

## 4. fw-plus 侧落地要点（本次不改框架代码）

- **脚本入口不会因为升级就获得结构化输出**：`SimpleAssistant#chat(...)` 在本分支仍返回 `String`
  （已核对该类公开方法签名），而 schema 只由**接口方法返回类型**决定。
- **工厂仍未暴露 schema 相关开关**：本工作区 `Langchain4jOpenAiModelFactory` 对
  `supported-capabilities` / `strict-json-schema` / `response-format` 的匹配数为 **0**，因此**无法通过 Magic-API datasource JSON 开启原生 schema**。
- 三条不改框架代码的路（按推荐顺序）：
  1. **自建 `ChatModel` Bean**（或直接 build），配 `supportedCapabilities(...)` + `strictJsonSchema(true)`，再 `AiServices.create(你的接口, model)`；
  2. **自定义 `ModelFactory`** 注册进 `Langchain4jModelFactoryRegistry`（`register(ModelFactory)` 公开，registry 是 `@Bean` + `@ConditionalOnMissingBean`）；
  3. 低层 `ChatRequest` + `ResponseFormat` 显式下发（§3.2）。
- 若要让**脚本入口开箱可用**：在工厂补 `supported-capabilities` / `strict-json-schema` 两个 options 键即可（openai 各 1 行），
  但**仍需要有一个返回 POJO 的接口方法**才能真正吃到 schema —— 那就是新入口，属于超出本次"只写文档"的范围。

---

## 5. 迁移检查清单

- [ ] **JDK**：确认已升到 17+（1.22.0 是 Java 17 字节码）。
- [ ] **类型**：顶层 `List<Pojo>` / `Set<Pojo>` 不再需要包一层（但注意 `{values:[...]}` 的底层包装）。
- [ ] **降级路径**：`List<Pojo>` 的提示词降级在 1.22 **也不可用**（`IllegalStateException`）→ 必须走 schema。
- [ ] **required 语义变化**：0.35 全必填 → 1.22 全可选；需要必填的字段补 `@JsonProperty(required = true)`。
- [ ] **开启能力**：`supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)`（默认关闭），openai/azure/mistral 再配 `strictJsonSchema(true)`。
- [ ] **流式**：schema 场景不支持 streaming。
- [ ] **解析**：可从"自己 fastjson2 解析"改为接口返回 POJO（更稳）；`parseText` 只吃裸 JSON。
- [ ] **脚本侧**：提示词模板可保留作兜底；若要强约束，需走 §4 的自建 model / 自建接口。

---

## 6. 复现方法（实测口径）

能力开关扫描（1.22.0，class 文件是 `major=61`，需要 JDK 17+ 才能反编译）：

```bash
for j in "$HOME"/.m2/repository/dev/langchain4j/langchain4j-*/1.22.0/*.jar; do
  n=$(basename "$j" .jar)
  sc=$(unzip -p "$j" 2>/dev/null | strings | grep -c supportedCapabilities)
  sj=$(unzip -p "$j" 2>/dev/null | strings | grep -c strictJsonSchema)
  [ "$sc" != "0" -o "$sj" != "0" ] && echo "$n : supportedCapabilities=$sc strictJsonSchema=$sj"
done
```

行为探针（§1 表格来源）：与 `dev.langchain4j.service.output` 同包建类（同包才能访问包私有类型），
JDK 21 编译运行，classpath = `langchain4j-1.22.0.jar` + `langchain4j-core-1.22.0.jar` + `jackson-{databind,core,annotations} 2.22.x`，依次调用：

- `new ServiceOutputParser().jsonSchema(Type)` → 看 `required` / `values` 包装
- `outputFormatInstructions(List<Tag>)` → 看是否仍抛异常
- `parseText(List<Tag>, 裸JSON)` / `parseText(Tag, ...)`
- `parse(ChatResponse, Type)`（代码块 case）
