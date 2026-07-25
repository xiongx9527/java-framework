### Task 9: Full verification and Maven build gates

**Files:**
- Test-only changes if prior verification exposes compile or behavior gaps.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: verified starter module and aggregate AI build.

- [ ] **Step 1: Run focused starter test suite**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter test`

Expected: PASS. All AskData unit and context tests pass.

- [ ] **Step 2: Run starter module install with upstream dependencies**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -am clean install -DskipTests`

Expected: PASS. Maven compiles upstream AI modules and installs the new starter.

- [ ] **Step 3: Run AI starter aggregate compile**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters -am clean install -DskipTests`

Expected: PASS. Existing AI starters continue to compile with the new module entry.

- [ ] **Step 4: Run dependency tree check for new starter**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter dependency:tree -Dincludes=com.xiongx9527.fw.plus.ai,dev.langchain4j,org.springframework`

Expected: output includes `fw-plus-ai-boot-autoconfigure`, `fw-plus-ai-langchain4j-spring-boot-starter`, Spring Boot 2.7.x managed dependencies, and LangChain4j 0.35.0 managed dependencies. Output does not include SQLBot runtime or UI dependencies.

- [ ] **Step 5: Inspect Java 8 compatibility**

Run: `cd fw-plus-ai && rg -n "List\\.of|Map\\.of|Map\\.copyOf|Set\\.of|Optional\\.orElseThrow\\(\\)" fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/main/java`

Expected: no matches.

- [ ] **Step 6: Inspect forbidden marker text and brand leakage**

Run: `cd fw-plus-ai && rg -n "SQLBot|sqlbot|待定|稍后实现|补充细节" fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter`

Expected: no matches except test method names or comments deliberately asserting absence of SQLBot text.

- [ ] **Step 7: Commit verification fixes**

If Steps 1-6 required test-only or compile-fix changes, commit message: `test(askdata): complete starter verification`.

Expected: `git status --short` shows only intentional committed changes after commit.

## Self-Review

**Spec coverage:** This plan maps Maven module接入 to Task 1, core API to Task 2, 多模型默认模型和请求级 `modelKey` to Task 3, 向量复用 to Task 4, Prompt/方言 to Task 4, SQL 安全和执行 to Task 5, 多 Agent/子 Agent to Task 6, 上下文压缩和崩溃后恢复 to Task 7, 自动配置 to Tasks 1 and 8, 测试与 Maven Wrapper 验证 to every task and Task 9.

**Placeholder scan:** Plan text avoids forbidden marker text and every task includes exact files, signatures, commands and expected results.

**Type consistency:** Core signatures use `AskDataRequest`, `AskDataResult`, `AskDataModelRegistry`, `AskDataVectorRetriever`, `AskDataFlowExecutor`, `AskDataConversationStore`, and `AskDataContextCompressor` consistently across producing and consuming tasks.
