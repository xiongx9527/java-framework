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

## Fix Round 1

### Scope

- The shared Magic footer toolbar now preserves plugin `beforeClose` metadata, keeps each dynamic component instance, and waits for the hook before removing a closeable tab. A missing hook still closes immediately; `false`, a rejected hook, or a repeated concurrent click keeps the tab open.
- The Tinyflow workbench observes the host-provided current resource, including a Vue ref whose `value` getter is inherited, loads the first open resource and tree switches without a watcher loop, and exposes `beforeClose()` so the shared host delegates to the real dirty-state confirmation path.
- Magic `HttpResponse.exception(...)` now rejects the Tinyflow request promise. UI stylesheet failures show an explicit error, Starter asset URLs use `constants.BASE_URL`, and the toolbar resource ID input remains usable before a resource is loaded.

### Regression Coverage

1. Added shared-toolbar close tests for dirty cancellation, accepted close, legacy no-hook close, rejected hooks, and concurrent clicks.
2. Added Tinyflow tests for `HttpResponse.exception`, host resource synchronization, `BASE_URL`, stylesheet failure, and built IIFE metadata.
3. Verified the Tinyflow console with:

   ```text
   npm run build && npm test
   ```

   Result: `tests 18`, `pass 18`, `fail 0`. The IIFE is `18.22 kB` before gzip.

### Integration Verification

1. The exact Zulu 8 AI reactor completed successfully:

   ```text
   JAVA_HOME="$(/usr/libexec/java_home -v 1.8)" mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow -am package -DskipTests=false
   ```

   A final rerun after the Vue ref compatibility regression ended with `BUILD SUCCESS` at `2026-08-06T12:54:16+08:00`.

2. The Boot full reactor was attempted with the Magic Starter and `-am`, but it stopped in the unrelated `fw-plus-boot-autoconfigure` test compilation. Existing SpringBean tests referenced missing `magicapi.plugin.springbean` model/module/runtime/service packages, so the target Starter was skipped. This round does not modify that concurrent SpringBean work.

3. The Magic Starter itself completed its normal Zulu 8 package flow without skipping tests:

   ```text
   JAVA_HOME="$(/usr/libexec/java_home -v 1.8)" mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml package -DskipTests=false
   ```

   It ended with `BUILD SUCCESS` at `2026-08-06T12:48:56+08:00`.

4. The resulting Starter JAR's active `magic-editor/index.html` references `assets/index.637fdba1.js`; that bundle contains `requestToolbarClose`, `beforeClose`, and `componentInstance`. The Tinyflow plugin JAR contains only `magic-editor/plugins/magic-tinyflow.1.0.0.iife.js`, no Starter UI assets, and its embedded POM has only the direct `fw-plus-ai-tinyflow-spring-boot-starter` dependency.

### Known Build Warnings

- The existing Magic console Vite build reports the non-module `config-js` script and large chunk warnings.
- npm reports inherited deprecated packages, audit findings, and pending native install-script approvals. No dependency changes were made in this round.

## Fix Round 2

### Scope

- Made the workflow toolbar genuinely closeable with `allowClose: true`; the shared footer close icon now invokes the exposed `beforeClose()` path, so clean work closes directly, dirty work asks for confirmation, cancellation preserves the tab, and confirmed close destroys the mounted UI.
- Limited resource synchronization to an actively opened `tinyflow` resource. The workbench observes both host-provided `opened` and `info` refs, including Vue prototype-backed refs, so API/function/other toolbar changes cannot request Tinyflow data or trigger a dirty confirmation.
- Added a load generation guard. When A and B overlap, only the newest Tinyflow load can update the resource, `loadedId`, canvas data, or dirty flag; a late success or failure from A is discarded.
- Replaced the shared close callback's captured array index with identity-based removal. Concurrent close operations, reorder, add/remove activity, and a tab already removed while a hook awaits now cannot remove a different toolbar.
- Updated Magic response exception handling for the actual `(code, message, response)` callback signature and use the supplied message safely.
- Made `npm test` build the Tinyflow IIFE before executing its metadata test, so a clean `npm ci && npm test && npm run build` sequence does not depend on an untracked `dist/` directory.

### RED/GREEN Coverage

1. Added regression tests for the real workflow `allowClose` metadata and built IIFE, direct clean close, dirty confirmed/cancelled close, and shared close reorder/removal behavior.
2. Added tests proving non-Tinyflow opened state performs no request or dirty confirmation, while Tinyflow resources still synchronize.
3. Added delayed A/B load tests for both late success and late failure, with B remaining the only committed resource.
4. Added a real three-argument `HttpResponse.exception(-1, message, response)` mock.
5. From a clean Tinyflow console directory, verified:

   ```text
   npm ci && npm test && npm run build
   ```

   Result: `tests 23`, `pass 23`, `fail 0`; final IIFE size is `19.35 kB` before gzip.

### Integration Verification

1. Shared Magic console `npm run build` completed. Existing Vite warnings remain limited to the `config-js` non-module script and chunk-size advisory.
2. The exact Zulu 8 AI reactor completed with exit code `0` and `BUILD SUCCESS` at `2026-08-06T13:09:55+08:00`:

   ```text
   JAVA_HOME="$(/usr/libexec/java_home -v 1.8)" mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow -am package -DskipTests=false
   ```

3. The Zulu 8 Magic Starter module package completed with exit code `0` and `BUILD SUCCESS` at `2026-08-06T13:10:51+08:00`:

   ```text
   JAVA_HOME="$(/usr/libexec/java_home -v 1.8)" mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml package -DskipTests=false
   ```

4. The full Boot `-am` reactor was rerun and ended with exit code `1` in the concurrently modified `fw-plus-boot-autoconfigure` test suite: `MagicAPIServletIsolationAutoConfigurationTest` sees two `JavaSourceAnalyzer` beans (`javaEntitySourceAnalyzer`, `springBeanJavaSourceAnalyzer`), and `TinyEngineAutoConfigurationTest.sqlResourcesAreDiscoverableButNotAutoInitializedByDefault` fails its resource assertion. The Magic Starter was skipped; neither failure is changed in this round.

5. The final Boot Starter JAR's active `magic-editor/index.html` references `assets/index.ae394546.js`, which contains `requestToolbarClose`, `removeToolbarByIdentity`, `beforeClose`, and `componentInstance`. The final Tinyflow plugin JAR contains only `magic-editor/plugins/magic-tinyflow.1.0.0.iife.js`, no Starter UI files, and retains only the direct Starter dependency.

## Fix Round 3

### Scope

- Resource synchronization now requires the currently opened host resource to be a `tinyflow` item with the same ID as the latest `info`. `OPEN_GROUP` updates that only replace `info` with a group no longer load a different resource or invoke the dirty confirmation for the previously opened workflow.
- The Tinyflow UI mount now has a disposal-aware lifecycle. It discards loader resolutions and rejections after unmount, avoids producing mount options from an unmounted canvas, and destroys a UI host created while disposal begins before attachment.
- Component operations use a pending-operation counter and disposal guards, so stale asynchronous operations cannot clear a newer loading state or update UI state after unmount.

### Regression Coverage

1. The console regression suite adds the real `OPEN_GROUP` sequence: a dirty workflow remains loaded while `info` changes to Tinyflow, API, and other group IDs without another request or confirmation.
2. The lifecycle coverage verifies deferred loader resolve/reject after disposal returns `false` without mounting or reading mount options, and verifies an already-created host is destroyed before it can attach.
3. From a clean console directory, this Round 3 revision completed:

   ```text
   npm ci && npm test && npm run build
   ```

   Result: `tests 26`, `pass 26`, `fail 0`; the generated IIFE is `21.12 kB` before gzip.

### Integration Verification

1. The exact Zulu 8 AI reactor was rerun after the Round 3 code and completed with exit code `0` and `BUILD SUCCESS` at `2026-08-06T13:33:03+08:00`:

   ```text
   JAVA_HOME="$(/usr/libexec/java_home -v 1.8)" mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow -am package -DskipTests=false
   ```

2. The Zulu 8 Magic Starter module package was rerun after the Round 3 code and completed with exit code `0` and `BUILD SUCCESS` at `2026-08-06T13:35:50+08:00`:

   ```text
   JAVA_HOME="$(/usr/libexec/java_home -v 1.8)" mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml package -DskipTests=false
   ```

3. JAR inspection confirms the final Boot Starter `magic-editor/index.html` activates `assets/index.07a100ea.js`; that asset contains `requestToolbarClose`, `removeToolbarByIdentity`, `beforeClose`, and `componentInstance`. The final Tinyflow JAR contains exactly `magic-editor/plugins/magic-tinyflow.1.0.0.iife.js` under the plugin resources, with no Starter UI entries. Its IIFE retains the Round 3 `createWorkbenchMountLifecycle`, `openedResource`, and `pendingOperations` logic.

4. The required complete Boot `-am` reactor was rerun with Zulu 8 and ended with exit code `1` at `2026-08-06T13:41:41+08:00`, before the Magic Starter module. The concurrently modified `fw-plus-boot-autoconfigure` suite ran 513 tests and failed three: `TinyEngineAutoConfigurationTest.sqlResourcesAreDiscoverableButNotAutoInitializedByDefault`, `JavaEntityMagicResourceStorageTest.validateCompilesTheCompletePersistedCandidateBeforeSave` (the dynamic entity compiler reached its 30000 ms limit), and `MagicAPIServletIsolationAutoConfigurationTest.retainsServletAuthBeansAndMagicApiControllerInServletWebContext` (two `JavaSourceAnalyzer` beans: `javaEntitySourceAnalyzer` and `springBeanJavaSourceAnalyzer`). These failures are outside Round 3 and were not changed or staged.
