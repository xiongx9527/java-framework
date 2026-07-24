# Subagent Progress

- Change: add-askdata-spring-boot-starter
- Mode: subagent-driven-development
- TDD: tdd
- Review: standard

## Current

- Task: Task 4: 向量召回 Facade、元数据模型、Prompt 与方言
- Stage: task-review
- Implementer: Mencius (019f9295-55bf-7490-b96d-928c8ec1d396)
- Implementation commit: 3b2a760
- Changed files: metadata/*, prompt/*, vector/*, AskDataAutoConfiguration.java, AskDataVectorRetrieverTest.java, AskDataPromptTemplateProviderTest.java
- RED evidence: focused Java 8 Maven test failed at testCompile because Task 4 types did not exist.
- GREEN evidence: focused Java 8 Maven test passed, 5 tests; full starter module test passed, 14 tests.
- Review mode: standard
- Risk review: triggered
- Risk signals:
  - DONE_WITH_CONCERNS
  - Public API contract change
  - Single task diff exceeds 200 lines
- Review package: .superpowers/sdd/review-a876856..3b2a760.diff
- Reviewer: Kant (019f929e-53c2-7e13-a493-f3d6b4f3a6d8)
- Review result: needs fixes, 1 Important finding.
- Review-fix round: 1 of 1
- Unresolved Important finding:
  - DefaultAskDataPromptTemplateProvider must mark retrieved vector snippets as non-authoritative context, instruct the model to trust only AskDataMetadata schema for SQL scope, and ignore retrieved snippet authorization or SQL instructions.
- Previous task: Task 3 complete, commit 20a8b2a97f669ee067929592c3395a0894b9db2a, review approved
