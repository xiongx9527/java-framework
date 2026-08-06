# Task 9 Report: Magic Resource and Toolbars Host

## Scope

- Added the Tinyflow Magic-API console plugin under `fw-plus-ai-magicapi-plugin-tinyflow`.
- Registered the singular `tinyflow` resource and the workflow and knowledge-base toolbars.
- The plugin loads only Starter-hosted UI resources at `/fw-plus/tinyflow-ui/1.3.6/tinyflow-ui.js` and `.css`; it does not package the Starter UI.

## RED Evidence

1. Before the console project existed:

   ```text
   cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console && npm test
   ```

   Exit code `254`: `ENOENT`, because `package.json` did not exist.

2. After adding the test runner but before implementation:

   ```text
   npm test
   ```

   Exit code `1`; all 7 tests failed with expected `ERR_MODULE_NOT_FOUND` errors for `tinyflow-ui-loader.js` or `tinyflow-service.js`.

3. A later TDD increment added the unmount contract:

   ```text
   npm test
   ```

   Exit code `1`; 7 passed and `destroys the mounted UI when the workbench unmounts` failed with `TypeError: workbench.destroy is not a function`.

## GREEN Evidence

1. Console verification:

   ```text
   cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console && npm ci && npm test && npm run build
   ```

   `npm ci` succeeded. Node test output: `tests 8`, `pass 8`, `fail 0`. Vite 5.4.14 produced only `dist/magic-tinyflow.1.0.0.iife.js`.

2. Exact reactor verification, run with Zulu 8 (`1.8.0_432`) and Maven 3.6.3:

   ```text
   JAVA_HOME="$(/usr/libexec/java_home -v 1.8)" mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow -am package -DskipTests=false
   ```

   The final 15-module reactor completed successfully after the preceding boundary fix `c36e6179`. Current Surefire report aggregation: `393` tests, `0` failures, `0` errors.

3. JAR inspection:

   ```text
   jar tf fw-plus-ai/.../fw-plus-ai-magicapi-plugin-tinyflow/target/fw-plus-ai-magicapi-plugin-tinyflow-0.1.1-SNAPSHOT.jar
   ```

   The plugin JAR contains exactly the plugin asset at `magic-editor/plugins/magic-tinyflow.1.0.0.iife.js`, plus Maven metadata. It contains no `tinyflow-ui.js`, `tinyflow-ui.css`, or other Starter UI resource. Its embedded POM has one direct dependency: `fw-plus-ai-tinyflow-spring-boot-starter`.

## Implementation Notes

- `tinyflow-ui-loader.js` derives versioned UI URLs from the Magic request context path and reports static-resource, missing-constructor, and version diagnostics instead of leaving a blank canvas.
- `tinyflow-service.js` keeps dirty state until save completes, blocks run while dirty, confirms before destructive switching/close, and destroys the mounted UI during unmount.
- `tinyflow-workbench.vue` delegates `setData`, `getData`, `onDataChange`, and `destroy` through the Starter global `FwPlusTinyflowUi.mount` host.
- The knowledge-base toolbar is a routed empty-state page only; knowledge-base management is intentionally outside Task 9.

## Files

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/pom.xml`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/package.json`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/package-lock.json`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/vite.config.js`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/test/`

## Self-review

- Resource registration uses the required singular `resource` key and `type: 'tinyflow'`.
- Toolbar metadata matches the required orders, icons, administrator restriction, close permission, and full-screen knowledge-base entry.
- The console bundle contains no Starter UI implementation or `@tinyflow-ai/ui` dependency.
- `git diff --check` passes for the Task 9 module and report.
- Generated `node_modules` and `dist` were removed before staging.

## Concerns

- `npm ci` reports two upstream audit findings, one moderate and one high. They were not changed because the console only uses the locked Vite/Vue build toolchain.
- During the first full reactor attempt, `NeutralAiAutoConfigurationBoundaryTest` correctly exposed a preceding Task 8 boundary regression in `MagicApiTinyflowModelProvider`. That fix was delivered in `c36e6179`; the final exact reactor run above passed.
