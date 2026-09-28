# LangChain4j 模型资源 Magic-API 插件实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 fw-plus-ai-boot-autoconfigure 新增七类 datasource 型 magic-api 插件（chat-model / streaming-chat-model / language-model / streaming-language-model / embedding-model / tokenizer / embedding-store），实例由动态注册器持有、脚本只读获取，前端在新建子工程 fw-plus-ai-magicapi-plugin-langchain4j 提供七个数据源组件。

**Architecture:** 每类资源一个 `MagicResourceStorage`（folder = datasource type）+ 一个 `AbstractMagicDynamicRegistry`（资源托管规则：先驱逐旧实例 → enabled 检查 → 显式工厂构建 → 成功才落库）；JSON = 公共字段（key/name/provider/enabled/+embeddingModelKey）+ options 自由 map；工厂按 provider 显式 builder 链（参照 core 层 AutoConfig 写法，core 层零修改）。

**Tech Stack:** Java 8、Spring Boot 2.7.18、magic-api 2.2.2（AbstractMagicDynamicRegistry/MagicResourceStorage/MagicPluginConfiguration）、langchain4j 0.35.0（社区版 dashscope 1.0.0-beta1）、JUnit 5、Vue3 + Vite（console）。

**Spec:** `docs/superpowers/specs/2026-09-28-langchain4j-magicapi-model-plugins-design.md`

## Global Constraints

- 所有对外命名（folder/type/插件标识/包名/类名/脚本模块）必须含 `langchain4j`，禁止 `uid` 残留。
- core 层 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-*/` 只做参考，**禁止任何改动**（含修饰符），验收含 git diff 为空审计。
- datasource JSON 结构：公共字段 `key/name/provider/enabled`（store 另加 `embeddingModelKey`）+ `options` 自由 map；公共字段提取到实体基类 `Langchain4jModelInfo`。
- 构建命令：在 `fw-plus-ai` 层执行；单测用 `./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=<TestClass> -nsu`（`-nsu` 禁止快照覆盖）；若本地仓库缺依赖先 `./mvnw install -DskipTests -nsu`。
- Java 8 兼容（无 var/record/text block）；Lombok `@Data` 可用；注释与异常信息用简体中文。
- 注册器遵循 CLAUDE.md「MQ 插件动态注册器」规则 A（register(MappingNode) 内构建，失败不落库）。
- 提交粒度：每任务一次 commit，message 用 `feat(ai):` / `test(ai):` 前缀。

## Review Focus

以下输入/失败模式没有单一任务能完全覆盖，按所属任务在各任务步骤中固化测试：

1. **options 键拼写错误**（如 `api-key` 写成 `apikey`）：构建期必须抛含完整路径 `langchain4j-chat-model[qwen-main].options.api-key` 的中文异常，而非 NPE —— Task 3、Task 6/7 的工厂测试固化。
2. **重复 register 覆盖**（同 key 保存两次）：第二次构建失败时旧实例必须已被驱逐（不能回滚到旧实例继续服务）—— Task 11 注册器测试固化。
3. **enabled=false 保存**：只清理不构建、不落库、不抛异常 —— Task 11。
4. **embedding-store 早于其 embedding-model 加载**：该次注册失败但启动完成钩子重试后恢复 —— Task 14。
5. **向量库实例驱逐时 close() 抛异常**：只记日志，不阻断后续 unregister 与其它 key —— Task 4、Task 11。
6. **未知 provider 短名**：validate 阶段即拒绝并在错误信息中列出该资源类型全部可用短名 —— Task 10。

---

### Task 1: 引入供应商 optional 依赖

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`

**Interfaces:**
- Consumes: 无
- Produces: autoconfigure 模块 classpath 上可用的 langchain4j 供应商模型/向量库类（Task 5-9 工厂 import 的前提）

- [ ] **Step 1: 在 pom.xml 的 dependencies 末尾追加 optional 依赖块**

```xml
        <!-- LangChain4j 模型资源插件：供应商模型与向量库（optional，版本由 langchain4j-bom 管理） -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai</artifactId>
            <scope>compile</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-anthropic</artifactId>
            <scope>compile</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-ollama</artifactId>
            <scope>compile</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-community-dashscope</artifactId>
            <scope>compile</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
            <scope>compile</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-redis</artifactId>
            <scope>compile</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-milvus</artifactId>
            <scope>compile</scope>
            <optional>true</optional>
        </dependency>
```

注意：`langchain4j-milvus` 已有的 test scope 依赖保留不动；若 `langchain4j`（主 jar，含 `InMemoryEmbeddingStore`）或 `langchain4j-community-dashscope` 未被现有 BOM 覆盖导致版本解析失败，按现有条目风格在 `fw-plus-ai-boot-dependencies/pom.xml` 补 `<dependencyManagement>` 条目（版本用本地仓库已存在的 0.35.0 / 1.0.0-beta1），并在 commit message 注明。

- [ ] **Step 2: 编译验证**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure compile -nsu -q`
Expected: BUILD SUCCESS（无 "Could not resolve" 错误）

- [ ] **Step 3: 确认 jar 已进入编译 classpath**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure dependency:tree -Dincludes=dev.langchain4j:langchain4j-open-ai,dev.langchain4j:langchain4j-community-dashscope,dev.langchain4j:langchain4j-milvus -nsu -q`
Expected: 三个 `dev.langchain4j:...:jar:...:compile` 条目

- [ ] **Step 4: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml
git commit -m "feat(ai): 引入 langchain4j 模型资源插件依赖（openai/anthropic/ollama/dashscope/主 jar/redis/milvus，optional）"
```

---

### Task 2: 实体基类与七类资源实体

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/model/Langchain4jModelInfo.java`
- Create: `.../model/Langchain4jChatModelInfo.java`
- Create: `.../model/Langchain4jStreamingChatModelInfo.java`
- Create: `.../model/Langchain4jLanguageModelInfo.java`
- Create: `.../model/Langchain4jStreamingLanguageModelInfo.java`
- Create: `.../model/Langchain4jEmbeddingModelInfo.java`
- Create: `.../model/Langchain4jTokenizerInfo.java`
- Create: `.../model/Langchain4jEmbeddingStoreInfo.java`
- Create: `.../model/Langchain4jModelTypes.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/model/Langchain4jModelInfoTest.java`

**Interfaces:**
- Consumes: `org.ssssssss.magicapi.core.model.MagicEntity`（magic-api 核心，含 id/groupId/name/updateTime 字段与无参构造）
- Produces:
  - `abstract class Langchain4jModelInfo extends MagicEntity`，字段 `String key`、`String name`、`String provider`、`boolean enabled = true`、`Map<String,Object> options`
  - `class Langchain4jEmbeddingStoreInfo extends Langchain4jModelInfo`，字段 `String embeddingModelKey`
  - `enum Langchain4jModelTypes`：`CHAT_MODEL("langchain4j-chat-model")`、`STREAMING_CHAT_MODEL("langchain4j-streaming-chat-model")`、`LANGUAGE_MODEL("langchain4j-language-model")`、`STREAMING_LANGUAGE_MODEL("langchain4j-streaming-language-model")`、`EMBEDDING_MODEL("langchain4j-embedding-model")`、`TOKENIZER("langchain4j-tokenizer")`、`EMBEDDING_STORE("langchain4j-embedding-store")`，提供 `String type()` 与 `static Langchain4jModelTypes of(String)`（未匹配抛 `IllegalArgumentException("不支持的资源类型: " + t)`）

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Langchain4jModelInfoTest {

    @Test
    void chatModelInfo_继承公共字段且默认值正确() {
        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        assertTrue(info.isEnabled());
        assertNull(info.getOptions());
    }

    @Test
    void embeddingStoreInfo_带embeddingModelKey() {
        Langchain4jEmbeddingStoreInfo info = new Langchain4jEmbeddingStoreInfo();
        info.setEmbeddingModelKey("qwen-embed");
        assertEquals("qwen-embed", info.getEmbeddingModelKey());
    }

    @Test
    void modelTypes_七个类型串含langchain4j前缀() {
        assertEquals(7, Langchain4jModelTypes.values().length);
        for (Langchain4jModelTypes t : Langchain4jModelTypes.values()) {
            assertTrue(t.type().startsWith("langchain4j-"), t.type());
        }
        assertEquals(Langchain4jModelTypes.CHAT_MODEL, Langchain4jModelTypes.of("langchain4j-chat-model"));
        assertThrows(IllegalArgumentException.class, () -> Langchain4jModelTypes.of("langchain4j-unknown"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jModelInfoTest -nsu -q`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 写实现**

`Langchain4jModelInfo.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.Map;

/**
 * LangChain4j 模型资源公共字段：七类 datasource 共享的登记信息。
 * 连接/生成参数不在此列，统一放 options（各 provider 内容不一致）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Langchain4jModelInfo extends MagicEntity {
    /** 唯一 key，脚本按 key 取实例；仅允许 [\w.-] */
    private String key;
    /** 展示名，缺省取 key */
    private String name;
    /** provider 短名，须命中该类资源短名表 */
    private String provider;
    /** false：销毁实例且不注册（下线而非删除） */
    private boolean enabled = true;
    /** 差异参数对象：不同 provider 的键集合不同 */
    private Map<String, Object> options;
}
```

另六个模型类均为（以 chat 为例，其余替换类名）：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** chat-model 资源：定义一个 ChatLanguageModel 实例。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jChatModelInfo extends Langchain4jModelInfo {
}
```

`Langchain4jEmbeddingStoreInfo.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** embedding-store 资源：定义一个 EmbeddingStore<TextSegment> 实例，必须搭配一个 embedding-model。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jEmbeddingStoreInfo extends Langchain4jModelInfo {
    /** 必填：引用的 embedding-model key */
    private String embeddingModelKey;
}
```

`Langchain4jModelTypes.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model;

/** 七类模型资源的类型常量：同时作为 datasource type 与 storage folder。 */
public enum Langchain4jModelTypes {
    CHAT_MODEL("langchain4j-chat-model"),
    STREAMING_CHAT_MODEL("langchain4j-streaming-chat-model"),
    LANGUAGE_MODEL("langchain4j-language-model"),
    STREAMING_LANGUAGE_MODEL("langchain4j-streaming-language-model"),
    EMBEDDING_MODEL("langchain4j-embedding-model"),
    TOKENIZER("langchain4j-tokenizer"),
    EMBEDDING_STORE("langchain4j-embedding-store");

    private final String type;

    Langchain4jModelTypes(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public static Langchain4jModelTypes of(String type) {
        for (Langchain4jModelTypes t : values()) {
            if (t.type.equals(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("不支持的资源类型: " + type);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jModelInfoTest -nsu -q`
Expected: Tests run: 3, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/model fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/model
git commit -m "feat(ai): 新增 LangChain4j 模型资源实体与类型常量（公共字段 + store 引用字段）"
```

### Task 3: Langchain4jOptionsReader（options 强类型读取）

**Files:**
- Create: `.../magicapi/modules/langchain4j/support/Langchain4jOptionsReader.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/support/Langchain4jOptionsReaderTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `class Langchain4jOptionsReader`，构造 `Langchain4jOptionsReader(String type, String key, Map<String,Object> options)`
  - `String required(String name)`、`String string(String name, String def)`、`int integer(String name, Integer def)`（键缺失返回 `def==null?0:def`）、`Float floatValue(String name, Float def)`（qwen 的 Float 型参数用）、`long longValue(String name, Long def)`、`double dbl(String name, Double def)`、`Boolean bool(String name, Boolean def)`（可空返回，供 builder 的 `Boolean` 形参区分未配置）、`<E extends Enum<E>> E enumValue(String name, Class<E> type, E def)`（大小写不敏感）、`Duration duration(String name, Duration def)`（支持 `60s`/`2m`/`PT1M`/毫秒数字）、`List<String> stringList(String name, List<String> def)`（`List` 原样或字符串 `,` 拆分）、`boolean has(String name)`、`Set<String> names()`
  - 错误路径格式：`langchain4j-chat-model[qwen-main].options.api-key`

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class Langchain4jOptionsReaderTest {

    private Langchain4jOptionsReader reader() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("api-key", "sk-x");
        options.put("temperature", 0.7);
        options.put("max-tokens", 2048);
        options.put("timeout", "60s");
        options.put("log-requests", true);
        options.put("stops", "停,词");
        return new Langchain4jOptionsReader("chat-model", "qwen-main", options);
    }

    @Test
    void required_存在时返回值() {
        assertEquals("sk-x", reader().required("api-key"));
    }

    @Test
    void required_缺失抛含完整路径的中文异常() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> reader().required("base-url"));
        assertTrue(ex.getMessage().contains("langchain4j-chat-model[qwen-main].options.base-url"), ex.getMessage());
    }

    @Test
    void required_空白串视为缺失() {
        Map<String, Object> options = new HashMap<>();
        options.put("api-key", "  ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new Langchain4jOptionsReader("chat-model", "k", options).required("api-key"));
        assertTrue(ex.getMessage().contains("api-key"), ex.getMessage());
    }

    @Test
    void 类型转换_数值布尔时长与列表() {
        Langchain4jOptionsReader r = reader();
        assertEquals(0.7, r.dbl("temperature", null), 0.0001);
        assertEquals(2048, r.integer("max-tokens", null));
        assertEquals(Boolean.TRUE, r.bool("log-requests", null));
        assertEquals(Duration.ofSeconds(60), r.duration("timeout", null));
        assertEquals(Arrays.asList("停", "词"), r.stringList("stops", null));
        assertEquals(0.7f, r.floatValue("temperature", null), 0.001f);
    }

    @Test
    void bool_未配置返回可空默认值() {
        Langchain4jOptionsReader r = reader();
        assertNull(r.bool("log-responses", null));
        assertFalse(r.bool("log-responses", false));
    }

    @Test
    void 缺省值_键不存在时返回默认且不抛错() {
        Langchain4jOptionsReader r = reader();
        assertEquals("dft", r.string("base-url", "dft"));
        assertEquals(3, r.integer("max-retries", 3));
        assertFalse(r.has("base-url"));
        assertTrue(r.has("api-key"));
    }

    @Test
    void 数值超范围报错() {
        Map<String, Object> options = new HashMap<>();
        options.put("max-tokens", 99999999999L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new Langchain4jOptionsReader("chat-model", "k", options).integer("max-tokens", null));
        assertTrue(ex.getMessage().contains("超出 int 范围"), ex.getMessage());
    }

    @Test
    void duration_支持多种写法() {
        Map<String, Object> o1 = new HashMap<>(); o1.put("timeout", "2m");
        Map<String, Object> o2 = new HashMap<>(); o2.put("timeout", "PT1M");
        Map<String, Object> o3 = new HashMap<>(); o3.put("timeout", 30000);
        assertEquals(Duration.ofMinutes(2), new Langchain4jOptionsReader("t", "k", o1).duration("timeout", null));
        assertEquals(Duration.ofMinutes(1), new Langchain4jOptionsReader("t", "k", o2).duration("timeout", null));
        assertEquals(Duration.ofMillis(30000), new Langchain4jOptionsReader("t", "k", o3).duration("timeout", null));
    }

    @Test
    void enumValue_大小写不敏感匹配() {
        Map<String, Object> o = new HashMap<>(); o.put("consistency-level", "strong");
        assertEquals(TimeUnit.SECONDS,
            new Langchain4jOptionsReader("t", "k", o).enumValue("consistency-level", TimeUnit.class, null));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jOptionsReaderTest -nsu -q`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 写实现**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support;

import java.time.Duration;
import java.util.*;

/**
 * options 差异参数的强类型读取：封装风格对齐 MagicAPIConfigUtils（集中读取 + 默认值 + 缺失报错带完整路径）。
 * options 中不同 provider 的键集合不同；各工厂按 builder 需要的键取值。
 */
public class Langchain4jOptionsReader {

    private final String type;
    private final String key;
    private final Map<String, Object> options;

    public Langchain4jOptionsReader(String type, String key, Map<String, Object> options) {
        this.type = type;
        this.key = key;
        this.options = options == null ? Collections.<String, Object>emptyMap() : options;
    }

    public boolean has(String name) {
        return options.get(name) != null;
    }

    public Set<String> names() {
        return options.keySet();
    }

    public String required(String name) {
        String value = string(name, null);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(path(name) + " 不能为空（请在 datasource options 中配置）");
        }
        return value;
    }

    public String string(String name, String defaultValue) {
        Object value = options.get(name);
        return value == null ? defaultValue : String.valueOf(value);
    }

    public int integer(String name, Integer defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue == null ? 0 : defaultValue;
        }
        long parsed = toLong(name, value);
        if (parsed > Integer.MAX_VALUE || parsed < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(path(name) + " 超出 int 范围: " + parsed);
        }
        return (int) parsed;
    }

    public long longValue(String name, Long defaultValue) {
        Object value = options.get(name);
        return value == null ? (defaultValue == null ? 0L : defaultValue) : toLong(name, value);
    }

    public double dbl(String name, Double defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue == null ? 0d : defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(path(name) + " 不是合法数字: " + value);
        }
    }

    public Float floatValue(String name, Float defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(path(name) + " 不是合法数字: " + value);
        }
    }

    public Boolean bool(String name, Boolean defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.parseBoolean(text);
        }
        throw new IllegalArgumentException(path(name) + " 不是合法布尔值: " + value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends Enum<E>> E enumValue(String name, Class<E> type, E defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(path(name) + " 不是合法枚举值，可选: " + Arrays.toString(type.getEnumConstants()));
    }

    public Duration duration(String name, Duration defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Duration) {
            return (Duration) value;
        }
        String text = String.valueOf(value).trim();
        try {
            if (text.startsWith("PT") || text.startsWith("pt")) {
                return Duration.parse(text);
            }
            if (text.matches("\\d+")) {
                return Duration.ofMillis(Long.parseLong(text));
            }
            String unit = text.substring(text.length() - 1);
            long amount = Long.parseLong(text.substring(0, text.length() - 1).trim());
            switch (unit) {
                case "s": return Duration.ofSeconds(amount);
                case "m": return Duration.ofMinutes(amount);
                case "h": return Duration.ofHours(amount);
                case "d": return Duration.ofDays(amount);
                default: throw new NumberFormatException(text);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(path(name) + " 不是合法时长（支持 60s/2m/PT1M/毫秒）: " + value);
        }
    }

    public List<String> stringList(String name, List<String> defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue == null ? Collections.<String>emptyList() : defaultValue;
        }
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        String text = String.valueOf(value);
        if (text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(text.split("\\s*,\\s*")));
    }

    private long toLong(String name, Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(path(name) + " 不是合法整数: " + value);
        }
    }

    private String path(String name) {
        return "langchain4j-" + type + "[" + key + "].options." + name;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jOptionsReaderTest -nsu -q`
Expected: Tests run: 9, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/support/Langchain4jOptionsReader.java fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/support/Langchain4jOptionsReaderTest.java
git commit -m "feat(ai): 新增 Langchain4jOptionsReader（options 强类型读取，缺失报错含完整路径）"
```

---

### Task 4: Langchain4jModelHolder（实例持有与驱逐）

**Files:**
- Create: `.../magicapi/modules/langchain4j/support/Langchain4jModelHolder.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/support/Langchain4jModelHolderTest.java`

**Interfaces:**
- Consumes: Task 2 的 `Langchain4jModelTypes`
- Produces:
  - `class Langchain4jModelHolder`：`void put(Langchain4jModelTypes type, String key, Object instance)`、`<T> T get(Langchain4jModelTypes type, String key)`（未命中返回 null）、`void evict(Langchain4jModelTypes type, String key)`（`Closeable` 实例 close，异常仅记日志）、`Map<String,Object> snapshot(Langchain4jModelTypes type)`（只读）、`Set<String> keys(Langchain4jModelTypes type)`、`void clear()`

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jModelTypes;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Langchain4jModelHolderTest {

    @Test
    void put后get可取到() {
        Langchain4jModelHolder holder = new Langchain4jModelHolder();
        Object model = new Object();
        holder.put(Langchain4jModelTypes.CHAT_MODEL, "qwen-main", model);
        assertSame(model, holder.get(Langchain4jModelTypes.CHAT_MODEL, "qwen-main"));
        assertNull(holder.get(Langchain4jModelTypes.CHAT_MODEL, "absent"));
    }

    @Test
    void evict会close可关闭实例() throws Exception {
        Langchain4jModelHolder holder = new Langchain4jModelHolder();
        TrackingCloseable store = new TrackingCloseable();
        holder.put(Langchain4jModelTypes.EMBEDDING_STORE, "milvus-main", store);
        holder.evict(Langchain4jModelTypes.EMBEDDING_STORE, "milvus-main");
        assertTrue(store.closed);
        assertNull(holder.get(Langchain4jModelTypes.EMBEDDING_STORE, "milvus-main"));
    }

    @Test
    void evict时close抛异常不阻断后续驱逐() {
        Langchain4jModelHolder holder = new Langchain4jModelHolder();
        holder.put(Langchain4jModelTypes.EMBEDDING_STORE, "bad", new Closeable() {
            @Override public void close() throws IOException { throw new IOException("boom"); }
        });
        Object good = new Object();
        holder.put(Langchain4jModelTypes.EMBEDDING_STORE, "good", good);
        assertDoesNotThrow(() -> holder.evict(Langchain4jModelTypes.EMBEDDING_STORE, "bad"));
        assertDoesNotThrow(() -> holder.evict(Langchain4jModelTypes.EMBEDDING_STORE, "good"));
        assertNull(holder.get(Langchain4jModelTypes.EMBEDDING_STORE, "good"));
    }

    @Test
    void clear清空全部类型() {
        Langchain4jModelHolder holder = new Langchain4jModelHolder();
        holder.put(Langchain4jModelTypes.CHAT_MODEL, "a", new Object());
        holder.put(Langchain4jModelTypes.EMBEDDING_STORE, "b", new Object());
        holder.clear();
        assertEquals(0, holder.snapshot(Langchain4jModelTypes.CHAT_MODEL).size());
        assertEquals(0, holder.snapshot(Langchain4jModelTypes.EMBEDDING_STORE).size());
    }

    @Test
    void snapshot只读且同类多实例并存() {
        Langchain4jModelHolder holder = new Langchain4jModelHolder();
        holder.put(Langchain4jModelTypes.CHAT_MODEL, "a", new Object());
        holder.put(Langchain4jModelTypes.CHAT_MODEL, "b", new Object());
        Map<String, Object> snapshot = holder.snapshot(Langchain4jModelTypes.CHAT_MODEL);
        assertEquals(2, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("c", new Object()));
    }

    private static class TrackingCloseable implements Closeable {
        boolean closed;
        @Override public void close() throws IOException { closed = true; }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jModelHolderTest -nsu -q`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 写实现**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jModelTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 七类模型实例的持有中心：动态注册器写入/驱逐，脚本侧 Langchain4jModule 只读获取。
 * 对实现 Closeable 的实例（向量库客户端）驱逐时关闭，异常仅记日志不阻断。
 */
public class Langchain4jModelHolder {

    private static final Logger logger = LoggerFactory.getLogger(Langchain4jModelHolder.class);

    private final Map<Langchain4jModelTypes, Map<String, Object>> registry =
            new EnumMap<Langchain4jModelTypes, Map<String, Object>>(Langchain4jModelTypes.class);

    public Langchain4jModelHolder() {
        for (Langchain4jModelTypes type : Langchain4jModelTypes.values()) {
            registry.put(type, new ConcurrentHashMap<String, Object>());
        }
    }

    public void put(Langchain4jModelTypes type, String key, Object instance) {
        map(type).put(key, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Langchain4jModelTypes type, String key) {
        return (T) map(type).get(key);
    }

    public void evict(Langchain4jModelTypes type, String key) {
        Object removed = map(type).remove(key);
        if (removed instanceof Closeable) {
            try {
                ((Closeable) removed).close();
            } catch (Exception e) {
                logger.warn("关闭 LangChain4j 实例失败: type={}, key={}", type.type(), key, e);
            }
        }
    }

    public Map<String, Object> snapshot(Langchain4jModelTypes type) {
        return Collections.unmodifiableMap(map(type));
    }

    public Set<String> keys(Langchain4jModelTypes type) {
        return map(type).keySet();
    }

    public void clear() {
        for (Langchain4jModelTypes type : Langchain4jModelTypes.values()) {
            for (String key : map(type).keySet()) {
                evict(type, key);
            }
            map(type).clear();
        }
    }

    private Map<String, Object> map(Langchain4jModelTypes type) {
        return registry.get(type);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jModelHolderTest -nsu -q`
Expected: Tests run: 5, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/support/Langchain4jModelHolder.java fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/support/Langchain4jModelHolderTest.java
git commit -m "feat(ai): 新增 Langchain4jModelHolder（七类实例持有，驱逐时 close 容错）"
```

### Task 5: ModelFactory SPI 与 FactoryRegistry（短名分发）

**Files:**
- Create: `.../magicapi/modules/langchain4j/factory/ModelFactory.java`
- Create: `.../magicapi/modules/langchain4j/factory/Langchain4jModelFactoryRegistry.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/Langchain4jModelFactoryRegistryTest.java`

**Interfaces:**
- Consumes: Task 2（实体/类型）、Task 3（OptionsReader）
- Produces:
  - `interface ModelFactory`：`String provider()`；七个默认方法：`ChatLanguageModel buildChatModel(Langchain4jOptionsReader o)`、`StreamingChatLanguageModel buildStreamingChatModel(o)`、`LanguageModel buildLanguageModel(o)`、`StreamingLanguageModel buildStreamingLanguageModel(o)`、`EmbeddingModel buildEmbeddingModel(o)`、`Tokenizer buildTokenizer(o)`、`EmbeddingStore<TextSegment> buildEmbeddingStore(o, EmbeddingModel embeddingModel)`；默认抛 `UnsupportedOperationException("provider " + provider() + " 不支持该资源类型")`
  - `class Langchain4jModelFactoryRegistry`：`void register(ModelFactory factory)`、`Object build(Langchain4jModelTypes type, Langchain4jModelInfo info)`（等价 `build(type, info, null)`）、`Object build(Langchain4jModelTypes type, Langchain4jModelInfo info, EmbeddingModel embeddingModel)`、`boolean supports(Langchain4jModelTypes type, String provider)`、`Set<String> providers(Langchain4jModelTypes type)`

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.*;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Langchain4jModelFactoryRegistryTest {

    /** 最小工厂：仅 chat 一种能力 */
    static class FakeFactory implements ModelFactory {
        @Override public String provider() { return "fake"; }
        @Override public ChatLanguageModel buildChatModel(Langchain4jOptionsReader o) {
            return OpenAiChatModel.builder().apiKey("k").modelName("m").build();
        }
    }

    /** 最小向量库工厂：仅 store 一种能力 */
    static class FakeStoreFactory implements ModelFactory {
        @Override public String provider() { return "mem"; }
        @Override public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel m) {
            assertEquals("dim-123", o.string("dimension", null));
            assertNotNull(m);
            return new InMemoryEmbeddingStore<TextSegment>();
        }
    }

    @Test
    void 注册后按类型分发构建() {
        Langchain4jModelFactoryRegistry registry = new Langchain4jModelFactoryRegistry();
        registry.register(new FakeFactory());
        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        info.setKey("k1");
        info.setProvider("fake");
        Object model = registry.build(Langchain4jModelTypes.CHAT_MODEL, info);
        assertTrue(model instanceof OpenAiChatModel);
        assertTrue(registry.supports(Langchain4jModelTypes.CHAT_MODEL, "fake"));
        assertFalse(registry.supports(Langchain4jModelTypes.TOKENIZER, "fake"));
        assertEquals(1, registry.providers(Langchain4jModelTypes.CHAT_MODEL).size());
    }

    @Test
    void 未注册provider抛含可用短名的异常() {
        Langchain4jModelFactoryRegistry registry = new Langchain4jModelFactoryRegistry();
        registry.register(new FakeFactory());
        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        info.setKey("k1");
        info.setProvider("ghost");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> registry.build(Langchain4jModelTypes.CHAT_MODEL, info));
        assertTrue(ex.getMessage().contains("ghost"), ex.getMessage());
        assertTrue(ex.getMessage().contains("fake"), ex.getMessage());
    }

    @Test
    void 工厂不支持的类型抛Unsupported() {
        Langchain4jModelFactoryRegistry registry = new Langchain4jModelFactoryRegistry();
        registry.register(new FakeFactory());
        Langchain4jTokenizerInfo info = new Langchain4jTokenizerInfo();
        info.setKey("k1");
        info.setProvider("fake");
        assertThrows(UnsupportedOperationException.class,
            () -> registry.build(Langchain4jModelTypes.TOKENIZER, info));
    }

    @Test
    void store构建注入embeddingModel() {
        Langchain4jModelFactoryRegistry registry = new Langchain4jModelFactoryRegistry();
        registry.register(new FakeStoreFactory());
        Langchain4jEmbeddingStoreInfo info = new Langchain4jEmbeddingStoreInfo();
        info.setKey("s1");
        info.setProvider("mem");
        Map<String, Object> options = new HashMap<>();
        options.put("dimension", "dim-123");
        info.setOptions(options);
        EmbeddingModel embeddingModel = new EmbeddingModel() {
            @Override public dev.langchain4j.model.output.Response<dev.langchain4j.data.embedding.Embedding> embedAll(
                    java.util.List<TextSegment> list) {
                throw new UnsupportedOperationException();
            }
            @Override public int dimension() { return 123; }
        };
        assertNotNull(registry.build(Langchain4jModelTypes.EMBEDDING_STORE, info, embeddingModel));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jModelFactoryRegistryTest -nsu -q`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 写实现**

`ModelFactory.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * 单家 provider 的模型构建工厂：每个短名对应一个实现（如 openai、qwen）。
 * 参数一律经 Langchain4jOptionsReader 从 datasource options 读取；
 * builder 链写法参照 core 层 fw-plus-ai-langchain4j-* 的 AutoConfig（只参考，不修改）。
 */
public interface ModelFactory {

    /** provider 短名，如 "openai" */
    String provider();

    default ChatLanguageModel buildChatModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default StreamingChatLanguageModel buildStreamingChatModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default LanguageModel buildLanguageModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default StreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default Tokenizer buildTokenizer(Langchain4jOptionsReader o) { throw unsupported(); }

    default EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        throw unsupported();
    }

    default UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("provider " + provider() + " 不支持该资源类型");
    }
}
```

`Langchain4jModelFactoryRegistry.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * provider 短名 → 工厂的分发表：每类资源一张短名集合，表外短名构建期拒绝。
 */
public class Langchain4jModelFactoryRegistry {

    private static final Langchain4jOptionsReader PROBE =
            new Langchain4jOptionsReader("probe", "probe", Collections.<String, Object>emptyMap());

    private final Map<String, ModelFactory> factories = new LinkedHashMap<String, ModelFactory>();

    public void register(ModelFactory factory) {
        factories.put(factory.provider(), factory);
    }

    public boolean supports(Langchain4jModelTypes type, String provider) {
        ModelFactory factory = factories.get(provider);
        return factory != null && factorySupports(type, factory);
    }

    public Set<String> providers(Langchain4jModelTypes type) {
        Set<String> result = new LinkedHashSet<String>();
        for (ModelFactory factory : factories.values()) {
            if (factorySupports(type, factory)) {
                result.add(factory.provider());
            }
        }
        return result;
    }

    public Object build(Langchain4jModelTypes type, Langchain4jModelInfo info) {
        return build(type, info, null);
    }

    public Object build(Langchain4jModelTypes type, Langchain4jModelInfo info, EmbeddingModel embeddingModel) {
        ModelFactory factory = factories.get(info.getProvider());
        if (factory == null) {
            throw new IllegalArgumentException(String.format(
                "资源 %s[%s] 的 provider '%s' 未注册，可用 provider: %s",
                type.type(), info.getKey(), info.getProvider(), providers(type)));
        }
        Langchain4jOptionsReader reader =
                new Langchain4jOptionsReader(type.type(), info.getKey(), info.getOptions());
        switch (type) {
            case CHAT_MODEL:
                return factory.buildChatModel(reader);
            case STREAMING_CHAT_MODEL:
                return factory.buildStreamingChatModel(reader);
            case LANGUAGE_MODEL:
                return factory.buildLanguageModel(reader);
            case STREAMING_LANGUAGE_MODEL:
                return factory.buildStreamingLanguageModel(reader);
            case EMBEDDING_MODEL:
                return factory.buildEmbeddingModel(reader);
            case TOKENIZER:
                return factory.buildTokenizer(reader);
            case EMBEDDING_STORE:
                return factory.buildEmbeddingStore(reader, embeddingModel);
            default:
                throw new IllegalArgumentException("不支持的资源类型: " + type);
        }
    }

    /**
     * 能力探测：用空 options 调一次对应方法，UnsupportedOperationException 表示未实现；
     * 其它运行时异常（如 required 缺失）说明能力已实现，仅探测参数不足。
     */
    private boolean factorySupports(Langchain4jModelTypes type, ModelFactory factory) {
        try {
            switch (type) {
                case CHAT_MODEL: factory.buildChatModel(PROBE); break;
                case STREAMING_CHAT_MODEL: factory.buildStreamingChatModel(PROBE); break;
                case LANGUAGE_MODEL: factory.buildLanguageModel(PROBE); break;
                case STREAMING_LANGUAGE_MODEL: factory.buildStreamingLanguageModel(PROBE); break;
                case EMBEDDING_MODEL: factory.buildEmbeddingModel(PROBE); break;
                case TOKENIZER: factory.buildTokenizer(PROBE); break;
                case EMBEDDING_STORE: factory.buildEmbeddingStore(PROBE, null); break;
                default: return false;
            }
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jModelFactoryRegistryTest -nsu -q`
Expected: Tests run: 4, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory
git commit -m "feat(ai): 新增 ModelFactory SPI 与短名分发注册表"
```

---

### Task 6: OpenAI 工厂（六类能力）

**Files:**
- Create: `.../magicapi/modules/langchain4j/factory/openai/Langchain4jOpenAiModelFactory.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/openai/Langchain4jOpenAiModelFactoryTest.java`

**Interfaces:**
- Consumes: Task 5（ModelFactory）、Task 3（OptionsReader）
- Produces: `provider() = "openai"`，六类资源全部支持；options 键（kebab-case）：`api-key`/`base-url`/`organization-id`/`model-name`/`temperature`(默认 0.7)/`top-p`/`stop`/`max-tokens`/`presence-penalty`/`frequency-penalty`/`seed`/`user`/`timeout`/`max-retries`/`log-requests`/`log-responses`
- 工具约定：`Langchain4jOpenAiModelFactory.nullIfZero(Integer)`——reader 的 `integer(name,null)` 键缺失返回 0，工厂对 OpenAi* 的 `Integer` 形参统一 `nullIfZero` 归一

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.openai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Langchain4jOpenAiModelFactoryTest {

    private Langchain4jOptionsReader options() {
        Map<String, Object> options = new HashMap<>();
        options.put("api-key", "sk-test");
        options.put("model-name", "gpt-4o");
        return new Langchain4jOptionsReader("chat-model", "openai-main", options);
    }

    @Test
    void 六类资源全部可构建() {
        ModelFactory factory = new Langchain4jOpenAiModelFactory();
        assertNotNull(factory.buildChatModel(options()));
        assertNotNull(factory.buildStreamingChatModel(options()));
        assertNotNull(factory.buildLanguageModel(options()));
        assertNotNull(factory.buildStreamingLanguageModel(options()));
        assertNotNull(factory.buildEmbeddingModel(options()));
        assertNotNull(factory.buildTokenizer(options()));
    }

    @Test
    void apiKey缺失抛含完整路径异常() {
        ModelFactory factory = new Langchain4jOpenAiModelFactory();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> factory.buildChatModel(new Langchain4jOptionsReader("chat-model", "openai-main", new HashMap<>())));
        assertTrue(ex.getMessage().contains("langchain4j-chat-model[openai-main].options.api-key"), ex.getMessage());
    }

    @Test
    void provider短名固定为openai() {
        assertEquals("openai", new Langchain4jOpenAiModelFactory().provider());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jOpenAiModelFactoryTest -nsu -q`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 写实现**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.openai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingLanguageModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;

/**
 * OpenAI 工厂：dev.langchain4j.model.openai.* builder 链。
 * 仅构建实例，不发起网络调用；测试与 Controller test 分别验证。
 */
public class Langchain4jOpenAiModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public OpenAiChatModel buildChatModel(Langchain4jOptionsReader o) {
        return OpenAiChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .organizationId(o.string("organization-id", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .stop(o.stringList("stop", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public OpenAiStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .organizationId(o.string("organization-id", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .stop(o.stringList("stop", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public OpenAiLanguageModel buildLanguageModel(Langchain4jOptionsReader o) {
        return OpenAiLanguageModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .organizationId(o.string("organization-id", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .stop(o.stringList("stop", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public OpenAiStreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) {
        return OpenAiStreamingLanguageModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .organizationId(o.string("organization-id", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .stop(o.stringList("stop", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public OpenAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .organizationId(o.string("organization-id", null))
                .modelName(o.required("model-name"))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public Tokenizer buildTokenizer(Langchain4jOptionsReader o) {
        // OpenAiTokenizer 无 builder，单参构造
        return new OpenAiTokenizer(o.required("api-key"));
    }

    /** reader 的 integer(name,null) 键缺失返回 0；对 Integer 形参归零为 null 表示未配置 */
    static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jOpenAiModelFactoryTest -nsu -q`
Expected: Tests run: 3, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/openai fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/openai
git commit -m "feat(ai): 新增 OpenAI 模型工厂（chat/streaming/language/embedding/tokenizer 六类）"
```

### Task 7: 通义千问（dashscope）工厂（六类能力）

**Files:**
- Create: `.../magicapi/modules/langchain4j/factory/qwen/Langchain4jQwenModelFactory.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/qwen/Langchain4jQwenModelFactoryTest.java`

**Interfaces:**
- Consumes: Task 5、Task 3（`floatValue`）
- Produces: `provider() = "qwen"`，六类资源全部支持；默认值：`base-url`=`https://dashscope.aliyuncs.com/compatible-mode/v1`、`model-name`=`qwen-plus`（embedding 默认 `text-embedding-v3`）、`temperature`=0.7f、`enable-search`=false；qwen 特有键：`enable-search`/`top-k`/`repetition-penalty`（Float）；类包为 `dev.langchain4j.community.model.dashscope.*`（注意与 core 层反编译的 `dev.langchain4j.dashscope.spring.*` 不同）

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.qwen;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Langchain4jQwenModelFactoryTest {

    private Langchain4jOptionsReader options() {
        Map<String, Object> options = new HashMap<>();
        options.put("api-key", "sk-qwen");
        options.put("enable-search", true);
        options.put("top-k", 20);
        options.put("repetition-penalty", 1.1);
        return new Langchain4jOptionsReader("chat-model", "qwen-main", options);
    }

    @Test
    void 六类资源全部可构建() {
        ModelFactory factory = new Langchain4jQwenModelFactory();
        assertNotNull(factory.buildChatModel(options()));
        assertNotNull(factory.buildStreamingChatModel(options()));
        assertNotNull(factory.buildLanguageModel(options()));
        assertNotNull(factory.buildStreamingLanguageModel(options()));
        assertNotNull(factory.buildEmbeddingModel(options()));
        assertNotNull(factory.buildTokenizer(options()));
    }

    @Test
    void 默认baseUrl与modelName不依赖配置() {
        ModelFactory factory = new Langchain4jQwenModelFactory();
        assertNotNull(factory.buildChatModel(new Langchain4jOptionsReader("chat-model", "k", options())));
    }

    @Test
    void apiKey缺失抛含完整路径异常() {
        ModelFactory factory = new Langchain4jQwenModelFactory();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> factory.buildChatModel(new Langchain4jOptionsReader("chat-model", "qwen-main", new HashMap<>())));
        assertTrue(ex.getMessage().contains("langchain4j-chat-model[qwen-main].options.api-key"), ex.getMessage());
    }

    @Test
    void provider短名固定为qwen() {
        assertEquals("qwen", new Langchain4jQwenModelFactory().provider());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jQwenModelFactoryTest -nsu -q`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 写实现**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.qwen;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenLanguageModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingLanguageModel;
import dev.langchain4j.community.model.dashscope.QwenTokenizer;
import dev.langchain4j.model.Tokenizer;

import java.util.Collections;

/**
 * 通义千问（dashscope）工厂：dev.langchain4j.community.model.dashscope.Qwen* builder 链。
 * qwen 四个生成类 builder 签名一致：baseUrl/apiKey/modelName/temperature(Float)/topP/topK/
 * enableSearch/seed/repetitionPenalty(Float)/stops/maxTokens。
 */
public class Langchain4jQwenModelFactory implements ModelFactory {

    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final Float DEFAULT_TEMPERATURE = 0.7f;
    private static final String DEFAULT_MODEL = "qwen-plus";
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-v3";

    @Override
    public String provider() {
        return "qwen";
    }

    @Override
    public QwenChatModel buildChatModel(Langchain4jOptionsReader o) {
        return QwenChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.floatValue("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .enableSearch(o.bool("enable-search", false))
                .seed(nullIfZero(o.integer("seed", null)))
                .repetitionPenalty(o.floatValue("repetition-penalty", null))
                .stops(o.stringList("stops", Collections.<String>emptyList()))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .build();
    }

    @Override
    public QwenStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o) {
        return QwenStreamingChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.floatValue("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .enableSearch(o.bool("enable-search", false))
                .seed(nullIfZero(o.integer("seed", null)))
                .repetitionPenalty(o.floatValue("repetition-penalty", null))
                .stops(o.stringList("stops", Collections.<String>emptyList()))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .build();
    }

    @Override
    public QwenLanguageModel buildLanguageModel(Langchain4jOptionsReader o) {
        return QwenLanguageModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.floatValue("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .enableSearch(o.bool("enable-search", false))
                .seed(nullIfZero(o.integer("seed", null)))
                .repetitionPenalty(o.floatValue("repetition-penalty", null))
                .stops(o.stringList("stops", Collections.<String>emptyList()))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .build();
    }

    @Override
    public QwenStreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) {
        return QwenStreamingLanguageModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.floatValue("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .enableSearch(o.bool("enable-search", false))
                .seed(nullIfZero(o.integer("seed", null)))
                .repetitionPenalty(o.floatValue("repetition-penalty", null))
                .stops(o.stringList("stops", Collections.<String>emptyList()))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .build();
    }

    @Override
    public QwenEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return QwenEmbeddingModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_EMBEDDING_MODEL))
                .build();
    }

    @Override
    public Tokenizer buildTokenizer(Langchain4jOptionsReader o) {
        // QwenTokenizer(String apiKey, String modelName) —— 双参构造，无 builder
        return new QwenTokenizer(o.required("api-key"), o.string("model-name", DEFAULT_MODEL));
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jQwenModelFactoryTest -nsu -q`
Expected: Tests run: 4, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/qwen fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/qwen
git commit -m "feat(ai): 新增通义千问模型工厂（六类能力，含 enable-search/top-k/repetition-penalty 特有键）"
```

---

### Task 8: Ollama 与 Anthropic 工厂

**Files:**
- Create: `.../magicapi/modules/langchain4j/factory/ollama/Langchain4jOllamaModelFactory.java`
- Create: `.../magicapi/modules/langchain4j/factory/anthropic/Langchain4jAnthropicModelFactory.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/ollama/Langchain4jOllamaModelFactoryTest.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/anthropic/Langchain4jAnthropicModelFactoryTest.java`

**Interfaces:**
- Consumes: Task 5、Task 3
- Produces:
  - `provider() = "ollama"`：实现 chat / streaming-chat / embedding 三类；**不实现** language/streaming-language/tokenizer/store（ollama jar 无这些类）；默认 `base-url`=`http://localhost:11434`、`temperature`=0.7、`max-retries`=3、chat `model-name`=`qwen2.5`、embedding `model-name`=`nomic-embed-text`
  - `provider() = "anthropic"`：实现 chat / streaming-chat 两类；默认 `version`=`2023-06-01`、`max-tokens`=1024

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ollama;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class Langchain4jOllamaModelFactoryTest {

    private Langchain4jOptionsReader options() {
        return new Langchain4jOptionsReader("chat-model", "ollama-local", new HashMap<>());
    }

    @Test
    void 三类能力可构建() {
        Langchain4jOllamaModelFactory factory = new Langchain4jOllamaModelFactory();
        assertNotNull(factory.buildChatModel(options()));
        assertNotNull(factory.buildStreamingChatModel(options()));
        assertNotNull(factory.buildEmbeddingModel(options()));
    }

    @Test
    void 无tokenizer与language能力() {
        Langchain4jOllamaModelFactory factory = new Langchain4jOllamaModelFactory();
        assertThrows(UnsupportedOperationException.class, () -> factory.buildTokenizer(options()));
        assertThrows(UnsupportedOperationException.class, () -> factory.buildLanguageModel(options()));
    }

    @Test
    void provider短名固定为ollama() {
        assertEquals("ollama", new Langchain4jOllamaModelFactory().provider());
    }
}
```

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.anthropic;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Langchain4jAnthropicModelFactoryTest {

    @Test
    void 两类能力可构建() {
        Langchain4jAnthropicModelFactory factory = new Langchain4jAnthropicModelFactory();
        Map<String, Object> options = new HashMap<>();
        options.put("api-key", "sk-ant");
        options.put("model-name", "claude-sonnet-4-5");
        Langchain4jOptionsReader reader = new Langchain4jOptionsReader("chat-model", "claude", options);
        assertNotNull(factory.buildChatModel(reader));
        assertNotNull(factory.buildStreamingChatModel(reader));
    }

    @Test
    void embedding能力不支持() {
        Langchain4jAnthropicModelFactory factory = new Langchain4jAnthropicModelFactory();
        assertThrows(UnsupportedOperationException.class,
            () -> factory.buildEmbeddingModel(new Langchain4jOptionsReader("embedding-model", "k", new HashMap<>())));
    }

    @Test
    void apiKey缺失抛含完整路径异常() {
        Langchain4jAnthropicModelFactory factory = new Langchain4jAnthropicModelFactory();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> factory.buildChatModel(new Langchain4jOptionsReader("chat-model", "claude", new HashMap<>())));
        assertTrue(ex.getMessage().contains("langchain4j-chat-model[claude].options.api-key"), ex.getMessage());
    }

    @Test
    void provider短名固定为anthropic() {
        assertEquals("anthropic", new Langchain4jAnthropicModelFactory().provider());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest='Langchain4jOllamaModelFactoryTest,Langchain4jAnthropicModelFactoryTest' -nsu -q`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 写实现**

`Langchain4jOllamaModelFactory.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ollama;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

/**
 * Ollama 工厂：本地模型，无需 api-key。ollama jar 不含 language/tokenizer，对应能力保持默认 Unsupported。
 */
public class Langchain4jOllamaModelFactory implements ModelFactory {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "qwen2.5";
    private static final String DEFAULT_EMBEDDING_MODEL = "nomic-embed-text";

    @Override
    public String provider() {
        return "ollama";
    }

    @Override
    public OllamaChatModel buildChatModel(Langchain4jOptionsReader o) {
        return OllamaChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.dbl("temperature", 0.7))
                .topK(nullIfZero(o.integer("top-k", null)))
                .topP(o.dbl("top-p", null))
                .repeatPenalty(o.dbl("repeat-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .numPredict(nullIfZero(o.integer("num-predict", null)))
                .stop(o.stringList("stop", null))
                .format(o.string("format", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", 3)))
                .build();
    }

    @Override
    public OllamaStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.dbl("temperature", 0.7))
                .topK(nullIfZero(o.integer("top-k", null)))
                .topP(o.dbl("top-p", null))
                .repeatPenalty(o.dbl("repeat-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .numPredict(nullIfZero(o.integer("num-predict", null)))
                .stop(o.stringList("stop", null))
                .format(o.string("format", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", 3)))
                .build();
    }

    @Override
    public OllamaEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", DEFAULT_EMBEDDING_MODEL))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", 3)))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
```

`Langchain4jAnthropicModelFactory.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.anthropic;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;

/**
 * Anthropic 工厂：dev.langchain4j.model.anthropic.* builder 链。仅 chat/streaming-chat 两类。
 */
public class Langchain4jAnthropicModelFactory implements ModelFactory {

    private static final String DEFAULT_VERSION = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 1024;

    @Override
    public String provider() {
        return "anthropic";
    }

    @Override
    public AnthropicChatModel buildChatModel(Langchain4jOptionsReader o) {
        return AnthropicChatModel.builder()
                .apiKey(o.required("api-key"))
                .baseUrl(o.string("base-url", null))
                .version(o.string("version", DEFAULT_VERSION))
                .beta(o.string("beta", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxTokens(nullIfZero(o.integer("max-tokens", DEFAULT_MAX_TOKENS)))
                .stopSequences(o.stringList("stop-sequences", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public AnthropicStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o) {
        return AnthropicStreamingChatModel.builder()
                .apiKey(o.required("api-key"))
                .baseUrl(o.string("base-url", null))
                .version(o.string("version", DEFAULT_VERSION))
                .beta(o.string("beta", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxTokens(nullIfZero(o.integer("max-tokens", DEFAULT_MAX_TOKENS)))
                .stopSequences(o.stringList("stop-sequences", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest='Langchain4jOllamaModelFactoryTest,Langchain4jAnthropicModelFactoryTest' -nsu -q`
Expected: Tests run: 7, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/ollama fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/anthropic fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/ollama fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/anthropic
git commit -m "feat(ai): 新增 Ollama 与 Anthropic 模型工厂"
```

### Task 9: 向量库工厂（in-memory / redis / milvus，dimension 回退链）

**Files:**
- Create: `.../magicapi/modules/langchain4j/factory/store/EmbeddingStoreDimensions.java`（包内工具）
- Create: `.../magicapi/modules/langchain4j/factory/store/Langchain4jInMemoryModelFactory.java`
- Create: `.../magicapi/modules/langchain4j/factory/store/Langchain4jRedisModelFactory.java`
- Create: `.../magicapi/modules/langchain4j/factory/store/Langchain4jMilvusModelFactory.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/store/Langchain4jEmbeddingStoreFactoriesTest.java`

**Interfaces:**
- Consumes: Task 5、Task 3
- Produces:
  - `provider() = "in-memory"`：仅 store；无连接参数；`InMemoryEmbeddingStore` 来自 `dev.langchain4j:langchain4j` 主 jar 的 `dev.langchain4j.store.embedding.inmemory`
  - `provider() = "redis"`：仅 store；options：`host`(默认 localhost)/`port`(6379)/`user`/`password`/`index-name`(默认 magic-langchain4j)/`dimension`/`metadata-keys`；builder：`dev.langchain4j.store.embedding.redis.RedisEmbeddingStore$Builder`
  - `provider() = "milvus"`：仅 store；options：`host`/`port`(19530)/`collection-name`/`dimension`/`index-type`(FLAT)/`metric-type`(COSINE)/`uri`/`token`/`username`(缺省回落环境变量 MILVUS_USERNAME)/`password`(MILVUS_PASSWORD)/`consistency-level`(STRONG)/`retrieve-embeddings-on-search`/`auto-flush-on-insert`/`database-name`；builder：`dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore$Builder`（方法名 `builder()`）
  - dimension 决策链：options `dimension` → `embeddingModel.dimension()` → 抛中文异常

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Langchain4jEmbeddingStoreFactoriesTest {

    private Langchain4jOptionsReader options(String... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return new Langchain4jOptionsReader("embedding-store", "store-main", map);
    }

    @Test
    void 三工厂provider短名() {
        assertEquals("in-memory", new Langchain4jInMemoryModelFactory().provider());
        assertEquals("redis", new Langchain4jRedisModelFactory().provider());
        assertEquals("milvus", new Langchain4jMilvusModelFactory().provider());
    }

    @Test
    void 三类store可构建_带dimension() {
        assertNotNull(new Langchain4jInMemoryModelFactory().buildEmbeddingStore(options(), null));
        assertNotNull(new Langchain4jRedisModelFactory().buildEmbeddingStore(options("dimension", 1024), null));
        assertNotNull(new Langchain4jMilvusModelFactory().buildEmbeddingStore(options("dimension", 1024), null));
    }

    @Test
    void dimension回退_配置优先() {
        ModelFactory redis = new Langchain4jRedisModelFactory();
        assertNotNull(redis.buildEmbeddingStore(options("dimension", 768), fakeModel(1024)));
    }

    @Test
    void dimension回退_取模型维度() {
        ModelFactory redis = new Langchain4jRedisModelFactory();
        assertNotNull(redis.buildEmbeddingStore(options(), fakeModel(1024)));
    }

    @Test
    void dimension都无时抛中文异常() {
        ModelFactory redis = new Langchain4jRedisModelFactory();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> redis.buildEmbeddingStore(options(), null));
        assertTrue(ex.getMessage().contains("dimension"), ex.getMessage());
    }

    @Test
    void 模型能力一律不支持() {
        ModelFactory mem = new Langchain4jInMemoryModelFactory();
        assertThrows(UnsupportedOperationException.class, () -> mem.buildChatModel(options()));
        assertThrows(UnsupportedOperationException.class, () -> mem.buildEmbeddingModel(options()));
    }

    private static EmbeddingModel fakeModel(int dimension) {
        return new EmbeddingModel() {
            @Override public Response<dev.langchain4j.data.embedding.Embedding> embedAll(List<TextSegment> list) {
                throw new UnsupportedOperationException();
            }
            @Override public int dimension() {
                return dimension;
            }
        };
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jEmbeddingStoreFactoriesTest -nsu -q`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 写实现**

`EmbeddingStoreDimensions.java`（包内共用）：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.embedding.EmbeddingModel;

/** dimension 决策链：options 配置值优先 → 关联 EmbeddingModel.dimension() → 抛中文异常。 */
final class EmbeddingStoreDimensions {

    private EmbeddingStoreDimensions() {
    }

    static Integer resolve(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer configured = o.integer("dimension", null);
        if (configured != null && configured != 0) {
            return configured;
        }
        if (embeddingModel != null) {
            return embeddingModel.dimension();
        }
        throw new IllegalArgumentException("未配置 dimension，且关联 embedding-model 无法提供维度："
            + "请在 datasource options 配置 dimension，或确保 embeddingModelKey 指向的 embedding-model 已启用");
    }
}
```

`Langchain4jInMemoryModelFactory.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * 内存向量库工厂：开发/测试用，无连接参数，不校验 dimension。
 * InMemoryEmbeddingStore 位于 dev.langchain4j:langchain4j（主 jar）。
 */
public class Langchain4jInMemoryModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "in-memory";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return new InMemoryEmbeddingStore<TextSegment>();
    }
}
```

`Langchain4jRedisModelFactory.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;

/**
 * Redis 向量库工厂：写法参照 core 层 fw-plus-ai-langchain4j-redis 的 AutoConfig。
 * RedisEmbeddingStore.builder() 静态方法返回 Builder；注意早期版本是 public 构造器。
 */
public class Langchain4jRedisModelFactory implements ModelFactory {

    private static final String DEFAULT_INDEX_NAME = "magic-langchain4j";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;

    @Override
    public String provider() {
        return "redis";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return RedisEmbeddingStore.builder()
                .host(o.string("host", DEFAULT_HOST))
                .port(o.integer("port", DEFAULT_PORT))
                .user(o.string("user", null))
                .password(o.string("password", null))
                .indexName(o.string("index-name", DEFAULT_INDEX_NAME))
                .dimension(dimension)
                .metadataKeys(o.stringList("metadata-keys", null))
                .build();
    }
}
```

`Langchain4jMilvusModelFactory.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;

/**
 * Milvus 向量库工厂：写法参照 core 层 fw-plus-ai-langchain4j-milvus 的 AutoConfig
 * （Optional 默认值 + @Nullable EmbeddingModel 维度回退）。
 */
public class Langchain4jMilvusModelFactory implements ModelFactory {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 19530;
    private static final String DEFAULT_COLLECTION_NAME = "magic_langchain4j";

    @Override
    public String provider() {
        return "milvus";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return MilvusEmbeddingStore.builder()
                .host(o.string("host", DEFAULT_HOST))
                .port(o.integer("port", DEFAULT_PORT))
                .collectionName(o.string("collection-name", DEFAULT_COLLECTION_NAME))
                .dimension(dimension)
                .indexType(o.enumValue("index-type", IndexType.class, IndexType.FLAT))
                .metricType(o.enumValue("metric-type", MetricType.class, MetricType.COSINE))
                .uri(o.string("uri", null))
                .token(o.string("token", null))
                .username(o.string("username", System.getenv("MILVUS_USERNAME")))
                .password(o.string("password", System.getenv("MILVUS_PASSWORD")))
                .consistencyLevel(o.enumValue("consistency-level", ConsistencyLevelEnum.class, ConsistencyLevelEnum.STRONG))
                .retrieveEmbeddingsOnSearch(o.bool("retrieve-embeddings-on-search", null))
                .autoFlushOnInsert(o.bool("auto-flush-on-insert", null))
                .databaseName(o.string("database-name", null))
                .build();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jEmbeddingStoreFactoriesTest -nsu -q`
Expected: Tests run: 6, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/store fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/factory/store
git commit -m "feat(ai): 新增向量库工厂（in-memory/redis/milvus，dimension 回退链）"
```

---

### Task 10: 七类资源的 MagicResourceStorage 与校验

**Files:**
- Create: `.../magicapi/modules/langchain4j/resource/AbstractLangchain4jModelResourceStorage.java`
- Create: `.../magicapi/modules/langchain4j/resource/Langchain4jChatModelMagicResourceStorage.java`
- Create: `.../magicapi/modules/langchain4j/resource/Langchain4jStreamingChatModelMagicResourceStorage.java`
- Create: `.../magicapi/modules/langchain4j/resource/Langchain4jLanguageModelMagicResourceStorage.java`
- Create: `.../magicapi/modules/langchain4j/resource/Langchain4jStreamingLanguageModelMagicResourceStorage.java`
- Create: `.../magicapi/modules/langchain4j/resource/Langchain4jEmbeddingModelMagicResourceStorage.java`
- Create: `.../magicapi/modules/langchain4j/resource/Langchain4jTokenizerMagicResourceStorage.java`
- Create: `.../magicapi/modules/langchain4j/resource/Langchain4jEmbeddingStoreMagicResourceStorage.java`
- Test: `src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/resource/Langchain4jModelResourceStorageTest.java`

**Interfaces:**
- Consumes: Task 2（实体/类型）；`MagicResourceStorage<T>`（magic-api 核心）
- Produces:
  - `abstract class AbstractLangchain4jModelResourceStorage<T extends Langchain4jModelInfo> implements MagicResourceStorage<T>`，构造 `(Langchain4jModelTypes type, Class<T> magicClass, Set<String> providers)`；方法：`folder()=type.type()`、`suffix()=".json"`、`requirePath()=false`、`requiredScript()=false`、`allowRoot()=true`、`buildMappingKey(entity)=key+"-"+updateTime`、`validate(entity)`（key 必填+格式、provider 命中短名表、key 同 folder 唯一）、`read(byte[])`/`write(MagicEntity)`/`readResource(Resource)`（JsonUtils）、`setMagicResourceService`
  - 七个子类各提供一个无参构造（空 providers，供单测）与 `(Set<String> providers)` 构造（装配期注入，Task 13）；store 子类覆写 `validate` 追加 `embeddingModelKey` 必填

- [ ] **Step 1: 写失败测试**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.*;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class Langchain4jModelResourceStorageTest {

    private final Set<String> providers = new LinkedHashSet<String>(Arrays.asList("openai", "qwen"));

    private Langchain4jChatModelInfo info(String key, String provider) {
        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        info.setId("id-" + key);
        info.setKey(key);
        info.setProvider(provider);
        return info;
    }

    @Test
    void chatStorage_folder与type一致且标志位正确() {
        Langchain4jChatModelMagicResourceStorage storage =
            new Langchain4jChatModelMagicResourceStorage(providers);
        assertEquals("langchain4j-chat-model", storage.folder());
        assertEquals(Langchain4jModelTypes.CHAT_MODEL.type(), storage.folder());
        assertEquals(".json", storage.suffix());
        assertFalse(storage.requirePath());
        assertFalse(storage.requiredScript());
        assertTrue(storage.allowRoot());
        assertEquals(Langchain4jChatModelInfo.class, storage.magicClass());
    }

    @Test
    void validate_key缺失与非法字符被拒() {
        Langchain4jChatModelMagicResourceStorage storage =
            new Langchain4jChatModelMagicResourceStorage(providers);
        assertThrows(IllegalArgumentException.class, () -> storage.validate(info(null, "openai")));
        assertThrows(IllegalArgumentException.class, () -> storage.validate(info("bad key!", "openai")));
    }

    @Test
    void validate_provider缺失与表外被拒_报错含可用短名() {
        Langchain4jChatModelMagicResourceStorage storage =
            new Langchain4jChatModelMagicResourceStorage(providers);
        assertThrows(IllegalArgumentException.class, () -> storage.validate(info("k1", null)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> storage.validate(info("k1", "ghost")));
        assertTrue(ex.getMessage().contains("openai"), ex.getMessage());
        assertTrue(ex.getMessage().contains("qwen"), ex.getMessage());
    }

    @Test
    void validate_同folder重复key被拒() {
        Langchain4jChatModelMagicResourceStorage storage =
            new Langchain4jChatModelMagicResourceStorage(providers);
        MagicResourceService service = mock(MagicResourceService.class);
        Langchain4jChatModelInfo exist = info("dup", "openai");
        exist.setId("other-id");
        when(service.listFiles("langchain4j-chat-model:0"))
            .thenReturn(Collections.<Object>singletonList(exist));
        storage.setMagicResourceService(service);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> storage.validate(info("dup", "openai")));
        assertTrue(ex.getMessage().contains("已存在"), ex.getMessage());
    }

    @Test
    void embeddingStore_要求embeddingModelKey() {
        Langchain4jEmbeddingStoreMagicResourceStorage storage =
            new Langchain4jEmbeddingStoreMagicResourceStorage(providers);
        Langchain4jEmbeddingStoreInfo info = new Langchain4jEmbeddingStoreInfo();
        info.setId("x");
        info.setKey("s1");
        info.setProvider("redis");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> storage.validate(info));
        assertTrue(ex.getMessage().contains("embeddingModelKey"), ex.getMessage());
    }

    @Test
    void 读写往返_options自由map保留() {
        Langchain4jChatModelMagicResourceStorage storage =
            new Langchain4jChatModelMagicResourceStorage(providers);
        Langchain4jChatModelInfo info = info("k1", "qwen");
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("enable-search", true);
        info.setOptions(options);
        Langchain4jChatModelInfo read = storage.read(storage.write(info));
        assertEquals("k1", read.getKey());
        assertEquals(Boolean.TRUE, read.getOptions().get("enable-search"));
    }

    @Test
    void 七个storage_folder与类型一一对应() {
        Map<String, String> folders = new LinkedHashMap<String, String>();
        folders.put("langchain4j-chat-model",
            new Langchain4jChatModelMagicResourceStorage(providers).folder());
        folders.put("langchain4j-streaming-chat-model",
            new Langchain4jStreamingChatModelMagicResourceStorage(providers).folder());
        folders.put("langchain4j-language-model",
            new Langchain4jLanguageModelMagicResourceStorage(providers).folder());
        folders.put("langchain4j-streaming-language-model",
            new Langchain4jStreamingLanguageModelMagicResourceStorage(providers).folder());
        folders.put("langchain4j-embedding-model",
            new Langchain4jEmbeddingModelMagicResourceStorage(providers).folder());
        folders.put("langchain4j-tokenizer",
            new Langchain4jTokenizerMagicResourceStorage(providers).folder());
        folders.put("langchain4j-embedding-store",
            new Langchain4jEmbeddingStoreMagicResourceStorage(providers).folder());
        for (Map.Entry<String, String> e : folders.entrySet()) {
            assertEquals(e.getKey(), e.getValue());
        }
        assertEquals(Langchain4jModelTypes.values().length, folders.size());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jModelResourceStorageTest -nsu -q`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 写实现**

`AbstractLangchain4jModelResourceStorage.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jModelTypes;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;
import java.util.Set;

/**
 * 七类模型资源存储的公共骨架：folder 与 datasource type 同源（Langchain4jModelTypes），
 * JSON 序列化、公共字段校验、key 唯一性。
 *
 * @param <T> 具体资源实体
 */
public abstract class AbstractLangchain4jModelResourceStorage<T extends Langchain4jModelInfo>
        implements MagicResourceStorage<T> {

    private final Langchain4jModelTypes type;
    private final Class<T> magicClass;
    private final Set<String> providers;

    protected MagicResourceService magicResourceService;

    protected AbstractLangchain4jModelResourceStorage(Langchain4jModelTypes type, Class<T> magicClass,
            Set<String> providers) {
        this.type = type;
        this.magicClass = magicClass;
        this.providers = providers;
    }

    @Override
    public String folder() {
        return type.type();
    }

    @Override
    public String suffix() {
        return ".json";
    }

    @Override
    public Class<T> magicClass() {
        return magicClass;
    }

    @Override
    public boolean requirePath() {
        return false;
    }

    @Override
    public boolean requiredScript() {
        return false;
    }

    @Override
    public boolean allowRoot() {
        return true;
    }

    @Override
    public String buildMappingKey(T entity) {
        return String.format("%s-%s", entity.getKey(), entity.getUpdateTime());
    }

    @Override
    public void validate(T entity) {
        validateRequired(entity.getKey(), "key");
        if (!entity.getKey().matches("^[\\w.-]+$")) {
            throw new IllegalArgumentException("key只能包含字母、数字、下划线、点与中划线");
        }
        validateRequired(entity.getProvider(), "provider");
        if (!providers.contains(entity.getProvider())) {
            throw new IllegalArgumentException(String.format(
                "provider '%s' 不受支持，可用 provider: %s", entity.getProvider(), providers));
        }
        validateKeyUnique(entity);
    }

    protected void validateRequired(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
    }

    private void validateKeyUnique(T entity) {
        if (magicResourceService == null) {
            return;
        }
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .filter(it -> it instanceof Langchain4jModelInfo)
                .map(it -> (Langchain4jModelInfo) it)
                .filter(it -> !Objects.equals(it.getId(), entity.getId()))
                .noneMatch(it -> Objects.equals(it.getKey(), entity.getKey()));
        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("key '%s' 已存在，请使用其他 key", entity.getKey()));
        }
    }

    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read(byte[] bytes) {
        return JsonUtils.readValue(bytes, magicClass);
    }

    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    @Override
    public T readResource(Resource resource) {
        return read(resource.read());
    }
}
```

七个子类（以 chat 为例，其余替换类型常量/实体类/文档注释；store 覆写 validate）：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jModelTypes;

import java.util.Collections;
import java.util.Set;

/** chat-model 资源存储：folder=langchain4j-chat-model，每份 JSON 一个 ChatLanguageModel。 */
public class Langchain4jChatModelMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jChatModelInfo> {

    /** 无 providers 构造：单测用；装配期用带 providers 构造（Task 13） */
    public Langchain4jChatModelMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jChatModelMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.CHAT_MODEL, Langchain4jChatModelInfo.class, providers);
    }
}
```

`Langchain4jEmbeddingStoreMagicResourceStorage.java`：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.model.Langchain4jModelTypes;

import java.util.Collections;
import java.util.Set;

/** embedding-store 资源存储：folder=langchain4j-embedding-store；额外必填 embeddingModelKey。 */
public class Langchain4jEmbeddingStoreMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jEmbeddingStoreInfo> {

    public Langchain4jEmbeddingStoreMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jEmbeddingStoreMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.EMBEDDING_STORE, Langchain4jEmbeddingStoreInfo.class, providers);
    }

    @Override
    public void validate(Langchain4jEmbeddingStoreInfo entity) {
        super.validate(entity);
        validateRequired(entity.getEmbeddingModelKey(), "embeddingModelKey");
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=Langchain4jModelResourceStorageTest -nsu -q`
Expected: Tests run: 8, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/resource fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/modules/langchain4j/resource
git commit -m "feat(ai): 新增七类模型资源存储与公共校验（folder=type、key 唯一、provider 短名表）"
```

<!-- CONTINUE-MARKER-TASK11 -->
