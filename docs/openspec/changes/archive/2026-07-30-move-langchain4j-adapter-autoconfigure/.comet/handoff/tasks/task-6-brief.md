### Task 6: 完整验证、任务同步和验证报告

**Files:**
- Create: `docs/superpowers/reports/2026-07-30-move-langchain4j-adapter-autoconfigure-verify.md`

**Interfaces:**
- Consumes: 前五个任务的提交、测试与文档证据。
- Produces: 可审计的 JDK 8 构建结果和验证报告；OpenSpec 任务状态由主协调会话在验收证据通过后同步。

- [ ] **Step 1: 运行统一 autoconfigure 全量测试**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am test
```

Expected: BUILD SUCCESS，0 failures/0 errors；报告记录实际测试数量。

- [ ] **Step 2: 运行统一模块与 adapter Starter 目标 Reactor**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter \
  -am test
```

Expected: BUILD SUCCESS；Maven Enforcer 通过且迁移后的 4 个 adapter 测试类均在统一模块执行。

- [ ] **Step 3: 执行规格、文档和 diff 校验**

```bash
openspec validate move-langchain4j-adapter-autoconfigure --strict
docs/ai-framework-migration/verify-migration-docs.sh
git diff --check 0212ccdebe60b458811d8ed0e3d20c47310a54d7..HEAD
```

Expected: OpenSpec valid、迁移文档通过、`git diff --check` 无输出。

- [ ] **Step 4: 编写验证报告**

报告必须列出：删除的公开坐标、46/4 文件迁移事实、双注册去重证据、缺 SDK 测试、Maven Enforcer、实际测试数量、未修改 Core/provider/AskData 边界和任何未执行验证。实现代理不得修改 plan 或 OpenSpec 复选框；主协调会话仅在对应证据真实通过后同步这些状态。

- [ ] **Step 5: 提交验证证据**

```bash
git add \
  docs/superpowers/reports/2026-07-30-move-langchain4j-adapter-autoconfigure-verify.md
git commit -m "test(ai): verify consolidated autoconfigure"
```

- [ ] **Step 6: 最终状态检查**

```bash
git status --short
git log --oneline 0212ccdebe60b458811d8ed0e3d20c47310a54d7..HEAD
```

Expected: 当前 change 的实现文件无未提交改动；输出只包含本计划产生的聚焦提交。若仍有 TinyEngine 等无关改动，只报告并保持不处理。
