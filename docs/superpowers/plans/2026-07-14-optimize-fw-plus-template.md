---
change: optimize-fw-plus-template
design-doc: docs/superpowers/specs/2026-07-14-optimize-fw-plus-template-design.md
base-ref: 7856512aa3855a8c0685440a8600afa7354b1721
---

# optimize-fw-plus-template 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `docs/superpowers/specs/2026-07-14-optimize-fw-plus-template-design.md` 稳定 `fw-plus-template` 的 Core 解析入口、可选转换边界、Boot 自动配置与 Magic-API 委托路径，同时保持旧 API 兼容。

**Architecture:** 保持现有 Core → Boot → Magic-API 薄委托分层不变。Core 的 `TemplateParser` 统一承担能力可用性检查、输入校验、`null` 参数 Map 归一化与异常上下文包装；具体 `WordParser` / `ExcelParser` / `HtmlParser` 只治理可选转换能力边界。Boot 自动配置改为 Spring Boot 惯用的“用户 Bean 优先”，Magic-API `TemplateModule` 只改委托到正确拼写入口。

**Tech Stack:** Java 1.8、Maven、Spring Boot 2.7.18、JUnit 5 / AssertJ（Boot 模块已有 `spring-boot-starter-test`）、EasyPoi、FreeMarker、Magic-API。

## Global Constraints

- 需求来源：Design Doc `docs/superpowers/specs/2026-07-14-optimize-fw-plus-template-design.md`；OpenSpec task 来源：`openspec/changes/optimize-fw-plus-template/tasks.md`。
- 不重写模板引擎体系，不替换底层依赖，不改变 Maven 坐标和包名。
- 不新增 `TemplateException`，不引入能力注册模型，不做 Magic-API runtime 脚本级集成测试。
- 保留历史拼写错误入口 `parsWordByUrl(String url, Map<String, Object> map)`，旧入口必须委托到新增的 `parseWordByUrl(String url, Map<String, Object> map)`。
- `TemplateParser` 允许构造时持有部分 `null` parser；调用缺失能力时抛 `IllegalStateException`，消息包含 `Word` / `Excel` / `HTML` 能力名称。
- `data == null` 或 `data.length == 0` 抛 `IllegalArgumentException`；`url == null` 或空白字符串抛 `IllegalArgumentException`；错误消息必须包含模板类型与输入问题。
- `map == null` 统一转为 `Collections.emptyMap()`。
- 底层 parser 抛出的异常包装为带上下文的 `IllegalStateException`，并保留原始异常为 `cause`。
- 未配置 `word2Html`、`word2Pdf`、`html2Pdf` 或 `excel2Html` 时，不做静默降级；抛明确 `UnsupportedOperationException`。`ExcelParser.toPdf` 保持 unsupported。
- Boot 默认 Bean 创建条件改为“缺 Bean”语义；用户自定义 `WordAbstractParser`、`ExcelAbstractParser`、`HtmlAbstractParser`、`TemplateParser` 时默认 Bean 不覆盖用户实现。
- 按 Core → Boot 顺序验证；根目录无统一 POM，不在仓库根目录直接运行 Maven。
- 每完成一个实施任务，按 Comet build 规则定向勾选对应 OpenSpec task，并提交一次 git commit；不得积攒多个任务后统一提交。

---

## OpenSpec 对齐矩阵

| OpenSpec task | 覆盖的实施任务 |
| --- | --- |
| 1.1 正确拼写 `parseWordByUrl` 与保留 `parsWordByUrl` | Task 1 |
| 1.2 parser 可用性校验 | Task 1 |
| 1.3 data / URL 非法输入校验 | Task 1 |
| 1.4 底层解析异常上下文包装 | Task 1 |
| 2.1 可选转换器缺失行为 | Task 2 |
| 2.2 旧版 `.doc`、无效 docx、无效 HTML 失败路径诊断 | Task 1、Task 2 |
| 2.3 不新增依赖或静默降级 | Task 2、Task 5 |
| 3.1 默认 Bean 条件改为缺失 Bean | Task 3 |
| 3.2 用户自定义 parser 优先 | Task 3 |
| 3.3 `TemplateParser` 使用容器 parser Bean | Task 3 |
| 3.4 classpath 不完整时默认 Bean 不创建 | Task 3 |
| 4.1 `TemplateModule.parseWordByUrl` 委托正确入口 | Task 4 |
| 4.2 Magic-API 方法签名兼容 | Task 4 |
| 4.3 注入与默认构造路径行为可预期 | Task 4 |
| 5.1 TemplateParser 单元测试 | Task 1 |
| 5.2 非法输入与可选转换器测试 | Task 1、Task 2 |
| 5.3 Boot 自动配置测试 | Task 3 |
| 5.4 TemplateModule 测试 | Task 4 |
| 5.5 Core → Boot Maven 验证 | Task 5 |
| 6.1 Maven 坐标、模块结构、包名、依赖版本回归 | Task 5 |
| 6.2 旧 `parsWordByUrl` API 兼容 | Task 1、Task 5 |
| 6.3 OpenSpec 场景覆盖 | Task 5 |

## File Structure

### Core 层：`fw-plus-core/fw-plus-project/fw-plus-template`

- Modify: `pom.xml`
  - 职责：如父 POM 未向子模块提供可用测试依赖，则为本模块补充 test scope 的 JUnit 5 / AssertJ 依赖。不得新增运行时模板依赖。
- Modify: `src/main/java/com/xiongx9527/fw/plus/template/TemplateParser.java`
  - 职责：新增正确 Word URL API；集中处理 parser 缺失、输入校验、`null` Map 归一化和异常上下文包装。
- Modify: `src/main/java/com/xiongx9527/fw/plus/template/excel/ExcelParser.java`
  - 职责：补齐 `excel2Html == null` 时的明确 unsupported 行为；保持 `toPdf` unsupported。
- Modify: `src/main/java/com/xiongx9527/fw/plus/template/word/WordParser.java`
  - 职责：保持现有 `word2Html` / `word2Pdf` unsupported 边界；如测试暴露消息不稳定，仅做最小消息治理。
- Modify: `src/main/java/com/xiongx9527/fw/plus/template/html/HtmlParser.java`
  - 职责：保持 `html2Pdf` unsupported 边界；如测试暴露消息不稳定，仅做最小消息治理。
- Create: `src/test/java/com/xiongx9527/fw/plus/template/TemplateParserTest.java`
  - 职责：通过 stub parser 覆盖 `TemplateParser` 的委托、兼容入口、输入校验、Map 归一化、parser 缺失和异常包装。
- Create: `src/test/java/com/xiongx9527/fw/plus/template/ParserConversionBoundaryTest.java`
  - 职责：覆盖 `WordParser`、`ExcelParser`、`HtmlParser` 可选转换器缺失时的明确 unsupported 行为。

### Boot 层：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure`

- Modify: `src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/TemplateParserConfiguration.java`
  - 职责：将无参数 `@ConditionalOnMissingClass` 改为按 Bean 类型判断的 `@ConditionalOnMissingBean`，同时保留底层 classpath 条件。
- Modify: `src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModule.java`
  - 职责：将 `parseWordByUrl` 委托到 `TemplateParser.parseWordByUrl`，保持所有公开方法签名不变。
- Create: `src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/TemplateParserConfigurationTest.java`
  - 职责：使用 `ApplicationContextRunner` 覆盖默认 Bean、用户 Bean 优先、`TemplateParser` 注入 parser Bean、缺 classpath 不创建默认 Bean。
- Create: `src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModuleTest.java`
  - 职责：使用 stub `TemplateParser` 验证 `TemplateModule` 的 Java 层委托路径与默认构造可用性。

## Task 1: 稳定 Core `TemplateParser` 入口与异常语义

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-template/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/TemplateParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-template/src/test/java/com/xiongx9527/fw/plus/template/TemplateParserTest.java`

**Interfaces:**
- Consumes:
  - `WordAbstractParser.parseByData(byte[] data, Map<String, Object> map) throws Exception`
  - `FileParser.parseByUrl(String url, Map<String, Object> map) throws Exception`
  - `ExcelAbstractParser.parseByData(byte[] data, Map<String, Object> map) throws Exception`
  - `HtmlAbstractParser.parseByData(byte[] data, Map<String, Object> map) throws Exception`
- Produces:
  - `TemplateParser.parseWordByUrl(String url, Map<String, Object> map): byte[]`
  - `TemplateParser.parsWordByUrl(String url, Map<String, Object> map): byte[]`，兼容别名，内部委托 `parseWordByUrl`
  - 所有 `parse*ByData` / `parse*ByUrl` 方法在 `map == null` 时传递 `Collections.emptyMap()` 给底层 parser
  - 缺 parser：`IllegalStateException("Word template parser is not available")`、`IllegalStateException("Excel template parser is not available")`、`IllegalStateException("HTML template parser is not available")`
  - 非法输入：`IllegalArgumentException`，消息分别包含 `Word template data`、`Word template URL`、`Excel template data`、`Excel template URL`、`HTML template data`、`HTML template URL`
  - 底层异常包装：`IllegalStateException("Failed to parse <type> template from <data|URL>", cause)`

- [ ] **Step 1: 确认 Core 测试依赖是否可用**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-template -DskipTests test-compile
```
Expected:
- 如果当前模块无需新增测试依赖，命令可进入 test-compile 阶段。
- 如果新增测试类后出现 `package org.junit.jupiter.api does not exist` 或 `package org.assertj.core.api does not exist`，在本任务 Step 2 中补充 test dependency。

- [ ] **Step 2: 必要时为 `fw-plus-template` 补测试依赖**

如果 Step 1 或 Step 3 后显示 JUnit / AssertJ 不可用，修改 `fw-plus-core/fw-plus-project/fw-plus-template/pom.xml`，在 `</dependencies>` 前加入：

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
```

Expected:
- 只新增 test scope 依赖，不改变运行时依赖。
- 不新增 EasyPoi、FreeMarker、POI 相关版本或 Maven 坐标。

- [ ] **Step 3: 写失败测试 `TemplateParserTest`**

Create `fw-plus-core/fw-plus-project/fw-plus-template/src/test/java/com/xiongx9527/fw/plus/template/TemplateParserTest.java` with:

```java
package com.xiongx9527.fw.plus.template;

import com.xiongx9527.fw.plus.template.excel.ExcelAbstractParser;
import com.xiongx9527.fw.plus.template.html.HtmlAbstractParser;
import com.xiongx9527.fw.plus.template.word.WordAbstractParser;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateParserTest {

    @Test
    void parseWordByUrlDelegatesToWordParser() {
        RecordingWordParser wordParser = new RecordingWordParser();
        TemplateParser parser = new TemplateParser(wordParser, null, null);

        byte[] result = parser.parseWordByUrl("https://example.com/template.docx", Collections.singletonMap("name", "fw"));

        assertThat(result).isEqualTo(RecordingWordParser.URL_RESULT);
        assertThat(wordParser.lastUrl).isEqualTo("https://example.com/template.docx");
        assertThat(wordParser.lastMap).containsEntry("name", "fw");
    }

    @Test
    void legacyParsWordByUrlDelegatesToCorrectSpellingEntry() {
        RecordingWordParser wordParser = new RecordingWordParser();
        TemplateParser parser = new TemplateParser(wordParser, null, null);

        byte[] result = parser.parsWordByUrl("https://example.com/legacy.docx", null);

        assertThat(result).isEqualTo(RecordingWordParser.URL_RESULT);
        assertThat(wordParser.lastUrl).isEqualTo("https://example.com/legacy.docx");
        assertThat(wordParser.lastMap).isEmpty();
    }

    @Test
    void dataEntriesDelegateAndConvertNullMapToEmptyMap() {
        RecordingWordParser wordParser = new RecordingWordParser();
        RecordingExcelParser excelParser = new RecordingExcelParser();
        RecordingHtmlParser htmlParser = new RecordingHtmlParser();
        TemplateParser parser = new TemplateParser(wordParser, excelParser, htmlParser);

        assertThat(parser.parseWordByData(new byte[]{1}, null)).isEqualTo(RecordingWordParser.DATA_RESULT);
        assertThat(parser.parseExcelByData(new byte[]{2}, null)).isEqualTo(RecordingExcelParser.DATA_RESULT);
        assertThat(parser.parseHtmlByData(new byte[]{3}, null)).isEqualTo(RecordingHtmlParser.DATA_RESULT);

        assertThat(wordParser.lastMap).isEmpty();
        assertThat(excelParser.lastMap).isEmpty();
        assertThat(htmlParser.lastMap).isEmpty();
    }

    @Test
    void missingParserFailsWithCapabilityName() {
        TemplateParser parser = new TemplateParser(null, null, null);

        assertThatThrownBy(() -> parser.parseWordByData(new byte[]{1}, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Word template parser is not available");
        assertThatThrownBy(() -> parser.parseExcelByData(new byte[]{1}, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Excel template parser is not available");
        assertThatThrownBy(() -> parser.parseHtmlByData(new byte[]{1}, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HTML template parser is not available");
    }

    @Test
    void invalidDataFailsBeforeDelegating() {
        RecordingWordParser wordParser = new RecordingWordParser();
        TemplateParser parser = new TemplateParser(wordParser, new RecordingExcelParser(), new RecordingHtmlParser());

        assertThatThrownBy(() -> parser.parseWordByData(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Word template data")
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(() -> parser.parseExcelByData(new byte[0], null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excel template data")
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(() -> parser.parseHtmlByData(new byte[0], null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML template data")
                .hasMessageContaining("must not be empty");
        assertThat(wordParser.parseByDataCalled).isFalse();
    }

    @Test
    void invalidUrlFailsBeforeDelegating() {
        RecordingWordParser wordParser = new RecordingWordParser();
        TemplateParser parser = new TemplateParser(wordParser, new RecordingExcelParser(), new RecordingHtmlParser());

        assertThatThrownBy(() -> parser.parseWordByUrl(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Word template URL")
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> parser.parseExcelByUrl(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excel template URL")
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> parser.parseHtmlByUrl("", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML template URL")
                .hasMessageContaining("must not be blank");
        assertThat(wordParser.parseByUrlCalled).isFalse();
    }

    @Test
    void parserExceptionIsWrappedWithOperationContextAndCause() {
        RuntimeException root = new RuntimeException("broken docx");
        TemplateParser parser = new TemplateParser(new FailingWordParser(root), null, null);

        assertThatThrownBy(() -> parser.parseWordByData(new byte[]{1}, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to parse Word template from data")
                .hasCause(root);
    }

    private static final class RecordingWordParser extends WordAbstractParser {
        private static final byte[] DATA_RESULT = new byte[]{11};
        private static final byte[] URL_RESULT = new byte[]{12};
        private boolean parseByDataCalled;
        private boolean parseByUrlCalled;
        private String lastUrl;
        private Map<String, Object> lastMap;

        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            this.parseByDataCalled = true;
            this.lastMap = map;
            return DATA_RESULT;
        }

        @Override
        public byte[] parseByUrl(String url, Map<String, Object> map) {
            this.parseByUrlCalled = true;
            this.lastUrl = url;
            this.lastMap = map;
            return URL_RESULT;
        }

        @Override
        public byte[] toHtml(byte[] srcBytes) {
            return new byte[0];
        }

        @Override
        public byte[] toPdf(byte[] srcBytes) {
            return new byte[0];
        }
    }

    private static final class RecordingExcelParser extends ExcelAbstractParser {
        private static final byte[] DATA_RESULT = new byte[]{21};
        private Map<String, Object> lastMap;

        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            this.lastMap = map;
            return DATA_RESULT;
        }

        @Override
        public byte[] toHtml(byte[] srcBytes) {
            return new byte[0];
        }

        @Override
        public byte[] toPdf(byte[] srcBytes) {
            return new byte[0];
        }
    }

    private static final class RecordingHtmlParser extends HtmlAbstractParser {
        private static final byte[] DATA_RESULT = new byte[]{31};
        private Map<String, Object> lastMap;

        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            this.lastMap = map;
            return DATA_RESULT;
        }

        @Override
        public byte[] toHtml(byte[] srcBytes) {
            return new byte[0];
        }

        @Override
        public byte[] toPdf(byte[] srcBytes) {
            return new byte[0];
        }
    }

    private static final class FailingWordParser extends RecordingWordParser {
        private final RuntimeException failure;

        private FailingWordParser(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            throw failure;
        }
    }
}
```

- [ ] **Step 4: 运行测试并确认失败原因来自未实现行为**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-template -Dtest=TemplateParserTest test
```
Expected:
- FAIL。
- 允许的失败包括：`cannot find symbol method parseWordByUrl`、旧入口未转空 Map、缺 parser 报 NPE、非法输入未校验、异常仍是 `RuntimeException`。
- 如果失败是 JUnit / AssertJ 依赖缺失，先回到 Step 2 补测试依赖，再重新执行。

- [ ] **Step 5: 最小实现 `TemplateParser`**

Modify `fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/TemplateParser.java` to include these helper methods and update all parse entries to use them:

```java
package com.xiongx9527.fw.plus.template;

import com.xiongx9527.fw.plus.template.excel.ExcelAbstractParser;
import com.xiongx9527.fw.plus.template.excel.ExcelParser;
import com.xiongx9527.fw.plus.template.html.HtmlAbstractParser;
import com.xiongx9527.fw.plus.template.html.HtmlParser;
import com.xiongx9527.fw.plus.template.word.WordAbstractParser;
import com.xiongx9527.fw.plus.template.word.WordParser;

import java.util.Collections;
import java.util.Map;

public class TemplateParser {

    private final WordAbstractParser wordParser;
    private final ExcelAbstractParser excelParser;
    private final HtmlAbstractParser htmlParser;

    public TemplateParser() {
        this.wordParser = new WordParser();
        this.excelParser = new ExcelParser();
        this.htmlParser = new HtmlParser();
    }

    public TemplateParser(WordAbstractParser wordParser, ExcelAbstractParser excelParser, HtmlAbstractParser htmlParser) {
        this.wordParser = wordParser;
        this.excelParser = excelParser;
        this.htmlParser = htmlParser;
    }

    public WordAbstractParser getWordParser() {
        return this.wordParser;
    }

    public ExcelAbstractParser getExcelParser() {
        return this.excelParser;
    }

    public HtmlAbstractParser getHtmlParser() {
        return this.htmlParser;
    }

    public byte[] parseWordByData(byte[] data, Map<String, Object> map) {
        WordAbstractParser parser = requireWordParser();
        requireData("Word", data);
        try {
            return parser.parseByData(data, safeMap(map));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Word template from data", e);
        }
    }

    public byte[] parseWordByUrl(String url, Map<String, Object> map) {
        WordAbstractParser parser = requireWordParser();
        requireUrl("Word", url);
        try {
            return parser.parseByUrl(url, safeMap(map));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Word template from URL", e);
        }
    }

    public byte[] parsWordByUrl(String url, Map<String, Object> map) {
        return parseWordByUrl(url, map);
    }

    public byte[] parseExcelByData(byte[] data, Map<String, Object> map) {
        ExcelAbstractParser parser = requireExcelParser();
        requireData("Excel", data);
        try {
            return parser.parseByData(data, safeMap(map));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Excel template from data", e);
        }
    }

    public byte[] parseExcelByUrl(String url, Map<String, Object> map) {
        ExcelAbstractParser parser = requireExcelParser();
        requireUrl("Excel", url);
        try {
            return parser.parseByUrl(url, safeMap(map));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Excel template from URL", e);
        }
    }

    public byte[] parseHtmlByData(byte[] data, Map<String, Object> map) {
        HtmlAbstractParser parser = requireHtmlParser();
        requireData("HTML", data);
        try {
            return parser.parseByData(data, safeMap(map));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse HTML template from data", e);
        }
    }

    public byte[] parseHtmlByUrl(String url, Map<String, Object> map) {
        HtmlAbstractParser parser = requireHtmlParser();
        requireUrl("HTML", url);
        try {
            return parser.parseByUrl(url, safeMap(map));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse HTML template from URL", e);
        }
    }

    private WordAbstractParser requireWordParser() {
        if (this.wordParser == null) {
            throw new IllegalStateException("Word template parser is not available");
        }
        return this.wordParser;
    }

    private ExcelAbstractParser requireExcelParser() {
        if (this.excelParser == null) {
            throw new IllegalStateException("Excel template parser is not available");
        }
        return this.excelParser;
    }

    private HtmlAbstractParser requireHtmlParser() {
        if (this.htmlParser == null) {
            throw new IllegalStateException("HTML template parser is not available");
        }
        return this.htmlParser;
    }

    private void requireData(String type, byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException(type + " template data must not be empty");
        }
    }

    private void requireUrl(String type, String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException(type + " template URL must not be blank");
        }
    }

    private Map<String, Object> safeMap(Map<String, Object> map) {
        return map == null ? Collections.emptyMap() : map;
    }
}
```

- [ ] **Step 6: 运行 Core `TemplateParserTest` 并确认通过**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-template -Dtest=TemplateParserTest test
```
Expected:
- PASS。
- 不应出现 `NullPointerException`、无上下文 `RuntimeException` 或 `parsWordByUrl` 找不到。

- [ ] **Step 7: 定向勾选并提交 Task 1**

Update `openspec/changes/optimize-fw-plus-template/tasks.md` by checking only:

```markdown
- [x] 1.1 为 `TemplateParser` 增加正确拼写的 `parseWordByUrl` 方法，并保留 `parsWordByUrl` 作为兼容委托入口。
- [x] 1.2 在 `TemplateParser` 的 Word / Excel / HTML 调用路径中增加 parser 可用性校验，缺失时返回包含能力名称的明确异常。
- [x] 1.3 在 `TemplateParser` 的 data 与 URL 入口中补充非法输入校验，覆盖 null 数据、空 URL 等边界。
- [x] 1.4 优化底层解析异常包装信息，使调用方能够区分 Word、Excel、HTML 解析失败来源。
- [x] 5.1 为 `TemplateParser` 增加单元测试，覆盖 Word / Excel / HTML 委托、兼容入口和缺失 parser 失败场景。
```

Run:
```bash
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework status --short
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework add fw-plus-core/fw-plus-project/fw-plus-template/pom.xml fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/TemplateParser.java fw-plus-core/fw-plus-project/fw-plus-template/src/test/java/com/xiongx9527/fw/plus/template/TemplateParserTest.java openspec/changes/optimize-fw-plus-template/tasks.md
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework commit -m "test(template): stabilize parser entry contracts"
```
Expected:
- Commit succeeds.
- 不包含 Task 2/3/4/5 的源码或测试改动。

## Task 2: 治理 parser 可选转换能力边界

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/excel/ExcelParser.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/word/WordParser.java`（仅当测试暴露消息不稳定时）
- Modify: `fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/html/HtmlParser.java`（仅当测试暴露消息不稳定时）
- Create: `fw-plus-core/fw-plus-project/fw-plus-template/src/test/java/com/xiongx9527/fw/plus/template/ParserConversionBoundaryTest.java`

**Interfaces:**
- Consumes:
  - `WordParser.toHtml(byte[] srcBytes): byte[]`
  - `WordParser.toPdf(byte[] srcBytes): byte[]`
  - `ExcelParser.toHtml(byte[] srcBytes): byte[]`
  - `ExcelParser.toPdf(byte[] srcBytes): byte[]`
  - `HtmlParser.toPdf(byte[] srcBytes): byte[]`
- Produces:
  - `WordParser.toHtml` 未配置转换器时抛 `UnsupportedOperationException("Word to Html conversion is not supported")`
  - `WordParser.toPdf` 未配置转换器时抛 `UnsupportedOperationException("Word to Pdf conversion is not supported")`
  - `ExcelParser.toHtml` 未配置转换器时抛 `UnsupportedOperationException("Excel to Html conversion is not supported")`
  - `ExcelParser.toPdf` 保持抛 `UnsupportedOperationException("Excel to Pdf conversion is not supported")`
  - `HtmlParser.toPdf` 未配置转换器时抛 `UnsupportedOperationException("Html to Pdf conversion is not supported")`

- [ ] **Step 1: 写失败测试 `ParserConversionBoundaryTest`**

Create `fw-plus-core/fw-plus-project/fw-plus-template/src/test/java/com/xiongx9527/fw/plus/template/ParserConversionBoundaryTest.java` with:

```java
package com.xiongx9527.fw.plus.template;

import com.xiongx9527.fw.plus.template.excel.ExcelParser;
import com.xiongx9527.fw.plus.template.html.HtmlParser;
import com.xiongx9527.fw.plus.template.word.WordParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserConversionBoundaryTest {

    @Test
    void wordParserFailsClearlyWhenWordToHtmlConverterMissing() {
        assertThatThrownBy(() -> new WordParser(null, null).toHtml(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Word to Html conversion is not supported");
    }

    @Test
    void wordParserFailsClearlyWhenWordToPdfConverterMissing() {
        assertThatThrownBy(() -> new WordParser(null, null).toPdf(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Word to Pdf conversion is not supported");
    }

    @Test
    void excelParserFailsClearlyWhenExcelToHtmlConverterMissing() {
        assertThatThrownBy(() -> new ExcelParser(null).toHtml(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Excel to Html conversion is not supported");
    }

    @Test
    void excelParserKeepsPdfUnsupported() {
        assertThatThrownBy(() -> new ExcelParser(null).toPdf(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Excel to Pdf conversion is not supported");
    }

    @Test
    void htmlParserFailsClearlyWhenHtmlToPdfConverterMissing() {
        assertThatThrownBy(() -> new HtmlParser(null).toPdf(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Html to Pdf conversion is not supported");
    }
}
```

- [ ] **Step 2: 运行测试并确认失败集中在 Excel HTML 缺失转换器**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-template -Dtest=ParserConversionBoundaryTest test
```
Expected:
- 当前 `WordParser` 与 `HtmlParser` 相关用例应已通过或仅需消息微调。
- `ExcelParser.toHtml` 在 `excel2Html == null` 时当前可能抛 `NullPointerException` 包装后的 `RuntimeException`，该用例应失败。

- [ ] **Step 3: 最小实现 Excel 转换边界**

Modify `fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/excel/ExcelParser.java` method `toHtml`:

```java
@Override
public byte[] toHtml(byte[] srcBytes) {
    if (this.excel2Html == null) {
        throw new UnsupportedOperationException("Excel to Html conversion is not supported");
    }
    try {
        return this.excel2Html.change(srcBytes);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

If `WordParser` or `HtmlParser` messages fail exact assertions, adjust only the message literals to the strings in **Produces**. Do not add new conversion implementations or dependencies.

- [ ] **Step 4: 运行 Task 1 与 Task 2 的 Core 测试**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-template -Dtest=TemplateParserTest,ParserConversionBoundaryTest test
```
Expected:
- PASS。
- 所有 unsupported 场景返回 `UnsupportedOperationException`，无 NPE 或静默降级。

- [ ] **Step 5: 定向勾选并提交 Task 2**

Update `openspec/changes/optimize-fw-plus-template/tasks.md` by checking only:

```markdown
- [x] 2.1 检查 `WordParser`、`ExcelParser`、`HtmlParser` 的可选转换器缺失行为，确保 UnsupportedOperationException 信息明确。
- [x] 2.3 保持现有底层依赖和转换能力边界，不新增非必要依赖或静默降级行为。
- [x] 5.2 为非法输入和可选转换器缺失场景增加测试，锁定明确异常信息。
```

Leave `2.2` unchecked until Task 5 confirms invalid document / invalid HTML diagnostic paths through tests or targeted checks.

Run:
```bash
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework add fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/excel/ExcelParser.java fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/word/WordParser.java fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/html/HtmlParser.java fw-plus-core/fw-plus-project/fw-plus-template/src/test/java/com/xiongx9527/fw/plus/template/ParserConversionBoundaryTest.java openspec/changes/optimize-fw-plus-template/tasks.md
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework commit -m "test(template): lock conversion capability boundaries"
```
Expected:
- Commit succeeds。
- 不包含 Boot 或 Magic-API 改动。

## Task 3: 修正 Boot `TemplateParserConfiguration` 自动配置条件

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/TemplateParserConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/TemplateParserConfigurationTest.java`

**Interfaces:**
- Consumes:
  - `TemplateParserConfiguration.wordParser(AbstractWord2Html word2Html, AbstractWord2Pdf word2Pdf): WordAbstractParser`
  - `TemplateParserConfiguration.excelParser(AbstractExcel2Html excel2Html): ExcelAbstractParser`
  - `TemplateParserConfiguration.htmlParser(AbstractHtml2Pdf html2Pdf): HtmlAbstractParser`
  - `TemplateParserConfiguration.templateParser(WordAbstractParser wordParser, ExcelAbstractParser excelParser, HtmlAbstractParser htmlParser): TemplateParser`
- Produces:
  - `@ConditionalOnMissingBean(WordAbstractParser.class)` on Word default parser Bean
  - `@ConditionalOnMissingBean(ExcelAbstractParser.class)` on Excel default parser Bean
  - `@ConditionalOnMissingBean(HtmlAbstractParser.class)` on HTML default parser Bean
  - `@ConditionalOnMissingBean(TemplateParser.class)` on `TemplateParser` Bean
  - Existing `@ConditionalOnClass` classpath guards retained for default parser Beans

- [ ] **Step 1: 写失败测试 `TemplateParserConfigurationTest`**

Create `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/TemplateParserConfigurationTest.java` with:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.template;

import com.xiongx9527.fw.plus.template.TemplateParser;
import com.xiongx9527.fw.plus.template.excel.ExcelAbstractParser;
import com.xiongx9527.fw.plus.template.excel.ExcelParser;
import com.xiongx9527.fw.plus.template.html.HtmlAbstractParser;
import com.xiongx9527.fw.plus.template.html.HtmlParser;
import com.xiongx9527.fw.plus.template.word.WordAbstractParser;
import com.xiongx9527.fw.plus.template.word.WordParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateParserConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TemplateParserConfiguration.class));

    @Test
    void createsDefaultParserBeansWhenNoUserBeansExist() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WordAbstractParser.class);
            assertThat(context).hasSingleBean(ExcelAbstractParser.class);
            assertThat(context).hasSingleBean(HtmlAbstractParser.class);
            assertThat(context).hasSingleBean(TemplateParser.class);
            assertThat(context.getBean(WordAbstractParser.class)).isInstanceOf(WordParser.class);
            assertThat(context.getBean(ExcelAbstractParser.class)).isInstanceOf(ExcelParser.class);
            assertThat(context.getBean(HtmlAbstractParser.class)).isInstanceOf(HtmlParser.class);
        });
    }

    @Test
    void userParserBeansWinOverDefaults() {
        contextRunner.withUserConfiguration(UserParserConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(WordAbstractParser.class);
            assertThat(context).hasSingleBean(ExcelAbstractParser.class);
            assertThat(context).hasSingleBean(HtmlAbstractParser.class);
            assertThat(context.getBean(WordAbstractParser.class)).isInstanceOf(UserWordParser.class);
            assertThat(context.getBean(ExcelAbstractParser.class)).isInstanceOf(UserExcelParser.class);
            assertThat(context.getBean(HtmlAbstractParser.class)).isInstanceOf(UserHtmlParser.class);
        });
    }

    @Test
    void templateParserUsesBeansFromContext() {
        contextRunner.withUserConfiguration(UserParserConfiguration.class).run(context -> {
            TemplateParser parser = context.getBean(TemplateParser.class);
            assertThat(parser.getWordParser()).isSameAs(context.getBean(WordAbstractParser.class));
            assertThat(parser.getExcelParser()).isSameAs(context.getBean(ExcelAbstractParser.class));
            assertThat(parser.getHtmlParser()).isSameAs(context.getBean(HtmlAbstractParser.class));
        });
    }

    @Test
    void userTemplateParserWinsOverDefaultTemplateParser() {
        contextRunner.withUserConfiguration(UserTemplateParserConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(TemplateParser.class);
            assertThat(context.getBean(TemplateParser.class)).isSameAs(UserTemplateParserConfiguration.TEMPLATE_PARSER);
        });
    }

    @Test
    void missingWordClasspathSkipsDefaultWordParserButStillCreatesTemplateParser() {
        contextRunner.withClassLoader(new FilteredClassLoader("cn.afterturn.easypoi.word.parse.ParseWord07"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(WordAbstractParser.class);
                    assertThat(context).hasSingleBean(TemplateParser.class);
                    assertThatThrownBy(() -> context.getBean(TemplateParser.class).parseWordByData(new byte[]{1}, null))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("Word template parser is not available");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class UserParserConfiguration {
        @Bean
        WordAbstractParser wordAbstractParser() {
            return new UserWordParser();
        }

        @Bean
        ExcelAbstractParser excelAbstractParser() {
            return new UserExcelParser();
        }

        @Bean
        HtmlAbstractParser htmlAbstractParser() {
            return new UserHtmlParser();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UserTemplateParserConfiguration {
        private static final TemplateParser TEMPLATE_PARSER = new TemplateParser(null, null, null);

        @Bean
        TemplateParser templateParser() {
            return TEMPLATE_PARSER;
        }
    }

    static class UserWordParser extends WordAbstractParser {
        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            return new byte[]{1};
        }

        @Override
        public byte[] toHtml(byte[] srcBytes) {
            return new byte[0];
        }

        @Override
        public byte[] toPdf(byte[] srcBytes) {
            return new byte[0];
        }
    }

    static class UserExcelParser extends ExcelAbstractParser {
        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            return new byte[]{2};
        }

        @Override
        public byte[] toHtml(byte[] srcBytes) {
            return new byte[0];
        }

        @Override
        public byte[] toPdf(byte[] srcBytes) {
            return new byte[0];
        }
    }

    static class UserHtmlParser extends HtmlAbstractParser {
        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            return new byte[]{3};
        }

        @Override
        public byte[] toHtml(byte[] srcBytes) {
            return new byte[0];
        }

        @Override
        public byte[] toPdf(byte[] srcBytes) {
            return new byte[0];
        }
    }
}
```

- [ ] **Step 2: 运行测试并确认失败来自条件注解语义**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TemplateParserConfigurationTest test
```
Expected:
- FAIL。
- 典型失败：用户 Bean 未优先、`TemplateParser` Bean 被默认 Bean 覆盖或无参数 `@ConditionalOnMissingClass` 语义不符合缺 Bean。

- [ ] **Step 3: 修改 `TemplateParserConfiguration` 条件注解**

Modify imports and Bean annotations in `TemplateParserConfiguration.java`:

```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
```

Use these annotations:

```java
@Bean
@ConditionalOnClass(name = "cn.afterturn.easypoi.word.parse.ParseWord07")
@ConditionalOnMissingBean(WordAbstractParser.class)
public WordAbstractParser wordParser(@Autowired(required = false) AbstractWord2Html word2Html,
                                     @Autowired(required = false) AbstractWord2Pdf word2Pdf) {
    return new WordParser(word2Html, word2Pdf);
}

@Bean
@ConditionalOnClass(name = "cn.afterturn.easypoi.excel.ExcelImportUtil")
@ConditionalOnMissingBean(ExcelAbstractParser.class)
public ExcelAbstractParser excelParser(@Autowired(required = false) AbstractExcel2Html excel2Html) {
    return new ExcelParser(excel2Html);
}

@Bean
@ConditionalOnClass(name = "freemarker.template.Template")
@ConditionalOnMissingBean(HtmlAbstractParser.class)
public HtmlAbstractParser htmlParser(@Autowired(required = false) AbstractHtml2Pdf html2Pdf) {
    return new HtmlParser(html2Pdf);
}

@Bean
@ConditionalOnMissingBean(TemplateParser.class)
public TemplateParser templateParser(@Autowired(required = false) WordAbstractParser wordParser,
                                     @Autowired(required = false) ExcelAbstractParser excelParser,
                                     @Autowired(required = false) HtmlAbstractParser htmlParser) {
    return new TemplateParser(wordParser, excelParser, htmlParser);
}
```

- [ ] **Step 4: 运行 Boot 自动配置测试**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TemplateParserConfigurationTest test
```
Expected:
- PASS。
- 用户 parser 与用户 `TemplateParser` 均不被默认 Bean 覆盖。
- 缺 Word classpath 时不创建默认 Word parser，但仍创建 `TemplateParser`，缺失能力由 Core 层明确失败。

- [ ] **Step 5: 定向勾选并提交 Task 3**

Update `openspec/changes/optimize-fw-plus-template/tasks.md` by checking only:

```markdown
- [x] 3.1 将 template parser 默认 Bean 创建条件调整为基于缺失 Bean 的覆盖规则，避免误用缺失 Class 条件。
- [x] 3.2 确保用户自定义 `WordAbstractParser`、`ExcelAbstractParser`、`HtmlAbstractParser` 时默认 Bean 不覆盖用户实现。
- [x] 3.3 确保自动配置创建的 `TemplateParser` 使用容器中已配置的 parser Bean。
- [x] 3.4 保持缺少底层依赖类时相关默认 Bean 不创建，避免 classpath 不完整导致启动失败。
- [x] 5.3 为 Boot 自动配置增加测试，覆盖默认 Bean 创建、用户 Bean 优先和 `TemplateParser` 注入 parser Bean。
```

Run:
```bash
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/TemplateParserConfiguration.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/TemplateParserConfigurationTest.java openspec/changes/optimize-fw-plus-template/tasks.md
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework commit -m "test(template): fix parser auto configuration conditions"
```
Expected:
- Commit succeeds。
- 不包含 Magic-API `TemplateModule` 改动。

## Task 4: 修正 Magic-API `TemplateModule` 委托路径并锁定兼容签名

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModule.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModuleTest.java`

**Interfaces:**
- Consumes:
  - `TemplateModule.parseWordByData(byte[] data, Map<String, Object> map): byte[]`
  - `TemplateModule.parseWordByUrl(String url, Map<String, Object> map): byte[]`
  - `TemplateModule.parseExcelByData(byte[] data, Map<String, Object> map): byte[]`
  - `TemplateModule.parseExcelByUrl(String url, Map<String, Object> map): byte[]`
  - `TemplateModule.parseHtmlByData(byte[] data, Map<String, Object> map): byte[]`
  - `TemplateModule.parseHtmlByUrl(String url, Map<String, Object> map): byte[]`
- Produces:
  - `TemplateModule.parseWordByUrl` delegates to `TemplateParser.parseWordByUrl`，不再调用 `parsWordByUrl`
  - 现有 Magic-API Java 层公开方法签名保持不变
  - `new TemplateModule(templateParser)` 注入路径可预测；`new TemplateModule()` 默认构造仍可创建模块实例

- [ ] **Step 1: 写失败测试 `TemplateModuleTest`**

Create `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModuleTest.java` with:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.template;

import com.xiongx9527.fw.plus.template.TemplateParser;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TemplateModuleTest {

    @Test
    void parseWordByUrlDelegatesToCorrectSpellingTemplateParserMethod() {
        RecordingTemplateParser parser = new RecordingTemplateParser();
        TemplateModule module = new TemplateModule(parser);

        byte[] result = module.parseWordByUrl("https://example.com/template.docx", Collections.singletonMap("name", "fw"));

        assertThat(result).isEqualTo(RecordingTemplateParser.WORD_URL_RESULT);
        assertThat(parser.parseWordByUrlCalled).isTrue();
        assertThat(parser.legacyParsWordByUrlCalled).isFalse();
        assertThat(parser.lastUrl).isEqualTo("https://example.com/template.docx");
        assertThat(parser.lastMap).containsEntry("name", "fw");
    }

    @Test
    void existingMethodsKeepDelegatingToTemplateParser() throws Exception {
        RecordingTemplateParser parser = new RecordingTemplateParser();
        TemplateModule module = new TemplateModule(parser);

        assertThat(module.parseWordByData(new byte[]{1}, null)).isEqualTo(RecordingTemplateParser.WORD_DATA_RESULT);
        assertThat(module.parseExcelByData(new byte[]{2}, null)).isEqualTo(RecordingTemplateParser.EXCEL_DATA_RESULT);
        assertThat(module.parseExcelByUrl("https://example.com/template.xlsx", null)).isEqualTo(RecordingTemplateParser.EXCEL_URL_RESULT);
        assertThat(module.parseHtmlByData(new byte[]{3}, null)).isEqualTo(RecordingTemplateParser.HTML_DATA_RESULT);
        assertThat(module.parseHtmlByUrl("https://example.com/template.html", null)).isEqualTo(RecordingTemplateParser.HTML_URL_RESULT);
    }

    @Test
    void defaultConstructorStillCreatesUsableModuleInstance() {
        assertThatCode(TemplateModule::new).doesNotThrowAnyException();
    }

    private static final class RecordingTemplateParser extends TemplateParser {
        private static final byte[] WORD_DATA_RESULT = new byte[]{11};
        private static final byte[] WORD_URL_RESULT = new byte[]{12};
        private static final byte[] EXCEL_DATA_RESULT = new byte[]{21};
        private static final byte[] EXCEL_URL_RESULT = new byte[]{22};
        private static final byte[] HTML_DATA_RESULT = new byte[]{31};
        private static final byte[] HTML_URL_RESULT = new byte[]{32};

        private boolean parseWordByUrlCalled;
        private boolean legacyParsWordByUrlCalled;
        private String lastUrl;
        private Map<String, Object> lastMap;

        private RecordingTemplateParser() {
            super(null, null, null);
        }

        @Override
        public byte[] parseWordByData(byte[] data, Map<String, Object> map) {
            return WORD_DATA_RESULT;
        }

        @Override
        public byte[] parseWordByUrl(String url, Map<String, Object> map) {
            this.parseWordByUrlCalled = true;
            this.lastUrl = url;
            this.lastMap = map;
            return WORD_URL_RESULT;
        }

        @Override
        public byte[] parsWordByUrl(String url, Map<String, Object> map) {
            this.legacyParsWordByUrlCalled = true;
            return new byte[]{99};
        }

        @Override
        public byte[] parseExcelByData(byte[] data, Map<String, Object> map) {
            return EXCEL_DATA_RESULT;
        }

        @Override
        public byte[] parseExcelByUrl(String url, Map<String, Object> map) {
            return EXCEL_URL_RESULT;
        }

        @Override
        public byte[] parseHtmlByData(byte[] data, Map<String, Object> map) {
            return HTML_DATA_RESULT;
        }

        @Override
        public byte[] parseHtmlByUrl(String url, Map<String, Object> map) {
            return HTML_URL_RESULT;
        }
    }
}
```

- [ ] **Step 2: 运行测试并确认 Word URL 委托失败**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TemplateModuleTest test
```
Expected:
- FAIL。
- `parseWordByUrlDelegatesToCorrectSpellingTemplateParserMethod` 应显示当前调用了 `parsWordByUrl` 或返回了 `{99}`。

- [ ] **Step 3: 修改 `TemplateModule.parseWordByUrl` 委托路径**

Modify only this method in `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModule.java`:

```java
public byte[] parseWordByUrl(String url, Map<String, Object> map)  {
    return this.templateParser.parseWordByUrl(url, map);
}
```

Do not change method name, parameters, return type, checked exception declarations on other methods, or `@MagicModule("template")`.

- [ ] **Step 4: 运行 Magic-API 模块测试**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TemplateModuleTest test
```
Expected:
- PASS。
- `parseWordByUrlCalled == true`，`legacyParsWordByUrlCalled == false`。

- [ ] **Step 5: 定向勾选并提交 Task 4**

Update `openspec/changes/optimize-fw-plus-template/tasks.md` by checking only:

```markdown
- [x] 4.1 将 `TemplateModule.parseWordByUrl` 委托到 `TemplateParser.parseWordByUrl` 正确拼写入口。
- [x] 4.2 确认 Magic-API 现有 Word / Excel / HTML data 与 URL 解析方法签名保持兼容。
- [x] 4.3 确认 `TemplateModule` 在注入 `TemplateParser` 和默认构造两种路径下行为可预期。
- [x] 5.4 为 Magic-API `TemplateModule` 增加或调整测试，覆盖 `parseWordByUrl` 委托和现有方法兼容性。
```

Run:
```bash
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModule.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModuleTest.java openspec/changes/optimize-fw-plus-template/tasks.md
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework commit -m "test(template): route magic api word url parsing"
```
Expected:
- Commit succeeds。
- 不包含 Core 或 Boot 自动配置条件的额外改动。

## Task 5: 分层验证、诊断路径回归与 OpenSpec 收口

**Files:**
- Modify: `openspec/changes/optimize-fw-plus-template/tasks.md`
- No application source file changes expected in this task. If verification reveals a code defect, stop this task, load `superpowers:systematic-debugging`, root-cause the failure, then create a focused fix task before returning here.

**Interfaces:**
- Consumes:
  - Task 1–4 的 commits
  - Core tests: `TemplateParserTest`, `ParserConversionBoundaryTest`
  - Boot tests: `TemplateParserConfigurationTest`, `TemplateModuleTest`
- Produces:
  - Core → Boot 验证证据
  - OpenSpec 任务 2.2、5.5、6.1、6.2、6.3 勾选
  - 不修改 Maven 坐标、模块结构、包名或底层模板依赖版本的回归确认

- [ ] **Step 1: 运行 Core 目标测试**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-template -Dtest=TemplateParserTest,ParserConversionBoundaryTest test
```
Expected:
- PASS。
- 覆盖 `TemplateParser` 委托、兼容入口、缺 parser、非法输入、异常包装、可选转换器缺失。

- [ ] **Step 2: 运行 Core 目标模块编译或安装**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-template -am install -DskipTests
```
Expected:
- SUCCESS。
- 该步骤确保 Boot 层随后使用本地 Maven 仓库中的最新 Core 产物。

- [ ] **Step 3: 运行 Boot 目标测试**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TemplateParserConfigurationTest,TemplateModuleTest test
```
Expected:
- PASS。
- 覆盖默认 Bean 创建、用户 Bean 优先、缺 classpath 行为、`TemplateParser` 注入 parser Bean、Magic-API Java 层委托。

- [ ] **Step 4: 运行 Boot 目标模块编译**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am compile -DskipTests
```
Expected:
- SUCCESS。
- 不触发前端构建，因为只运行 compile。

- [ ] **Step 5: 检查诊断路径和兼容 API**

Run:
```bash
grep -R "parseWordByUrl" /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-template/src/main/java /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModule.java
grep -R "parsWordByUrl" /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-template/src/main/java /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModule.java
grep -R "Failed to parse Word template from data\|Failed to parse Word template from URL\|Failed to parse Excel template from data\|Failed to parse Excel template from URL\|Failed to parse HTML template from data\|Failed to parse HTML template from URL" /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/TemplateParser.java
```
Expected:
- `TemplateParser` 同时存在 `parseWordByUrl` 和 `parsWordByUrl`。
- `TemplateModule` 只委托 `parseWordByUrl`，不委托 `parsWordByUrl`。
- `TemplateParser` 中存在 6 条带模板类型与来源的失败消息。

- [ ] **Step 6: 检查 Maven 坐标、包名、依赖版本未漂移**

Run:
```bash
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework diff -- fw-plus-core/fw-plus-project/fw-plus-template/pom.xml fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework diff --name-only 7856512aa3855a8c0685440a8600afa7354b1721...HEAD
```
Expected:
- `fw-plus-template/pom.xml` 如有变化，仅包含 test scope 测试依赖。
- `fw-plus-boot-autoconfigure/pom.xml` 不应变化。
- 不出现 Maven 坐标、模块结构、Java package 重命名或底层模板依赖版本变更。

- [ ] **Step 7: 收口 OpenSpec tasks**

If Step 1–6 all pass, update `openspec/changes/optimize-fw-plus-template/tasks.md` by checking the remaining verification/regression items:

```markdown
- [x] 2.2 检查 Word 旧版 `.doc`、无效 docx、无效 HTML 模板等失败路径，确保异常信息可诊断。
- [x] 5.5 按 Core → Boot 顺序运行相关 Maven 编译/测试，并记录验证结果。
- [x] 6.1 检查本次变更未修改 Maven 坐标、模块结构、包名或底层模板依赖版本。
- [x] 6.2 检查公开 API 兼容性，确认旧 `parsWordByUrl` 仍可用。
- [x] 6.3 检查 OpenSpec 规格中的每个场景均有对应实现或测试验证路径。
```

Record the exact command outcomes in the commit message body if any command was skipped due to environment limitations.

- [ ] **Step 8: 提交验证收口**

Run:
```bash
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework add openspec/changes/optimize-fw-plus-template/tasks.md
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework commit -m "chore(template): verify parser optimization change"
```
Expected:
- Commit succeeds。
- 只包含 OpenSpec task 勾选或验证记录；没有新的应用源码改动。

## Final Verification Checklist

- [ ] `fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/TemplateParser.java` 提供 `parseWordByUrl`，并保留 `parsWordByUrl` 兼容委托。
- [ ] `TemplateParser` 对 Word / Excel / HTML 缺 parser、非法 data、非法 URL、底层异常均有明确标准异常语义。
- [ ] `WordParser`、`ExcelParser`、`HtmlParser` 可选转换器缺失时抛明确 `UnsupportedOperationException`。
- [ ] `TemplateParserConfiguration` 使用 `@ConditionalOnMissingBean` 表达用户 Bean 优先，并保留底层 classpath guard。
- [ ] `TemplateModule.parseWordByUrl` 委托到 `TemplateParser.parseWordByUrl`。
- [ ] Core 目标测试与 Boot 目标测试均通过。
- [ ] 未修改 Maven 坐标、模块结构、包名或底层模板依赖版本。
- [ ] `openspec/changes/optimize-fw-plus-template/tasks.md` 所有条目均有对应实现或验证证据。

## Self-Review

- Spec coverage: 本计划覆盖 Design Doc 第 3 节 Core API/校验/异常、第 4 节 Boot 自动配置、第 5 节 Magic-API 委托、第 6 节测试策略、第 7 节验证顺序和第 8 节风险缓解。
- Placeholder scan: 计划中未使用 TBD、TODO、implement later、fill in details；每个代码改动步骤均给出目标文件、代码片段、命令和期望结果。
- Type consistency: `parseWordByUrl` / `parsWordByUrl`、`WordAbstractParser` / `ExcelAbstractParser` / `HtmlAbstractParser`、`ApplicationContextRunner`、`TemplateModule` 方法签名在任务间保持一致。
