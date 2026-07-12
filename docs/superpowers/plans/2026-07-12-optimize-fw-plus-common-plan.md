---
change: optimize-fw-plus-common
design-doc: docs/superpowers/specs/2026-07-12-optimize-fw-plus-common-design.md
base-ref: 8317c3b8c36e073abb83fc78fb9d9bc91344ee02
---

# optimize-fw-plus-common Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 以兼容收敛方式严格优化 `fw-plus-common`，只处理有明确证据、可验证收益的最小改动。

**Architecture:** 先做调用半径和候选点筛选，再只实现一个最小优化批次。当前优先候选是 `CommonsValidatorHolder` 的线程上下文空值处理与相关公共校验路径；其他区域仅记录，不无证据改动。

**Tech Stack:** Java 1.8、Maven、Spring Boot 2.7.18、Hibernate Validator、JUnit（沿用父工程测试栈）。

## Global Constraints

- 不新增业务能力。
- 不引入新依赖。
- 不改变 public API 方法签名、返回语义和异常语义。
- 不做跨模块架构重写。
- 不做批量格式化或纯风格化修改。
- 非平凡逻辑变化必须有最小可运行验证。
- 每个 Comet task 验收后立即勾选 `openspec/changes/optimize-fw-plus-common/tasks.md` 并提交，不累计提交。

---

## File Structure

- Modify: `fw-plus-core/fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolder.java`
  - 责任：持有校验上下文 ThreadLocal；本计划只允许简化空 Map 判断，保持 API 不变。
- Create: `fw-plus-core/fw-plus-project/fw-plus-common/src/test/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolderTest.java`
  - 责任：验证 holder 的 get/set/clear 和空 Map 清理语义。
- Maybe modify: `openspec/changes/optimize-fw-plus-common/tasks.md`
  - 责任：每个任务验收后定向勾选对应 OpenSpec task。

## Task 1: 筛选并固定最小优化清单

**Files:**
- Read: `docs/superpowers/specs/2026-07-12-optimize-fw-plus-common-design.md`
- Read: `openspec/changes/optimize-fw-plus-common/tasks.md`
- Read: `fw-plus-core/fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolder.java`
- Read: `fw-plus-core/fw-plus-project/fw-plus-common/pom.xml`
- Modify: `openspec/changes/optimize-fw-plus-common/tasks.md`

**Interfaces:**
- Consumes: OpenSpec requirement `common-module-optimization`。
- Produces: 一个固定的最小优化清单：本轮只改 `CommonsValidatorHolder`，除非代码证据发现同一路径还有必须一起修的兼容问题。

- [ ] **Step 1: 确认当前阶段仍是 build**

Run:
```bash
COMET_ENV="${COMET_ENV:-$(find . "$HOME"/.*/skills "$HOME/.config" "$HOME/.gemini" -path '*/comet/scripts/comet-env.mjs' -type f -print -quit 2>/dev/null)}"
COMET_SCRIPTS_DIR="$(node "$COMET_ENV")"
COMET_STATE="$COMET_SCRIPTS_DIR/comet-state.mjs"
node "$COMET_STATE" get optimize-fw-plus-common phase
```
Expected: `build`

- [ ] **Step 2: 用 CodeGraph 确认 holder 调用半径**

Run CodeGraph query:
```text
CommonsValidatorHolder CommonsValidatorRequestBodyAdvice CommonsValidatorResponseBodyAdvice PoiValidationUtil callers tests
```
Expected: 输出包含 `CommonsValidatorHolder` 调用方和相关 Boot 测试。

- [ ] **Step 3: 固定本轮不处理项**

记录到任务执行日志或提交说明：
```text
本轮只处理 CommonsValidatorHolder 的空 Map / clear 语义。
SpringUtils、WebRequestContextHolder、异常/结果封装暂不处理：调用面更宽或缺少明确收益，避免扩大回归面。
```

- [ ] **Step 4: 勾选 OpenSpec 任务 1.1 和 1.2**

Edit `openspec/changes/optimize-fw-plus-common/tasks.md`:
```md
- [x] 1.1 盘点 `fw-plus-common` 关键公共类及调用半径，筛掉无明确收益的改动。
- [x] 1.2 确定本次只处理的最小优化清单，并记录不处理项。
```

- [ ] **Step 5: 提交筛选结果**

Run:
```bash
git add openspec/changes/optimize-fw-plus-common/tasks.md docs/superpowers/plans/2026-07-12-optimize-fw-plus-common-plan.md
git commit -m "docs: plan fw-plus-common optimization scope"
```
Expected: commit succeeds.

## Task 2: 用 TDD 固化 CommonsValidatorHolder 语义

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-common/src/test/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolderTest.java`
- Modify: `openspec/changes/optimize-fw-plus-common/tasks.md`

**Interfaces:**
- Consumes: `CommonsValidatorHolder.get()`, `get(String)`, `set(Map<String,Object>)`, `set(String,Object)`, `clear()`。
- Produces: 最小测试覆盖 holder 的现有兼容语义。

- [ ] **Step 1: 创建失败测试**

Create `fw-plus-core/fw-plus-project/fw-plus-common/src/test/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolderTest.java`:
```java
package com.xiongx9527.fw.plus.common.validator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommonsValidatorHolderTest {

    @After
    public void tearDown() {
        CommonsValidatorHolder.clear();
    }

    @Test
    public void getCreatesWritableContext() {
        Map<String, Object> context = CommonsValidatorHolder.get();
        context.put("name", "fw-plus");

        Assert.assertEquals("fw-plus", CommonsValidatorHolder.get("name"));
    }

    @Test
    public void setEmptyMapClearsContext() {
        CommonsValidatorHolder.set("name", "fw-plus");

        CommonsValidatorHolder.set(Collections.<String, Object>emptyMap());

        Assert.assertNull(CommonsValidatorHolder.get("name"));
    }

    @Test
    public void setNonEmptyMapKeepsValues() {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("name", "fw-plus");

        CommonsValidatorHolder.set(context);

        Assert.assertEquals("fw-plus", CommonsValidatorHolder.get("name"));
    }
}
```

- [ ] **Step 2: 运行测试确认当前行为**

Run:
```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-common -Dtest=CommonsValidatorHolderTest test
```
Expected: 当前实现应 PASS；若失败，先记录失败输出并按 systematic-debugging 处理。

- [ ] **Step 3: 提交测试**

Run:
```bash
git add fw-plus-core/fw-plus-project/fw-plus-common/src/test/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolderTest.java
git commit -m "test: cover common validator holder context semantics"
```
Expected: commit succeeds.

## Task 3: 最小兼容优化 CommonsValidatorHolder

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolder.java`
- Modify: `openspec/changes/optimize-fw-plus-common/tasks.md`

**Interfaces:**
- Consumes: Task 2 的 `CommonsValidatorHolderTest`。
- Produces: 保持 public API 不变的更小实现。

- [ ] **Step 1: 简化空 Map 判断**

Replace `CommonsValidatorHolder.java` import and empty checks:
```java
package com.xiongx9527.fw.plus.common.validator;

import java.util.HashMap;
import java.util.Map;
```

`get()` body:
```java
public static Map<String,Object> get() {
    Map<String,Object> map = CONTEXT_HOLDER.get();
    if (map == null || map.isEmpty()) {
        map = new HashMap<>();
        set(map);
    }
    return map;
}
```

`set(Map<String,Object> map)` body:
```java
public static void set(Map<String,Object> map) {
    if (map == null || map.isEmpty()) {
        CONTEXT_HOLDER.remove();
    } else {
        CONTEXT_HOLDER.set(map);
    }
}
```

Rationale: 用 JDK `Map#isEmpty` 替代 `ObjectUtils.isEmpty`，删除一个不必要的 commons-lang3 工具调用；空 Map 语义保持不变。

- [ ] **Step 2: 运行 holder 测试**

Run:
```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-common -Dtest=CommonsValidatorHolderTest test
```
Expected: PASS.

- [ ] **Step 3: 如触及公共校验路径，运行下游最小测试**

Run:
```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='CommonsValidatorRequestBodyAdviceTest,CommonsValidatorResponseBodyAdviceTest' test
```
Expected: PASS.

- [ ] **Step 4: 勾选 OpenSpec 任务 2.1、2.2、3.1、3.2**

Edit `openspec/changes/optimize-fw-plus-common/tasks.md`:
```md
- [x] 2.1 对选定公共工具代码执行兼容性优化，优先删除、复用和简化。
- [x] 2.2 为涉及分支、状态或异常语义的改动补充最小测试。
- [x] 3.1 运行 `fw-plus-common` 相关 Maven 测试或最小可行测试命令。
- [x] 3.2 如影响下游公共调用方，运行对应最小下游验证。
```

- [ ] **Step 5: 提交实现**

Run:
```bash
git add fw-plus-core/fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolder.java \
  openspec/changes/optimize-fw-plus-common/tasks.md
git commit -m "refactor: simplify common validator holder emptiness checks"
```
Expected: commit succeeds.

## Task 4: 最终验证与范围检查

**Files:**
- Read: `openspec/changes/optimize-fw-plus-common/tasks.md`
- Read: `docs/superpowers/specs/2026-07-12-optimize-fw-plus-common-design.md`

**Interfaces:**
- Consumes: Tasks 1-3 的提交。
- Produces: build 阶段可进入 verify 的证据。

- [ ] **Step 1: 确认 OpenSpec tasks 全部勾选**

Run:
```bash
grep -n '\- \[ \]' openspec/changes/optimize-fw-plus-common/tasks.md || true
```
Expected: no output.

- [ ] **Step 2: 运行 common 最小测试**

Run:
```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-common -Dtest=CommonsValidatorHolderTest test
```
Expected: PASS.

- [ ] **Step 3: 运行下游最小测试**

Run:
```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='CommonsValidatorRequestBodyAdviceTest,CommonsValidatorResponseBodyAdviceTest' test
```
Expected: PASS.

- [ ] **Step 4: 确认无无关改动**

Run:
```bash
git status --short
git diff --stat 8317c3b8c36e073abb83fc78fb9d9bc91344ee02..HEAD
```
Expected: diff 只包含本 change 的计划、测试、holder 实现和 OpenSpec task 勾选。

- [ ] **Step 5: 代码审查前置说明**

If `review_mode=standard`, request one final lightweight code review before build guard. Critical findings must be fixed before verify.
