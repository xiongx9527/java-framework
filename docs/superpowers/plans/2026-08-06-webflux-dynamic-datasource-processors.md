# WebFlux Dynamic DataSource Processors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 WebFlux 请求中的同步 JDBC/MyBatis 调用可以继续使用 `@DS("#header.xxx")` 和 `@DS("#session.xxx")` 选择数据源。

**Architecture:** 保留现有 Servlet 读取路径，新增不可变 WebFlux 请求快照和严格作用域的 ThreadLocal 持有器。Reactive `WebFilter` 把快照写入 Reactor Context，全局但按键隔离的 Reactor lifter 仅在同步信号回调期间将快照桥接到 ThreadLocal；Reactive 条件自动配置负责注册和销毁桥接。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Spring WebFlux 5.3.x、Reactor Core 3.4.x、dynamic-datasource、JUnit 5、AssertJ、WebTestClient。

## Global Constraints

- 生产代码必须兼容 Java 8，不使用 `List.of()`、`Map.of()`、`Map.copyOf()` 等 Java 9+ API。
- 保持 `#header.<name>`、`#session.<name>` 语法以及 Header -> Session -> SpEL 处理器链顺序不变。
- Servlet 请求上下文优先于 WebFlux 快照，现有 Servlet 行为必须回归通过。
- WebFlux 桥接仅在 `ConditionalOnWebApplication.Type.REACTIVE` 且 WebFlux 类存在时装配。
- 不支持 `Mono`/`Flux` 内延迟 JDBC 操作的数据源路由传播，不改造 dynamic-datasource 的 ThreadLocal 模型。
- 当前 `dev` 工作区包含用户的其他未提交改动；每次只暂存本计划列出的文件。
- `fw-plus-boot` 缺少 `.mvn/wrapper` 运行文件，验证使用 PATH 中的系统 `mvn`。

---

## File Map

- `DsRequestContext.java`：不可变、与 Servlet/WebFlux API 解耦的 Header/Session 请求快照。
- `DsRequestContextHolder.java`：当前同步回调的请求快照和可嵌套恢复的 `Scope`。
- `DsHeaderProcessor.java`：Servlet 读取失败后回退到请求快照。
- `DsSessionProcessor.java`：Servlet 读取失败后回退到请求快照。
- `DsWebFluxContextSubscriber.java`：在 Reactor 信号回调期间打开并关闭请求快照作用域。
- `DsWebFluxContextHook.java`：幂等注册/注销唯一 Reactor operator hook。
- `DsWebFluxRequestContextFilter.java`：从 `ServerWebExchange` 构造快照并写入 Reactor Context。
- `DynamicDataSourceWebFluxAutoConfiguration.java`：只在 Reactive Web 应用中创建 hook 和 filter。
- `JdbcAutoConfiguration.java`：导入 Reactive 动态数据源自动配置。
- 对应测试文件：验证处理器、作用域、桥接、条件装配和真实 WebFlux 请求。

---

### Task 1: 请求快照和处理器兼容回退

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsRequestContext.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsRequestContextHolder.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsHeaderProcessor.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsSessionProcessor.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsHeaderProcessorTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsSessionProcessorTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsRequestContextHolderTest.java`

**Interfaces:**
- Produces: `new DsRequestContext(Map<String, ? extends List<String>>, Map<String, ?>)`、`String getHeader(String)`、`Object getSessionAttribute(String)`。
- Produces: `DsRequestContextHolder.getContext()` 和 `DsRequestContextHolder.open(DsRequestContext)`；`open` 返回 `DsRequestContextHolder.Scope extends AutoCloseable`，其 `close()` 不声明受检异常。
- Consumes: 现有 `RequestContextHolder` 和 `ServletRequestAttributes` 行为。

- [ ] **Step 1: 为 WebFlux 快照回退写失败测试**

在两个现有处理器测试的 `tearDown()` 中增加 `DsRequestContextHolder.clear()`，并添加：

```java
@Test
void determineDatasourceReadsReactiveHeaderSnapshot() {
    Map<String, List<String>> headers = Collections.singletonMap(
            "DS", Collections.singletonList("tenant_reactive"));
    DsRequestContext context = new DsRequestContext(headers, Collections.<String, Object>emptyMap());

    try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(context)) {
        assertEquals("tenant_reactive", processor.doDetermineDatasource(null, "#header.ds"));
    }
}

@Test
void determineDatasourceReadsReactiveSessionSnapshot() {
    DsRequestContext context = new DsRequestContext(
            Collections.<String, List<String>>emptyMap(),
            Collections.<String, Object>singletonMap("ds", "tenant_session"));

    try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(context)) {
        assertEquals("tenant_session", processor.doDetermineDatasource(null, "#session.ds"));
    }
}
```

再添加 Servlet 优先级测试：同时设置 Servlet 值 `tenant_servlet` 和快照值 `tenant_reactive`，断言处理器返回 `tenant_servlet`。

- [ ] **Step 2: 运行处理器测试并确认 RED**

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=DsHeaderProcessorTest,DsSessionProcessorTest test
```

Expected: 编译失败，错误明确指向缺少 `DsRequestContext` / `DsRequestContextHolder`，不是测试语法或依赖错误。

- [ ] **Step 3: 实现不可变快照和可嵌套持有器**

`DsRequestContext` 必须在构造时深复制 Header 列表、使用 `Locale.ENGLISH` 把 Header 名转为小写，并用不可变映射保存 Session 属性。核心接口：

```java
public final class DsRequestContext {
    private final Map<String, List<String>> headers;
    private final Map<String, Object> sessionAttributes;

    public DsRequestContext(Map<String, ? extends List<String>> headers,
                            Map<String, ?> sessionAttributes) { /* defensive copies */ }

    public String getHeader(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ENGLISH));
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public Object getSessionAttribute(String name) {
        return sessionAttributes.get(name);
    }
}
```

`DsRequestContextHolder` 必须保存旧值并在关闭时恢复：

```java
public final class DsRequestContextHolder {
    private static final ThreadLocal<DsRequestContext> CONTEXT = new ThreadLocal<>();

    public static DsRequestContext getContext() { return CONTEXT.get(); }
    public static Scope open(DsRequestContext context) { return new Scope(context); }
    public static void clear() { CONTEXT.remove(); }

    public static final class Scope implements AutoCloseable {
        private final DsRequestContext previous;
        private boolean closed;
        private Scope(DsRequestContext current) { /* save previous, set/remove current */ }
        @Override public void close() { /* once only; restore previous or remove */ }
    }
}
```

- [ ] **Step 4: 修改两个处理器的回退路径**

保持 Servlet 分支原样优先，仅在当前不是 `ServletRequestAttributes` 时读取快照：

```java
DsRequestContext context = DsRequestContextHolder.getContext();
return context == null ? null : context.getHeader(headerName);
```

Session 分支读取 `context.getSessionAttribute(sessionAttributeName)` 并沿用 `toString()` 转换。

- [ ] **Step 5: 增加并运行持有器隔离测试**

`DsRequestContextHolderTest` 至少覆盖嵌套恢复和重复关闭：

```java
@Test
void nestedScopeRestoresPreviousContext() {
    DsRequestContext outer = context("outer");
    DsRequestContext inner = context("inner");
    try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(outer)) {
        assertSame(outer, DsRequestContextHolder.getContext());
        try (DsRequestContextHolder.Scope nested = DsRequestContextHolder.open(inner)) {
            assertSame(inner, DsRequestContextHolder.getContext());
        }
        assertSame(outer, DsRequestContextHolder.getContext());
    }
    assertNull(DsRequestContextHolder.getContext());
}
```

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=DsRequestContextHolderTest,DsHeaderProcessorTest,DsSessionProcessorTest test
```

Expected: PASS，且原 Servlet 用例保持通过。

- [ ] **Step 6: 提交 Task 1**

```bash
git add \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor
git commit -m "feat: 支持动态数据源 WebFlux 请求快照"
```

---

### Task 2: Reactor Context 桥接和 WebFilter

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DsWebFluxContextSubscriber.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DsWebFluxContextHook.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DsWebFluxRequestContextFilter.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DsWebFluxContextHookTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DsWebFluxRequestContextFilterTest.java`

**Interfaces:**
- Consumes: `DsRequestContext` 和 `DsRequestContextHolder.open(...)` from Task 1。
- Produces: `DsWebFluxContextHook.CONTEXT_KEY`，类型为 `Class<DsRequestContext>`，作为唯一 Reactor Context 键。
- Produces: `DsWebFluxContextHook` 实现 `InitializingBean`、`DisposableBean`；`DsWebFluxRequestContextFilter` 实现 `WebFilter`、`Ordered`。

- [ ] **Step 1: 为 Reactor 信号桥接写失败测试**

`DsWebFluxContextHookTest` 在每个测试中创建并安装 hook，结束后销毁并清理持有器：

```java
@Test
void exposesSnapshotDuringSynchronousSignalAndClearsAfterward() {
    DsRequestContext context = headerContext("tenant_a");

    String result = Mono.just("trigger")
            .map(value -> new DsHeaderProcessor().doDetermineDatasource(null, "#header.ds"))
            .contextWrite(reactorContext -> reactorContext.put(DsWebFluxContextHook.CONTEXT_KEY, context))
            .block();

    assertEquals("tenant_a", result);
    assertNull(DsRequestContextHolder.getContext());
}
```

再用两个不同快照、`publishOn(Schedulers.parallel())` 和 `Mono.zip` 验证并发值不串，并添加回调抛异常后持有器为空的测试。

- [ ] **Step 2: 运行 hook 测试并确认 RED**

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=DsWebFluxContextHookTest test
```

Expected: 编译失败，明确缺少 WebFlux hook/subscriber 类。

- [ ] **Step 3: 实现作用域 subscriber 和幂等 hook**

`DsWebFluxContextSubscriber<T>` 委托 `CoreSubscriber<T>`，在 `onSubscribe`、`onNext`、
`onError`、`onComplete` 每个回调外围执行：

```java
private void withRequestContext(Runnable callback) {
    DsRequestContext context = actual.currentContext()
            .getOrDefault(DsWebFluxContextHook.CONTEXT_KEY, null);
    if (context == null) {
        callback.run();
        return;
    }
    try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(context)) {
        callback.run();
    }
}
```

`DsWebFluxContextHook` 使用固定键和静态引用计数，首次初始化时注册、最后一个实例销毁时注销：

```java
public static final String HOOK_KEY = DsWebFluxContextHook.class.getName();
public static final Class<DsRequestContext> CONTEXT_KEY = DsRequestContext.class;

Hooks.onEachOperator(HOOK_KEY,
        Operators.lift((scannable, subscriber) -> new DsWebFluxContextSubscriber<Object>(subscriber)));
Hooks.resetOnEachOperator(HOOK_KEY);
```

实例必须防止重复 `afterPropertiesSet()` 或 `destroy()` 破坏引用计数。

- [ ] **Step 4: 为 WebFilter 快照写失败测试**

使用 `MockServerHttpRequest`、`MockServerWebExchange` 和 `MockWebSession` 构造真实过滤链：

```java
@Test
void exposesHeaderAndSessionToProcessors() {
    MockWebSession session = new MockWebSession();
    session.getAttributes().put("ds", "tenant_session");
    MockServerWebExchange exchange = MockServerWebExchange.builder(
            MockServerHttpRequest.get("/test").header("X-DS", "tenant_header"))
            .session(session)
            .build();
    AtomicReference<String> header = new AtomicReference<String>();
    AtomicReference<String> sessionValue = new AtomicReference<String>();

    WebFilterChain chain = current -> Mono.just("trigger").map(value -> {
        header.set(new DsHeaderProcessor().doDetermineDatasource(null, "#header.X-DS"));
        sessionValue.set(new DsSessionProcessor().doDetermineDatasource(null, "#session.ds"));
        return value;
    }).then();

    new DsWebFluxRequestContextFilter().filter(exchange, chain).block();
    assertEquals("tenant_header", header.get());
    assertEquals("tenant_session", sessionValue.get());
    assertNull(DsRequestContextHolder.getContext());
}
```

- [ ] **Step 5: 实现 WebFilter 并验证 GREEN**

过滤器先复制 Header，再异步取得 WebSession 属性，把快照写入下游 Reactor Context：

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    Map<String, List<String>> headers = copyHeaders(exchange.getRequest().getHeaders());
    return exchange.getSession().flatMap(session -> {
        DsRequestContext snapshot = new DsRequestContext(headers, session.getAttributes());
        return chain.filter(exchange)
                .contextWrite(context -> context.put(DsWebFluxContextHook.CONTEXT_KEY, snapshot));
    });
}

@Override
public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
}
```

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=DsWebFluxContextHookTest,DsWebFluxRequestContextFilterTest test
```

Expected: PASS，包括异常清理、调度切换、并发隔离、Header 和 Session 快照读取。

- [ ] **Step 6: 提交 Task 2**

```bash
git add \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux
git commit -m "feat: 桥接 WebFlux 动态数据源请求上下文"
```

---

### Task 3: Reactive 条件装配和端到端验证

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DynamicDataSourceWebFluxAutoConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/JdbcAutoConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DynamicDataSourceWebFluxAutoConfigurationTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DynamicDataSourceWebFluxIntegrationTest.java`

**Interfaces:**
- Consumes: `DsWebFluxContextHook`、`DsWebFluxRequestContextFilter` from Task 2。
- Produces: Reactive 应用中的单例 hook Bean 和 filter Bean；Servlet/非 Web 应用中均不创建。

- [ ] **Step 1: 为条件装配和真实 WebFlux 请求写失败测试**

```java
private final ReactiveWebApplicationContextRunner reactiveRunner =
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DynamicDataSourceWebFluxAutoConfiguration.class));

@Test
void reactiveApplicationRegistersBridgeBeans() {
    reactiveRunner.run(context -> {
        assertThat(context).hasSingleBean(DsWebFluxContextHook.class);
        assertThat(context).hasSingleBean(DsWebFluxRequestContextFilter.class);
    });
}

@Test
void servletApplicationDoesNotRegisterBridgeBeans() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DynamicDataSourceWebFluxAutoConfiguration.class))
            .run(context -> {
                assertThat(context).doesNotHaveBean(DsWebFluxContextHook.class);
                assertThat(context).doesNotHaveBean(DsWebFluxRequestContextFilter.class);
            });
}

@Test
void nonWebApplicationDoesNotRegisterBridgeBeans() {
    new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DynamicDataSourceWebFluxAutoConfiguration.class))
            .run(context -> {
                assertThat(context).doesNotHaveBean(DsWebFluxContextHook.class);
                assertThat(context).doesNotHaveBean(DsWebFluxRequestContextFilter.class);
            });
}
```

`DynamicDataSourceWebFluxIntegrationTest` 安装一个 `DsWebFluxContextHook`，并使用真实
`WebTestClient` 驱动同步 Controller：

```java
@BeforeEach
void setUp() {
    hook = new DsWebFluxContextHook();
    hook.afterPropertiesSet();
    WebFilter sessionSeed = (exchange, chain) -> exchange.getSession()
            .doOnNext(session -> session.getAttributes().put("ds", "tenant_b"))
            .then(chain.filter(exchange));
    client = WebTestClient.bindToController(new TestController())
            .webFilter(sessionSeed, new DsWebFluxRequestContextFilter())
            .build();
}

@AfterEach
void tearDown() {
    hook.destroy();
    DsRequestContextHolder.clear();
}

@Test
void resolvesHeaderAndSessionInsideSynchronousController() {
    assertThat(response("/header", "tenant_a")).isEqualTo("tenant_a");
    assertThat(response("/session", "ignored")).isEqualTo("tenant_b");
    assertThat(DsRequestContextHolder.getContext()).isNull();
}

@RestController
static class TestController {
    @GetMapping(value = "/header", produces = MediaType.TEXT_PLAIN_VALUE)
    String header() {
        return new DsHeaderProcessor().doDetermineDatasource(null, "#header.X-DS");
    }

    @GetMapping(value = "/session", produces = MediaType.TEXT_PLAIN_VALUE)
    String session() {
        return new DsSessionProcessor().doDetermineDatasource(null, "#session.ds");
    }
}
```

并发用例用 Java 8 `ExecutorService` 同时发出两个不同 Header 的请求：

```java
@Test
void concurrentRequestsDoNotShareHeaderValues() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
        Future<String> first = executor.submit(() -> response("/header", "tenant_a"));
        Future<String> second = executor.submit(() -> response("/header", "tenant_b"));
        assertThat(first.get()).isEqualTo("tenant_a");
        assertThat(second.get()).isEqualTo("tenant_b");
    } finally {
        executor.shutdownNow();
    }
    assertThat(DsRequestContextHolder.getContext()).isNull();
}

private String response(String path, String datasource) {
    return client.get().uri(path).header("X-DS", datasource)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
}
```

- [ ] **Step 2: 运行自动配置和集成测试并确认 RED**

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=DynamicDataSourceWebFluxAutoConfigurationTest,DynamicDataSourceWebFluxIntegrationTest test
```

Expected: 编译失败，明确缺少自动配置类；如果 Task 2 的类已存在，失败原因不能是
WebTestClient、Session 或测试装配语法错误。

- [ ] **Step 3: 实现并导入 Reactive 自动配置**

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({WebFilter.class, Hooks.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class DynamicDataSourceWebFluxAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DsWebFluxContextHook dsWebFluxContextHook() {
        return new DsWebFluxContextHook();
    }

    @Bean
    @ConditionalOnMissingBean
    public DsWebFluxRequestContextFilter dsWebFluxRequestContextFilter() {
        return new DsWebFluxRequestContextFilter();
    }
}
```

把 `JdbcAutoConfiguration` 的导入更新为：

```java
@Import({DynamicDataSourceAutoConfiguration.class,
        DynamicDataSourceWebFluxAutoConfiguration.class})
```

- [ ] **Step 4: 运行条件装配和端到端测试并确认 GREEN**

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=DynamicDataSourceWebFluxAutoConfigurationTest,DynamicDataSourceWebFluxIntegrationTest test
```

Expected: PASS；Reactive 应用创建两个桥接 Bean，Servlet/非 Web 应用不创建；真实
Header、Session 和并发请求均解析正确。

- [ ] **Step 5: 运行 JDBC 动态数据源回归测试**

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest='com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.**.*Test' test
```

Expected: PASS；Servlet、AOP 和 Reactive 测试全部通过。

- [ ] **Step 6: 运行模块完整测试和静态检查**

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml test
git diff --check
```

Expected: Maven `BUILD SUCCESS`，`git diff --check` 无输出。若模块全量测试受现有无关工作区改动或外部依赖阻塞，记录准确失败并保证本任务定向测试通过，不能把未运行或失败写成通过。

- [ ] **Step 7: 提交 Task 3**

```bash
git add \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/JdbcAutoConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DynamicDataSourceWebFluxAutoConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DynamicDataSourceWebFluxAutoConfigurationTest.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/webflux/DynamicDataSourceWebFluxIntegrationTest.java
git commit -m "test: 验证动态数据源 WebFlux 自动配置"
```

---

## Final Review Checklist

- 每个新增生产类都有先失败后通过的对应测试。
- Header 名大小写不敏感，Session 属性按原有大小写精确匹配。
- Servlet 上下文优先，现有处理器链和自定义 `DsProcessor` 覆盖点不变。
- Reactor hook 有唯一键、引用计数和销毁清理，不影响没有 Context 键的订阅。
- 正常、异常、调度切换和并发路径结束后 ThreadLocal 均无残留。
- Reactive/Servlet 条件装配测试通过。
- 只提交本计划文件和实现文件，用户已有工作区改动保持不变。
