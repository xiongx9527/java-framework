---
comet_change: optimize-fw-plus-template
role: technical-design
canonical_spec: openspec
---

# 优化 fw-plus-template 技术设计

## 1. 背景与目标

`fw-plus-template` 是框架中 Word、Excel、HTML 模板解析与部分格式转换能力的 Core 层入口。该能力还会通过 Spring Boot 自动配置创建 Bean，并通过 Magic-API `template` 模块暴露给脚本侧调用。

当前实现存在几个稳定性风险：

- `TemplateParser` 中 Word URL 入口存在历史拼写错误 `parsWordByUrl`。
- `TemplateParser` 带参构造允许 Word / Excel / HTML parser 为 `null`，调用时可能出现缺少上下文的 NPE。
- 解析异常统一包装为无上下文 `RuntimeException`，不利于排查是 Word、Excel 还是 HTML 能力失败。
- Boot 自动配置使用无参数 `@ConditionalOnMissingClass` 表达默认 Bean 覆盖规则，语义不符合“用户 Bean 优先”。
- Magic-API `TemplateModule.parseWordByUrl` 当前继续委托到拼写错误入口。
- 核心行为缺少测试覆盖。

本设计采用“最小兼容稳定化”方案，在不重写模板引擎体系、不替换底层依赖、不改变 Maven 坐标和包名的前提下，稳定入口、错误边界、自动配置和 Magic-API 委托路径。

## 2. 总体方案

保持现有组件层次不变：

```text
调用方 / Magic-API 脚本
        │
        ▼
Boot Magic-API TemplateModule
        │
        ▼
TemplateParser 统一入口
        │
        ├── WordAbstractParser / WordParser
        ├── ExcelAbstractParser / ExcelParser
        └── HtmlAbstractParser / HtmlParser
```

职责边界：

- Core 层负责模板解析入口、parser 可用性校验、输入校验、异常上下文和可选转换能力边界。
- Boot 层负责默认 parser Bean 与 `TemplateParser` Bean 的自动配置，并遵循用户 Bean 优先。
- Magic-API 层保持薄委托，只暴露现有方法签名并委托到 Core 层稳定入口。

不新增 `TemplateException`，不引入能力注册模型，不做 Magic-API runtime 脚本级集成测试。

## 3. Core 层设计

### 3.1 `TemplateParser` API 兼容治理

新增正确拼写入口：

```java
public byte[] parseWordByUrl(String url, Map<String, Object> map)
```

保留历史拼写错误入口：

```java
public byte[] parsWordByUrl(String url, Map<String, Object> map)
```

旧入口内部委托到新入口：

```java
return parseWordByUrl(url, map);
```

这样可以让新代码使用正确 API，同时不破坏已有 Java 调用或间接调用。

### 3.2 parser 可用性校验

`TemplateParser` 在每个入口调用具体 parser 前执行能力检查：

- Word 入口要求 `wordParser != null`
- Excel 入口要求 `excelParser != null`
- HTML 入口要求 `htmlParser != null`

缺失时抛出 `IllegalStateException`，消息必须包含具体能力名称，例如：

- `Word template parser is not available`
- `Excel template parser is not available`
- `HTML template parser is not available`

允许 `TemplateParser` 在构造时持有部分 `null` parser，因为某些应用可能只引入部分底层依赖。是否可用由调用时的能力检查决定。

### 3.3 输入校验

Data 入口：

- `data == null` 或 `data.length == 0` 视为非法输入。
- 抛出 `IllegalArgumentException`。
- 错误消息包含模板类型和输入问题。

URL 入口：

- `url == null` 或空白字符串视为非法输入。
- 抛出 `IllegalArgumentException`。
- 错误消息包含模板类型和 URL 问题。

参数 Map：

- `map == null` 时统一转为 `Collections.emptyMap()`。
- 这样无变量模板不要求调用方显式传空 Map，也避免底层模板引擎暴露不一致的 null 行为。

### 3.4 异常包装策略

不新增专门 `TemplateException`。异常类型按语义使用 Java 标准异常：

- `IllegalArgumentException`：非法输入。
- `IllegalStateException`：parser 能力不可用或底层解析失败。
- `UnsupportedOperationException`：可选转换能力未配置或不支持。

底层 parser 抛出的异常应包装为带上下文的 `IllegalStateException`，保留原始异常为 cause。例如：

- `Failed to parse Word template from data`
- `Failed to parse Word template from URL`
- `Failed to parse Excel template from data`
- `Failed to parse Excel template from URL`
- `Failed to parse HTML template from data`
- `Failed to parse HTML template from URL`

### 3.5 Parser 转换能力边界

`WordParser`、`ExcelParser`、`HtmlParser` 不做大重构，只治理可选转换边界：

- 未配置 `word2Html` 时，`WordParser.toHtml` 抛明确 `UnsupportedOperationException`。
- 未配置 `word2Pdf` 时，`WordParser.toPdf` 抛明确 `UnsupportedOperationException`。
- 未配置 `html2Pdf` 时，`HtmlParser.toPdf` 抛明确 `UnsupportedOperationException`。
- `ExcelParser.toPdf` 保持 unsupported。
- 不新增底层转换依赖，不做静默降级。

## 4. Boot 自动配置设计

`TemplateParserConfiguration` 的默认 Bean 创建条件调整为“缺 Bean”语义，符合 Spring Boot 自动配置惯例。

### 4.1 Word parser Bean

创建条件：

- classpath 存在 `cn.afterturn.easypoi.word.parse.ParseWord07`
- 容器中不存在 `WordAbstractParser` Bean

行为：

- 用户提供 `WordAbstractParser` 时，不创建默认 `WordParser`。
- 用户未提供时，创建 `new WordParser(word2Html, word2Pdf)`。

### 4.2 Excel parser Bean

创建条件：

- classpath 存在 `cn.afterturn.easypoi.excel.ExcelImportUtil`
- 容器中不存在 `ExcelAbstractParser` Bean

行为：

- 用户提供 `ExcelAbstractParser` 时，不创建默认 `ExcelParser`。
- 用户未提供时，创建 `new ExcelParser(excel2Html)`。

### 4.3 HTML parser Bean

创建条件：

- classpath 存在 `freemarker.template.Template`
- 容器中不存在 `HtmlAbstractParser` Bean

行为：

- 用户提供 `HtmlAbstractParser` 时，不创建默认 `HtmlParser`。
- 用户未提供时，创建 `new HtmlParser(html2Pdf)`。

### 4.4 TemplateParser Bean

创建条件：

- 容器中不存在 `TemplateParser` Bean

行为：

- 使用容器中已有的 Word / Excel / HTML parser Bean 组装 `TemplateParser`。
- 允许某类 parser 缺失。
- 缺失能力在调用时由 `TemplateParser` 抛出明确 `IllegalStateException`。

## 5. Magic-API 模块设计

`TemplateModule` 保持现有方法签名：

- `parseWordByData`
- `parseWordByUrl`
- `parseExcelByData`
- `parseExcelByUrl`
- `parseHtmlByData`
- `parseHtmlByUrl`

关键调整：

```text
TemplateModule.parseWordByUrl(...)
  → TemplateParser.parseWordByUrl(...)
```

不再委托到历史拼写错误的 `parsWordByUrl`。

`TemplateModule` 仍作为薄委托层，不承载输入校验或异常转换逻辑。校验与异常上下文统一由 `TemplateParser` 负责。

## 6. 测试策略

### 6.1 Core 层测试

优先覆盖 `TemplateParser` 行为契约：

- `parseWordByUrl` 正确委托 Word parser。
- `parsWordByUrl` 仍可用，并与 `parseWordByUrl` 等价。
- Word / Excel / HTML data 入口分别委托对应 parser。
- 某类 parser 为 null 时抛出明确 `IllegalStateException`。
- `data == null` 或空数组时抛出明确 `IllegalArgumentException`。
- `url == null` 或空白时抛出明确 `IllegalArgumentException`。
- `map == null` 时统一转为空 Map。

测试可通过轻量 stub parser 验证委托和参数，不依赖真实 Office 文件全链路。

### 6.2 Parser 转换边界测试

覆盖可稳定断言的 unsupported 场景：

- `WordParser.toPdf` 未配置 `word2Pdf` 时抛 `UnsupportedOperationException`。
- `WordParser.toHtml` 未配置 `word2Html` 时抛 `UnsupportedOperationException`。
- `HtmlParser.toPdf` 未配置 `html2Pdf` 时抛 `UnsupportedOperationException`。
- `ExcelParser.toPdf` 保持 unsupported。

不强制新增复杂 Office 文件端到端测试。

### 6.3 Boot 自动配置测试

在 Boot autoconfigure 模块使用 `ApplicationContextRunner` 风格测试：

- 无用户 parser Bean 时创建默认 Bean。
- 有用户 parser Bean 时用户 Bean 优先。
- 自动配置的 `TemplateParser` 使用容器中的 parser Bean。
- 部分 parser 缺失时 `TemplateParser` 仍可创建，调用缺失能力时明确失败。

### 6.4 Magic-API Java 层测试

不启动 Magic-API runtime。

测试重点：

- 使用 stub/mock `TemplateParser` 验证 `TemplateModule.parseWordByUrl` 委托到 `TemplateParser.parseWordByUrl`。
- 验证现有方法签名和委托路径保持兼容。
- 编译层面确保 `TemplateModule` 仍可调用 Core API。

## 7. 验证顺序

按项目分层构建约束执行：

1. 在 `fw-plus-core` 中验证 `fw-plus-template` 相关模块测试和编译。
2. Core install 后，在 `fw-plus-boot` 中验证 `fw-plus-boot-autoconfigure` 相关测试或编译。
3. 如完整测试成本过高，至少执行目标模块测试与编译，并记录未执行项和原因。

## 8. 风险与缓解

- 风险：保留 `parsWordByUrl` 会继续暴露历史拼写错误。  
  缓解：旧方法只作为兼容别名保留，新代码和 `TemplateModule` 统一使用 `parseWordByUrl`。

- 风险：异常类型从 NPE 或无上下文 RuntimeException 变为标准异常，可能影响极少数依赖异常类型的调用方。  
  缓解：该变化提升诊断能力，不删除旧 API；通过测试锁定消息和语义。

- 风险：Boot 自动配置条件调整可能改变用户 Bean 覆盖行为。  
  缓解：遵循 Spring Boot 自动配置惯例，并使用 `ApplicationContextRunner` 测试默认 Bean 与用户 Bean 优先规则。

- 风险：Magic-API 不做 runtime 集成测试，无法覆盖 `.ms` 脚本真实执行。  
  缓解：本次改动点是 Java 委托路径，Java 层测试足以覆盖；不扩大到 Magic-API 引擎集成范围。

## 9. Spec Patch

本次不回写 OpenSpec delta spec。

原因：现有 `template-parser-optimization` 规格已经覆盖稳定入口、Word URL 兼容、parser 缺失、非法输入、可选转换、Boot 自动配置和 Magic-API 兼容。`map == null` 转为空 Map 属于实现层细化，不提升为独立 OpenSpec 行为需求。
