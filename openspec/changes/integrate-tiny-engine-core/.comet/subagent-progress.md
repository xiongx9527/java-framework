# Subagent Progress

- Change: integrate-tiny-engine-core
- Review mode: thorough
- TDD mode: tdd
- Build mode: subagent-driven-development

## Current Task

- Task: Task 5: 迁移 Mapper 接口、XML 与 SQL 资源清单
- OpenSpec mapping: 3.2, 3.3, 3.4
- Stage: task-review
- Implementer: DONE_WITH_CONCERNS
- Reviewer: Linnaeus completed first task review
- Fixer: pending dispatch
- Review/fix rounds: 1
- Review result: Needs fixes

## Evidence

- RED: implementer reported `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineMapperResourceTest,TinyEngineSqlScriptManifestTest test` failed as expected before implementation with missing resources/classes.
- GREEN: coordinator reran `./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineMapperResourceTest,TinyEngineSqlScriptManifestTest test` from `fw-plus-core`; 5 tests passed, 0 failures, 0 errors.
- Extra: coordinator reran `./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile`; build success. Forbidden API/old-package scan returned no matches. Mapper/XML/SQL counts are 28/28/25.
- Commits: 62936df0e12a50267474973d57a1c1dcdcbb5f96
- Changed files: 28 mapper interfaces, 28 mapper XML files, 25 SQL resources, `TinyEngineSqlScriptManifest`, SQL manifest, and 2 tests under `fw-plus-tiny-engine-core`.
- Risk signals: SQL/schema resources, implementer `DONE_WITH_CONCERNS`, diff over 200 lines.
- Concern: source PostgreSQL schema resource is empty; migrated resource is present in the manifest but has 0 bytes. Core only packages and exposes the script list; it does not execute SQL or auto-initialize databases.
- Reviewer findings: Critical PostgreSQL manifest entry points to 0-byte resource; Important mapper test does not scan actual resources or load XML through MyBatis; Important default manifest includes `init_data_for_test_v1.0.0.sql`; Important `ResourceMapper.xml#findResourcesByAppId` has no mapper interface method; Minor AppMapper sort branch uses `create_time` while column is `created_time`.
