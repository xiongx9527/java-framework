---
change: optimize-easytrans
design-doc: docs/superpowers/specs/2026-07-12-optimize-easytrans-design.md
base-ref: 5a66ee7c7ab0f88dd23d4ffee1e2a5e39b52780e
---

# optimize-easytrans Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 优化 `fw-plus-easytrans` 共享翻译链路的空值、集合、递归判重和代理兜底行为，并用最小测试锁住兼容输出。

**Architecture:** 按 `docs/superpowers/specs/2026-07-12-optimize-easytrans-design.md` 执行“共享链路最小修复”。只改 `MateEasyTransUtils` 的公共入口内部实现和同模块测试，不改公开方法签名，不重构 ByteBuddy 代理，不在各翻译服务分散打补丁。

**Tech Stack:** Java 1.8、Maven、Spring Boot 2.7.18、Easy-Trans、JUnit/Spring Boot 测试能力（若当前模块无测试依赖，则在 service 模块以 test scope 引入已有 BOM 管理的 `spring-boot-starter-test`）。

## Global Constraints

- 产物语言：zh-CN。
- 修改范围仅限 `fw-plus-core/fw-plus-project/fw-plus-easytrans` 的 annotation/common/service 三个子模块。
- 保持公开 API、注解、配置 key、返回结构兼容。
- 不新增第三方运行时依赖，不新增未立即使用的抽象。
- 优先稳定性与明确热点性能；性能优化只处理重复首元素读取、递归判重线性扫描等已确认开销。
- Boolean、Enum、Dictionary、RPC 翻译服务输出必须保持兼容。
- 每个 OpenSpec task 验收后按 Comet 规则勾选 `openspec/changes/optimize-easytrans/tasks.md` 并提交。

---

## File Structure

- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/pom.xml`
  - 责任：仅在缺少测试能力时添加 test scope 依赖，让 service 模块能运行最小单元测试。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/main/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtils.java`
  - 责任：修复共享翻译入口的集合首元素、集合内 null、身份判重、代理生成空值兜底。
- Create: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java`
  - 责任：覆盖本次改动的最小失败/回归场景，不启动完整 Spring 容器。
- Modify: `openspec/changes/optimize-easytrans/tasks.md`
  - 责任：每个任务验收后定向勾选对应 OpenSpec task。

---

### Task 1: 建立 MateEasyTransUtils 最小回归测试

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java`
- Verify: `openspec/changes/optimize-easytrans/tasks.md` item `1.2`

**Interfaces:**
- Consumes: 当前 `MateEasyTransUtils.transOne(...)`、`transBatch(...)`、`createProxyVo(...)` public static 方法。
- Produces: `MateEasyTransUtilsTest.CountingTransService`，后续任务继续复用它验证实现。

- [ ] **Step 1: Add test dependency if missing**

在 `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/pom.xml` 的 `<dependencies>` 内追加 test scope 依赖。若父 POM 已统一提供测试依赖且当前模块测试可编译，则跳过此改动。

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Write the failing tests**

创建 `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java`：

```java
package com.xiongx9527.fw.plus.easytrans.service.util;

import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MateEasyTransUtilsTest {

    @Test
    void transOneReturnsNullForNullInput() throws Exception {
        Object result = MateEasyTransUtils.transOne(null, new CountingTransService(), false,
                new ArrayList<>(), null, null);

        assertNull(result);
    }

    @Test
    void transBatchReturnsEmptyCollectionAsIs() throws Exception {
        List<Object> source = new ArrayList<>();

        Collection result = MateEasyTransUtils.transBatch(source, new CountingTransService(), false,
                new ArrayList<>(), null, null);

        assertSame(source, result);
        assertTrue(result.isEmpty());
    }

    @Test
    void transBatchSkipsLeadingNullAndTranslatesLaterVo() throws Exception {
        CountingTransService transService = new CountingTransService();
        TestVo vo = new TestVo(1L);
        List<Object> source = new ArrayList<>(Arrays.asList(null, vo));

        Collection result = MateEasyTransUtils.transBatch(source, transService, false,
                new ArrayList<>(), null, null);

        assertSame(source, result);
        assertEquals(1, transService.transOneCount);
        assertSame(vo, transService.lastVo);
    }

    @Test
    void transOneDoesNotRecurseForeverOnCycle() throws Exception {
        CountingTransService transService = new CountingTransService();
        Node node = new Node();
        node.self = node;

        Object result = MateEasyTransUtils.transOne(node, transService, false,
                new ArrayList<>(), null, null);

        assertSame(node, result);
    }

    @Test
    void createProxyVoReturnsOriginalVoWhenProxyCannotBeCreated() {
        TestVo vo = new TestVo(2L);
        vo.getTransMap().put("dynamicField", "value");

        Object result = MateEasyTransUtils.createProxyVo(vo);

        assertTrue(result instanceof VO);
    }

    static class CountingTransService extends TransService {
        int transOneCount;
        int transMoreCount;
        VO lastVo;

        @Override
        public void transOne(VO obj, Set<String> includeFields, Set<String> excludeFields) {
            transOneCount++;
            lastVo = obj;
            obj.getTransMap().put("translated", "yes");
        }

        @Override
        public void transMore(List<? extends VO> objList, Set<String> includeFields, Set<String> excludeFields) {
            transMoreCount++;
            for (VO vo : objList) {
                if (vo != null) {
                    transOne(vo, includeFields, excludeFields);
                }
            }
        }
    }

    static class TestVo implements VO {
        private final Long id;
        private final Map<String, Object> transMap = new java.util.LinkedHashMap<>();

        TestVo(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        @Override
        public Map<String, Object> getTransMap() {
            return transMap;
        }
    }

    static class Node {
        Node self;
    }
}
```

如果 `VO` 不是 interface 或需要额外方法，按编译错误补齐 `TestVo` 的最小实现，但不要引入 Spring 容器。

- [ ] **Step 3: Run tests to verify current failures**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service -Dtest=MateEasyTransUtilsTest
```

Expected: 至少 `transBatchSkipsLeadingNullAndTranslatesLaterVo` 在当前实现下失败，因为首元素为 `null` 时不会翻译后续 VO。若测试依赖或 `VO` stub 编译失败，先按错误最小调整测试代码，不改生产代码。

- [ ] **Step 4: Commit test setup only after it compiles to the expected failing point**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/pom.xml \
        fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java
git commit -m "test: cover easytrans shared translation edge cases"
```

---

### Task 2: 修复集合批量翻译的 null 与重复首元素读取

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/main/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtils.java:103-157`
- Test: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java`
- Verify: `openspec/changes/optimize-easytrans/tasks.md` items `2.1`, `3.1`

**Interfaces:**
- Consumes: Task 1 的 `transBatchSkipsLeadingNullAndTranslatesLaterVo` 测试。
- Produces: `firstNonNull(Collection)` 私有辅助方法；后续任务不依赖公开签名变化。

- [ ] **Step 1: Replace transBatch with single first-non-null scan**

在 `MateEasyTransUtils.java` 中把 `transBatch` 方法替换为：

```java
    public static Collection transBatch(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        Collection param = (Collection)object;
        if (param == null) {
            return null;
        }
        if (param.isEmpty()) {
            return param;
        }

        Object firstNonNull = firstNonNull(param);
        if (firstNonNull == null) {
            return param;
        }

        boolean isVo = firstNonNull instanceof VO;
        if (isVo) {
            transService.transMore(new ArrayList(param), includeFields, excludeFields);
            for (Object tempObject : param) {
                if (tempObject != null) {
                    transFields(tempObject, transService, isProxy, hasTransObjs, includeFields, excludeFields);
                }
            }
        } else {
            for (Object tempObject : param) {
                if (tempObject != null) {
                    transOne(tempObject, transService, isProxy, hasTransObjs, includeFields, excludeFields);
                }
            }
        }

        if (isProxy && isVo) {
            Collection result;
            if (param instanceof List) {
                result = new ArrayList();
            } else if (param instanceof Set) {
                result = new HashSet();
            } else {
                return param;
            }

            for (Object vo : param) {
                if (vo == null) {
                    result.add(null);
                } else {
                    result.add(createProxyVo((VO)vo));
                }
            }
            return result;
        }
        return param;
    }

    private static Object firstNonNull(Collection collection) {
        for (Object item : collection) {
            if (item != null) {
                return item;
            }
        }
        return null;
    }
```

- [ ] **Step 2: Run focused tests**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service -Dtest=MateEasyTransUtilsTest
```

Expected: `transBatchSkipsLeadingNullAndTranslatesLaterVo` passes. If proxy mode with `null` in a `Set` changes ordering, accept because Set ordering is already undefined; keep element preservation for `List`.

- [ ] **Step 3: Update OpenSpec task checkboxes for accepted work**

After focused tests pass, edit `openspec/changes/optimize-easytrans/tasks.md` only for completed items:

```md
- [x] 1.2 为将要修改的稳定性或性能路径补最小失败用例。
- [x] 2.1 修复共享翻译链路中的 null、空集合、反射访问或翻译异常兜底问题。
- [x] 3.1 移除批量翻译中的重复首元素读取、重复遍历或无效转换。
```

Do not check items `1.1`, `2.2`, `3.2`, `4.1`, or `4.2` yet.

- [ ] **Step 4: Commit**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/main/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtils.java \
        fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java \
        openspec/changes/optimize-easytrans/tasks.md
git commit -m "fix: handle easytrans batch null elements"
```

---

### Task 3: 引入内部身份判重上下文

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/main/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtils.java:197-279`
- Test: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java`
- Verify: `openspec/changes/optimize-easytrans/tasks.md` items `1.1`, `3.2`

**Interfaces:**
- Consumes: public `transOne(Object, TransService, boolean, ArrayList<Object>, Set<String>, Set<String>)` signature remains unchanged.
- Produces: private overload using `Set<Object>` backed by `IdentityHashMap`.

- [ ] **Step 1: Add JDK imports**

在 `MateEasyTransUtils.java` imports 中确认存在：

```java
import java.util.Collections;
import java.util.IdentityHashMap;
```

- [ ] **Step 2: Replace contains-based entry with identity set adapter**

将现有 public `transOne(...)` 方法改为薄适配器，并新增私有重载：

```java
    public static Object transOne(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        Set<Object> transContext = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        if (hasTransObjs != null) {
            transContext.addAll(hasTransObjs);
        }
        return transOne(object, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
    }

    private static Object transOne(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs, Set<Object> transContext, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        if (object == null) {
            return null;
        }
        if (transContext.contains(object)) {
            return object;
        }
        transContext.add(object);
        if (hasTransObjs != null) {
            hasTransObjs.add(object);
        }

        boolean isVo = false;
        if (transResultMap && object instanceof Map) {
            Map tempMap = (Map)object;
            for (Object key : tempMap.keySet()) {
                Object mapValue = tempMap.get(key);
                try {
                    tempMap.put(key, transOne(mapValue, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields));
                } catch (Exception e) {
                    log.error("翻译Map字段错误", e);
                }
            }
            return object;
        }

        if (object instanceof VO) {
            transService.transOne((VO)object, includeFields, excludeFields);
            transFields(object, transService, isProxy, hasTransObjs, includeFields, excludeFields);
            isVo = true;
        } else {
            if (object instanceof Collection) {
                return transBatch(object, transService, isProxy, hasTransObjs, includeFields, excludeFields);
            }
            if (object.getClass().getName().startsWith("java.")) {
                return object;
            }
            transFields(object, transService, isProxy, hasTransObjs, includeFields, excludeFields);
        }
        return isProxy && isVo ? createProxyVo((VO)object) : object;
    }
```

说明：此步骤保持 public 签名不变。内部重载先不强行贯穿 `transFields` 和 `transBatch` 的签名，避免扩大改动面；若测试发现循环仍未覆盖，再在同任务中把私有重载继续传入 `transFields`/`transBatch` 的私有版本。

- [ ] **Step 3: Run cycle test**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service -Dtest=MateEasyTransUtilsTest#transOneDoesNotRecurseForeverOnCycle
```

Expected: PASS，不出现 `StackOverflowError`。

- [ ] **Step 4: Run full focused tests**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service -Dtest=MateEasyTransUtilsTest
```

Expected: PASS。

- [ ] **Step 5: Update OpenSpec task checkboxes**

Edit `openspec/changes/optimize-easytrans/tasks.md`:

```md
- [x] 1.1 梳理 `fw-plus-easytrans` 当前翻译入口、工具方法和翻译服务调用链，确认实际修改点。
- [x] 3.2 复用已有缓存或局部变量降低重复反射开销，不新增独立缓存层。
```

- [ ] **Step 6: Commit**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/main/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtils.java \
        fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java \
        openspec/changes/optimize-easytrans/tasks.md
git commit -m "perf: use identity tracking in easytrans recursion"
```

---

### Task 4: 补代理和兼容输出验证

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/main/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtils.java:421-453`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java`
- Verify: `openspec/changes/optimize-easytrans/tasks.md` item `2.2`

**Interfaces:**
- Consumes: existing `createProxyVo(VO vo)` public static method.
- Produces: same return contract：代理成功返回代理对象，代理失败或无 transMap 返回原 VO。

- [ ] **Step 1: Add explicit null-class guard in createProxyVo**

在 `createProxyVo` 中 `Class clazz = genNewClass(vo);` 后立即增加：

```java
                if (clazz == null) {
                    return vo;
                }
```

目标代码片段：

```java
                Class clazz = genNewClass(vo);
                if (clazz == null) {
                    return vo;
                }
                Object newObject = clazz.newInstance();
```

- [ ] **Step 2: Add compatibility tests for Boolean and Enum translators only if cheap**

如果当前测试依赖已经可用，在 `MateEasyTransUtilsTest.java` 追加最小兼容断言，不启动 Spring：

```java
    @Test
    void booleanAndEnumServicesRemainInstantiable() {
        assertTrue(new com.xiongx9527.fw.plus.easytrans.service.MateEasyTransBooleanTransService()
                .isValid(Boolean.class, "", Boolean.TRUE));
        assertTrue(new com.xiongx9527.fw.plus.easytrans.service.MateEasyTransEnumTransService()
                .isValid(SampleEnum.class, "code", "A"));
    }

    enum SampleEnum {
        A("A", "启用");

        private final String code;
        private final String label;

        SampleEnum(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }
```

如果 `MateEasyTransEnumTransService.isValid` 因 Easy-Trans 反射细节无法在单测中稳定运行，不为它写脆弱测试；改为运行 service 模块编译并记录未修改 Enum/Dictionary/RPC 服务实现，避免制造低价值测试。

- [ ] **Step 3: Run focused tests**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service -Dtest=MateEasyTransUtilsTest
```

Expected: PASS。

- [ ] **Step 4: Update OpenSpec task checkbox**

Edit `openspec/changes/optimize-easytrans/tasks.md`:

```md
- [x] 2.2 确认 Boolean、Enum、Dictionary、RPC 翻译输出保持兼容。
```

若 Dictionary/RPC 未写单测，在提交信息 body 或 tasks.md 附近保留一句：`兼容确认：本 change 未修改 Dictionary/RPC 服务实现，模块编译覆盖其公开签名。`

- [ ] **Step 5: Commit**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/main/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtils.java \
        fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java \
        openspec/changes/optimize-easytrans/tasks.md
git commit -m "fix: keep easytrans proxy fallback safe"
```

---

### Task 5: 模块验证与 Build 阶段收口

**Files:**
- Modify: `openspec/changes/optimize-easytrans/tasks.md`
- No source changes expected unless verification exposes a real defect.

**Interfaces:**
- Consumes: Tasks 1-4 committed code.
- Produces: 全部 OpenSpec task 勾选完成，build guard 可进入 verify。

- [ ] **Step 1: Run easytrans focused tests**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service -Dtest=MateEasyTransUtilsTest
```

Expected: BUILD SUCCESS。

If this fails, load `superpowers:systematic-debugging` before any source fix, identify root cause, then fix and rerun.

- [ ] **Step 2: Run module compile**

Run:

```bash
cd fw-plus-core && ./mvnw compile -pl fw-plus-project/fw-plus-easytrans -DskipTests
```

Expected: BUILD SUCCESS。

If Maven reports missing reactor dependencies, rerun with `-am`:

```bash
cd fw-plus-core && ./mvnw compile -pl fw-plus-project/fw-plus-easytrans -am -DskipTests
```

- [ ] **Step 3: Update final OpenSpec verification checkboxes**

Edit `openspec/changes/optimize-easytrans/tasks.md`:

```md
- [x] 4.1 运行 `fw-plus-easytrans` 相关单元测试或最小验证。
- [x] 4.2 运行模块编译，确认不破坏下游兼容编译。
```

- [ ] **Step 4: Commit verification evidence**

```bash
git add openspec/changes/optimize-easytrans/tasks.md
git commit -m "chore: verify easytrans optimization"
```

- [ ] **Step 5: Prepare review gate before build guard**

If `review_mode=standard`, request one final lightweight code review before running build guard. If review skill is unavailable, add this persistent note to `openspec/changes/optimize-easytrans/tasks.md` and commit it:

```md
<!-- review skipped: skill unavailable -->
```

- [ ] **Step 6: Run Comet build guard**

Run from repo root:

```bash
COMET_GUARD=/Users/xiongxiao/.claude/skills/comet/scripts/comet-guard.mjs
node "$COMET_GUARD" optimize-easytrans build --apply
```

Expected: `ALL CHECKS PASSED` and `.comet.yaml` phase advances to `verify`.

---

## Self-Review

- Spec coverage: `兼容的 easytrans 优化` covered by Tasks 2-4; `翻译失败被限制在翻译层内` covered by Tasks 1-2 and existing AOP/ResponseBodyAdvice behavior; `避免可避免的重复工作` covered by Tasks 2-3; `优化具备最小验证` covered by Task 5。
- Placeholder scan: no TBD/TODO placeholders remain; every code-changing step includes exact path and code/command.
- Type consistency: public `MateEasyTransUtils` signatures remain unchanged; new helpers are private; no new runtime dependency is introduced.
