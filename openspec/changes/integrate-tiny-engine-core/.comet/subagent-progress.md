# Subagent Progress

- Change: integrate-tiny-engine-core
- Review mode: thorough
- TDD mode: tdd
- Build mode: subagent-driven-development

## Current Task

- Task: Task 4: 迁移领域实体与 DTO
- OpenSpec mapping: 3.1, 3.5
- Stage: implementing
- Implementer: DONE
- Reviewer: CHANGES_REQUESTED
- Review/fix rounds: 1
- Fixer: DONE
- Fix commit: b5bd8292faabf587c3a3b34d013da760f7825578
- Re-review: CHANGES_REQUESTED
- Second fix commit: a344feb55019966216c1951bf60c79dd2d6a230b
- Second re-review: pending
- Review result: Critical JSON TypeHandlers use `JSONObject`/`JSONArray` generics but migrated model fields are `Map`/`List`, causing MyBatis bridge casts and `ClassCastException` on normal `HashMap`/`ArrayList` writes. Important: several JSON Map fields still use `JacksonTypeHandler`, and `BlockHistory.contentBlocks` / `Platform.prettierOpts` have wrong handler choices. Minor: model contract test does not verify TypeHandler assignment or explicit AI/Chat exclusions.
- Re-review result: Important entities with JSON `Map`/`List` fields use field `typeHandler` annotations but lack `@TableName(autoResultMap = true)`, so MyBatis-Plus select mappings may not apply the TypeHandlers. Minor: add assertions that `Block`/`BlockDto`/`BlockParam.contentBlocks` remain String fields without JSON handlers.

## Evidence

- RED: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineModelContractTest test` failed before implementation with `ClassNotFoundException` for the first checklist model class.
- GREEN: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineModelContractTest test` passed with 1 test, 0 failures, 0 errors. Coordinator re-ran this command successfully on 2026-07-27.
- Extra: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile` passed. Banned API scan for Web/OpenAPI/Jakarta/Java 9+ APIs and `com.tinyengine.it` produced no matches. Additional AI/Chat/OpenAPI keyword scan in migrated model/checklist files produced no matches.
- Fix RED: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest,TinyEngineModelContractTest test` failed with expected `ClassCastException`/handler mismatch regressions before the fix.
- Fix GREEN: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest,TinyEngineModelContractTest test` passed with 12 tests, 0 failures, 0 errors after aligning handlers. Coordinator re-ran this command successfully on 2026-07-27.
- Second fix RED: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineModelContractTest test` failed on the new autoResultMap regression before adding `autoResultMap = true`.
- Second fix GREEN: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest,TinyEngineModelContractTest test` passed with 14 tests, 0 failures, 0 errors after enabling auto result maps. Coordinator re-ran this command successfully on 2026-07-27.
- Commits: aa09308574eebc2217ee3d0251b780f0d337aa1d, b5bd8292faabf587c3a3b34d013da760f7825578, a344feb55019966216c1951bf60c79dd2d6a230b
- Changed files: 30 entity classes, 51 DTO classes, `TinyEngineModelContractTest`, `TinyEngineUtilityTest`, `tinyengine/model-domain-checklist.txt`, `JsonObjectTypeHandler`, `JsonArrayTypeHandler`, and `fw-plus-tiny-engine-core/pom.xml` with `jackson-annotations`.
- Risk signals: public model API added; diff exceeds 200 lines; POM dependency added; thorough task review required.
