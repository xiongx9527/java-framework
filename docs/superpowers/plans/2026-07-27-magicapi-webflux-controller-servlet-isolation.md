---
change: add-magicapi-webflux-support
design-doc: docs/superpowers/specs/2026-07-27-magicapi-webflux-controller-servlet-isolation-design.md
base-ref: 18be86e44a394e943c7177aa1cb7a1a27d8bcfb4
---

# Magic-API WebFlux Controller 与 Servlet 隔离实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 保持 Servlet 环境继续走现有 Magic-API 路径，同时让 WebFlux 环境进入新的 controller/管理接口适配路径，并隔离 Servlet-only bean。

**Architecture:** 采用“公共管理服务 + 双 Web 入口”。Servlet controller 保留现有注解、路径和语义，内部委托公共管理服务；WebFlux 新增独立 controller/handler 调用同一服务；Servlet-only filter/MVC/plugin controller registration 保持 Servlet 条件保护。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring Web MVC, Spring WebFlux, Magic-API 2.2.2, JUnit 5, Mockito, Spring Boot ApplicationContextRunner/WebApplicationContextRunner.

## Global Constraints

- Servlet 环境必须继续进入目前已有 Magic-API Servlet/MVC 自动配置、controller、filter 和 plugin controller registration 路径。
- WebFlux 环境必须进入新的 Magic-API controller/管理接口路径，不通过 Servlet/MVC controller path 暴露管理接口。
- 本 change 只覆盖 Magic-API 管理/controller 接口和 Servlet/WebFlux path 隔离，不实现完整动态 API runtime。
- 保持 JDK 1.8 兼容，不使用 Java 9+ API。
- 不修改 Spring Boot 2.7.18、Magic-API 2.2.2 或内部 Nexus 依赖版本。
- 不改变 Servlet/MVC 下已有路径、权限、响应结构和插件注册语义。

---

## File Structure

- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIController.java`
  - 保留 Servlet controller 入口，委托公共管理服务。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIManagementService.java`
  - 承载 option、reload、datasource options、datasource test 的公共逻辑。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIRequestAccess.java`
  - 管理接口授权所需的最小 request/user 访问抽象。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/ServletMagicAPIRequestAccess.java`
  - 从 `MagicHttpServletRequest` 适配现有授权检查。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/webflux/WebFluxMagicAPIRequestAccess.java`
  - 从 `ServerWebExchange` 提供 WebFlux 授权所需访问能力。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/webflux/MagicAPIWebFluxController.java`
  - WebFlux 管理接口入口。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/webflux/MagicAPIWebFluxAutoConfiguration.java`
  - Reactive Web 条件下导入 WebFlux 管理 controller。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/filter/auth/AuthConfiguration.java`
  - 补充 Servlet Web 条件，避免 WebFlux-only context 创建 Servlet filter registration。
- Review/Modify as needed: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
  - 保持 Servlet 条件，按需补充 WebFlux auto-configuration 导入或 spring.factories 注册。
- Review/Modify as needed: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-autoconfigure/src/main/java/com/xiongx9527/fw/plus/cloud/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
  - 仅在必要时调整条件边界。
- Review/Modify as needed: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
  - 仅在必要时调整条件边界。
- Create/Modify tests under `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/` and `.../controller/`.

### Task 1: 公共管理服务与 Servlet 回归入口

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIManagementService.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIRequestAccess.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/ServletMagicAPIRequestAccess.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIController.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIControllerJdbcOptionsTest.java`

**Interfaces:**
- Produces: `MagicAPIManagementService.options()`, `datasourceOptions(MagicAPIRequestAccess)`, `reloadResource(String, MagicAPIRequestAccess)`, `reloadSingleResource(String, String, MagicAPIRequestAccess)`, `test(DataSourceInfo)`.
- Produces: `MagicAPIRequestAccess.getMagicUser()` and, if needed, `MagicAPIRequestAccess.asMagicHttpServletRequest()` for existing interceptor compatibility.
- Consumes: `MagicConfiguration`, `MagicJdbcClassOptionProvider`, `MyDefaultMagicResourceService`, `JdbcUtils`, `MagicAPIConfigUtils`.

- [ ] **Step 1: Add service-focused tests before extraction**

Add tests to `MagicAPIControllerJdbcOptionsTest` verifying current behavior still works through controller after delegation:

```java
@Test
void controllerDelegatesOptionListWithoutChangingPayload() {
    MagicAPIController controller = new MagicAPIController(new MagicConfiguration(), mock(MagicJdbcClassOptionProvider.class));

    JsonBean<List<List<String>>> result = controller.options();

    @SuppressWarnings("unchecked")
    List<List<String>> options = (List<List<String>>) result.getData();
    assertThat(options).contains(Arrays.asList(
            Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue(),
            "避免重复提交超时时间(毫秒)",
            "1000"));
}
```

- [ ] **Step 2: Run the existing controller tests to capture baseline**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIControllerJdbcOptionsTest test
```

Expected: current tests pass before refactor; new delegation test initially fails only if helper/service class does not exist yet.

- [ ] **Step 3: Create `MagicAPIRequestAccess` and Servlet adapter**

Implement minimal Java 8-compatible interfaces/classes:

```java
public interface MagicAPIRequestAccess {
    MagicUser getMagicUser();
    MagicHttpServletRequest asServletRequest();
}
```

```java
public class ServletMagicAPIRequestAccess implements MagicAPIRequestAccess {
    private final MagicHttpServletRequest request;

    public ServletMagicAPIRequestAccess(MagicHttpServletRequest request) {
        this.request = request;
    }

    @Override
    public MagicUser getMagicUser() {
        return (MagicUser) request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
    }

    @Override
    public MagicHttpServletRequest asServletRequest() {
        return request;
    }
}
```

If the final implementation can avoid exposing `asServletRequest()`, prefer that tighter interface; keep tests aligned.

- [ ] **Step 4: Create `MagicAPIManagementService`**

Move non-Web business logic out of `MagicAPIController`:

```java
public JsonBean<List<List<String>>> options() {
    return new JsonBean<>(Stream.of(Options.values())
            .map(item -> Arrays.asList(item.getValue(), item.getName(), item.getDefaultValue()))
            .collect(Collectors.toList()));
}
```

For authorization, centralize calls so both Servlet and WebFlux can share semantics:

```java
boolean allowVisit(MagicAPIRequestAccess access, Authorization authorization) {
    if (authorization == null) {
        return true;
    }
    return configuration.getAuthorizationInterceptor()
            .allowVisit(access.getMagicUser(), access.asServletRequest(), authorization);
}
```

If WebFlux cannot supply a real `MagicHttpServletRequest`, introduce an adapter in Task 3 and test it.

- [ ] **Step 5: Thin `MagicAPIController` without changing annotations**

Keep existing annotations and method signatures where they are part of Servlet contract. Replace method bodies with service calls:

```java
@GetMapping("${magic-api.web:/magic-api/web}/datasource/options")
@ResponseBody
@Valid(requireLogin = true)
public JsonBean<MagicJdbcClassOptions> datasourceOptions(MagicHttpServletRequest request) {
    return managementService.datasourceOptions(new ServletMagicAPIRequestAccess(request));
}
```

- [ ] **Step 6: Run controller tests**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIControllerJdbcOptionsTest test
```

Expected: PASS.

- [ ] **Step 7: Commit Task 1**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller         fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIControllerJdbcOptionsTest.java
git commit -m "feat: extract MagicAPI management service"
```

### Task 2: Servlet-only 条件隔离

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/filter/auth/AuthConfiguration.java`
- Review/Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIServletIsolationAutoConfigurationTest.java`

**Interfaces:**
- Consumes: current Servlet `MagicAPIBaseAutoConfiguration` path.
- Produces: test proof that Servlet path remains current and WebFlux-only context excludes Servlet-only beans.

- [ ] **Step 1: Write Servlet isolation tests**

Create `MagicAPIServletIsolationAutoConfigurationTest` with ApplicationContextRunner/WebApplicationContextRunner tests. Include assertions:

```java
assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
assertThat(context).doesNotHaveBean(AuthFilter.class);
```

For Servlet context, assert the opposite where dependencies are present:

```java
assertThat(context).hasSingleBean(MagicAPIController.class);
```

Use existing project test patterns from `MateWebMvcValidatorAutoConfigurationTest` and Magic-API plugin tests.

- [ ] **Step 2: Add explicit Servlet condition to `AuthConfiguration`**

Add:

```java
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
```

Import `org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication`.

- [ ] **Step 3: Review `MagicAPIBaseAutoConfiguration`**

Confirm it already has:

```java
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
```

Do not weaken this condition. If plugin imports create Servlet-only beans in WebFlux tests, add targeted Servlet conditions to those specific configuration classes instead of broadening the base config.

- [ ] **Step 4: Run isolation tests**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIServletIsolationAutoConfigurationTest test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 2**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/filter/auth/AuthConfiguration.java         fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIServletIsolationAutoConfigurationTest.java
git commit -m "fix: isolate MagicAPI servlet-only auth configuration"
```

### Task 3: WebFlux 管理接口新路径

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/webflux/WebFluxMagicAPIRequestAccess.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/webflux/MagicAPIWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/webflux/MagicAPIWebFluxAutoConfiguration.java`
- Modify: auto-configuration registration file if this repo uses `spring.factories` for boot auto-config registration.
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/webflux/MagicAPIWebFluxControllerTest.java`

**Interfaces:**
- Consumes: `MagicAPIManagementService` from Task 1.
- Produces: WebFlux controller/handler exposing management endpoints through Reactive Web path.

- [ ] **Step 1: Locate auto-configuration registration**

Search:

```bash
rg -n "MagicAPIBaseAutoConfiguration|EnableAutoConfiguration" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources
```

Record whether new `MagicAPIWebFluxAutoConfiguration` must be added to `META-INF/spring.factories` or imported from an existing aggregate configuration.

- [ ] **Step 2: Write WebFlux controller tests first**

Test enabled/disabled registration and at least option list response:

```java
WebTestClient client = WebTestClient.bindToController(new MagicAPIWebFluxController(service, properties)).build();
client.get().uri("/magic-api/web/optionList")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.data").exists();
```

For disabled auto-config, use Reactive context runner with `fw.plus.magicapi.enabled=false` and assert the WebFlux controller bean is absent.

- [ ] **Step 3: Implement `MagicAPIWebFluxAutoConfiguration`**

Use Reactive condition:

```java
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
public class MagicAPIWebFluxAutoConfiguration {
}
```

Register `MagicAPIManagementService` only if not already provided by common configuration, and register `MagicAPIWebFluxController`/handler.

- [ ] **Step 4: Implement WebFlux request access**

Map `ServerWebExchange` to the minimal authorization context. If an exact `MagicHttpServletRequest` is unavoidable, create a narrow adapter class and cover it with tests. Keep it internal to the WebFlux controller package.

- [ ] **Step 5: Implement WebFlux endpoints**

Expose paths matching the Servlet logical contract:

```java
@GetMapping("${magic-api.web:/magic-api/web}/optionList")
public Mono<JsonBean<List<List<String>>>> options() {
    return Mono.just(managementService.options());
}
```

For reload and datasource options, call service with `new WebFluxMagicAPIRequestAccess(exchange)`.

- [ ] **Step 6: Run WebFlux tests**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIWebFluxControllerTest test
```

Expected: PASS.

- [ ] **Step 7: Commit Task 3**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/webflux         fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/webflux         fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources         fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/webflux
git commit -m "feat: add MagicAPI WebFlux management endpoints"
```

### Task 4: Cloud/AI 条件边界复核

**Files:**
- Review/Modify: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-autoconfigure/src/main/java/com/xiongx9527/fw/plus/cloud/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
- Review/Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
- Test as needed under Cloud/AI autoconfigure test trees.

**Interfaces:**
- Consumes: Boot WebFlux management endpoint path.
- Produces: no duplicated controller implementation in Cloud/AI; only condition boundary changes when necessary.

- [ ] **Step 1: Verify current Cloud/AI behavior**

Confirm both classes currently use Servlet condition. If they only import plugin configurations that are Servlet-specific, leave them Servlet-only.

- [ ] **Step 2: Add tests only for changed boundaries**

If code changes are needed, add context runner tests proving Servlet context still imports the existing configuration and Reactive context does not create Servlet-only beans.

- [ ] **Step 3: Run changed module tests**

For Cloud if changed:

```bash
cd fw-plus-cloud && ./mvnw -pl fw-plus-cloud-project/fw-plus-cloud-autoconfigure -Dtest=*MagicAPI* test
```

For AI if changed:

```bash
cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=*MagicAPI* test
```

- [ ] **Step 4: Commit Task 4 if files changed**

```bash
git add fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-autoconfigure fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "test: cover MagicAPI servlet and webflux boundaries"
```

If no files changed, record the no-op reason in the final verification report instead of committing.

### Task 5: Final verification and OpenSpec task sync

**Files:**
- Modify: `openspec/changes/add-magicapi-webflux-support/tasks.md`
- No implementation source changes unless previous verification finds a bug.

**Interfaces:**
- Consumes: all previous tasks.
- Produces: checked OpenSpec tasks and build evidence.

- [ ] **Step 1: Run focused Boot tests**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIControllerJdbcOptionsTest,MagicAPIServletIsolationAutoConfigurationTest,MagicAPIWebFluxControllerTest test
```

Expected: PASS.

- [ ] **Step 2: Run relevant existing Magic-API regression tests**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIBaseAutoConfigurationRepeatSubmitTest,MagicAPIBaseAutoConfigurationSentinelGuardTest test
```

Expected: PASS.

- [ ] **Step 3: Run OpenSpec validation**

Run:

```bash
openspec validate add-magicapi-webflux-support --strict
```

Expected: `Change 'add-magicapi-webflux-support' is valid`.

- [ ] **Step 4: Check Java compatibility and dependency drift**

Run:

```bash
git diff -- fw-plus-boot fw-plus-cloud fw-plus-ai | rg -n "List\.of\(|Map\.of\(|Set\.of\(|Optional\.orElseThrow\(|<magic-api|org.ssssssss|2\.2\.2" || true
```

Expected: no Java 9+ APIs introduced and no Magic-API dependency version changes.

- [ ] **Step 5: Mark OpenSpec tasks complete only after verification**

Update `openspec/changes/add-magicapi-webflux-support/tasks.md` checkboxes corresponding to completed implementation work.

- [ ] **Step 6: Commit final task sync**

```bash
git add openspec/changes/add-magicapi-webflux-support/tasks.md
git commit -m "chore: complete MagicAPI WebFlux controller tasks"
```

## Self-Review

- Spec coverage: tasks cover Servlet existing path, WebFlux new management endpoint path, response compatibility, Servlet-only bean isolation, and explicit dynamic API runtime non-goal.
- Placeholder scan: 未发现占位符或未完成标记。
- Type consistency: service/controller names are stable across tasks; implementation may tighten `MagicAPIRequestAccess` if tests prove `asServletRequest()` is unnecessary.
