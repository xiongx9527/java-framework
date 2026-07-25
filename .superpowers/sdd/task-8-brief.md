### Task 8: AskDataService 默认实现与完整自动配置测试

**Files:**
- Create: `.../askdata/DefaultAskDataService.java`
- Modify: `.../askdata/autoconfigure/AskDataAutoConfiguration.java`
- Test: `.../askdata/DefaultAskDataServiceTest.java`
- Test: `.../askdata/autoconfigure/AskDataAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `AskDataFlowExecutor`.
- Produces: `AskDataService.ask(AskDataRequest request)` and all default replaceable Spring Beans.

- [ ] **Step 1: Write failing service tests**

Assert null request, blank question, delegated success, and delegated failure:

```java
AskDataResult result = service.ask(request);
assertTrue(result.isSuccess());
assertEquals("select count(*) from orders limit 100", result.getSql());
```

For blank question, assert `success=false`, `errorCode=MODEL_RESPONSE_INVALID`, and no flow delegation.

- [ ] **Step 2: Write failing auto-configuration tests**

Using `ApplicationContextRunner`, assert default beans exist: `AskDataProperties`, `AskDataModelRegistry`, `AskDataVectorRetriever`, `AskDataMetadataProvider`, `AskDataPromptTemplateProvider`, `AskDataSqlValidator`, `AskDataSqlExecutor`, `AskDataFlowExecutor`, `AskDataConversationStore`, `AskDataContextCompressor`, `AskDataService`.

Also register custom `AskDataSqlValidator` and custom `AskDataService` beans and assert `@ConditionalOnMissingBean` preserves user beans.

- [ ] **Step 3: Run tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataServiceTest,AskDataAutoConfigurationTest test`

Expected: FAIL because service and complete auto-configuration are missing.

- [ ] **Step 4: Implement service**

Required class:

```java
public class DefaultAskDataService implements AskDataService {
    private final AskDataFlowExecutor flowExecutor;

    public DefaultAskDataService(AskDataFlowExecutor flowExecutor) {
        this.flowExecutor = flowExecutor;
    }

    @Override
    public AskDataResult ask(AskDataRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return AskDataResult.failure(AskDataErrorCode.MODEL_RESPONSE_INVALID, "question must not be blank");
        }
        return flowExecutor.execute(request);
    }
}
```

- [ ] **Step 5: Complete auto-configuration**

Add `@ConditionalOnProperty(prefix = "fw.plus.ai.askdata", name = "enabled", havingValue = "true", matchIfMissing = true)`. Register every default Bean with `@ConditionalOnMissingBean`. Use `ObjectProvider` for optional dependencies: `LangChain4jModelInfo`, `ContentRetriever`, `JdbcTemplate`, custom sub-agents and custom delegates.

- [ ] **Step 6: Run tests**

Run the command from Step 3.

Expected: PASS.

- [ ] **Step 7: Commit**

Commit message: `feat(askdata): wire service auto configuration`.

