# Subagent Progress

- Change: integrate-tiny-engine-core
- Review mode: thorough
- TDD mode: tdd
- Build mode: subagent-driven-development

## Current Task

- Task: Task 3: 迁移 Java 8 兼容工具与 TypeHandler
- OpenSpec mapping: 2.2
- Stage: task-review
- Implementer: DONE
- Reviewer: CHANGES_REQUESTED
- Review/fix rounds: 1
- Fixer: DONE
- Fix commit: fe70d0dd506d26a6ae6af224100ccf213b7e602d
- Re-review: pending
- Review result: Important `TinyEngineHashUtils` sorts only the outer map; nested maps can still hash nondeterministically through `toString()`. Recursively normalize maps and add a nested map insertion-order regression test.

## Evidence

- RED: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest test` failed before implementation because fastjson2 and target utility/typehandler classes were absent.
- GREEN: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest test` passed with 6 tests, 0 failures, 0 errors. Coordinator re-ran this command successfully on 2026-07-27.
- Extra: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile` passed. Banned API scan for Web/OpenAPI/Jakarta/Java 9+ APIs and `com.tinyengine.it` produced no matches.
- Fix RED: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest test` failed with the new nested map insertion-order regression before recursive hash normalization.
- Fix GREEN: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest test` passed with 7 tests, 0 failures, 0 errors after recursive hash normalization. Coordinator re-ran this command successfully on 2026-07-27.
- Commits: 09f4fa6ba3fbf1373658a25eb4b62ed1e450fcba, fe70d0dd506d26a6ae6af224100ccf213b7e602d
- Changed files: `fw-plus-tiny-engine-core/pom.xml`, `common/util/*`, `common/typehandler/*`, `TinyEngineUtilityTest.java`
- Risk signals: public utility and TypeHandler API added; diff exceeds 200 lines and requires thorough task review. Fixer noted cyclic Map/collection references are not handled and custom object stability still depends on `toString()`.
