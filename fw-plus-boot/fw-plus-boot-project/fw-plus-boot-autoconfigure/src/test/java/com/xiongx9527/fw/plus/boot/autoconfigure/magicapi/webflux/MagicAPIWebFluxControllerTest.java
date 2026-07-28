package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.MagicAPIManagementService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux.MagicAPIWebFluxController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux.WebFluxMagicAPIRequestAccess;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptionProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptions;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.starter.MagicAPIDebeziumConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.starter.MagicAPIDruidConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.web.reactive.context.StandardReactiveWebEnvironment;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.exception.InvalidArgumentException;
import org.ssssssss.magicapi.core.exception.MagicLoginException;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.RequestHandler;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicAPIWebFluxControllerTest {

    @Test
    void startsRealReactiveAutoConfigurationWithoutManuallyProvidingManagementBeans() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MagicAPIWebFluxAutoConfiguration.class))
                .withBean(MagicConfiguration.class, MagicConfiguration::new)
                .run(context -> assertThat(context)
                        .hasSingleBean(MagicConfiguration.class)
                        .hasSingleBean(MagicJdbcClassOptionProvider.class)
                        .hasSingleBean(MagicAPIManagementService.class)
                        .hasSingleBean(MagicAPIWebFluxController.class));
    }

    @Test
    void startsReactiveAutoConfigurationFromFullChainWithoutManualMagicConfiguration() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.class,
                        MagicAPIWebFluxAutoConfiguration.class))
                .withPropertyValues("magic-api.resource.location=target/magic-api-webflux-test")
                .run(context -> assertThat(context)
                        .hasSingleBean(MagicConfiguration.class)
                        .hasSingleBean(MagicResourceService.class)
                        .hasSingleBean(MagicAPIWebFluxController.class)
                        .hasSingleBean(RequestHandler.class)
                        .hasBean("webFluxRequestMagicDynamicRegistry"));
    }

    @Test
    void registersUpstreamWorkbenchAndResourceRoutesInReactiveHandlerMapping() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.class,
                        MagicAPIWebFluxAutoConfiguration.class))
                .withPropertyValues("magic-api.resource.location=target/magic-api-webflux-routes-test")
                .run(context -> {
                    org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping mapping =
                            context.getBean("requestMappingHandlerMapping",
                                    org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping.class);
                    assertThat(mapping.getHandlerMethods().keySet())
                            .anyMatch(info -> hasPath(info, "/magic-api/web/plugins"))
                            .anyMatch(info -> hasPath(info, "/magic-api/web/resource"))
                            .anyMatch(info -> hasPath(info, "/magic-api/web/resource/file/{folder}/save"));
                });
    }

    @Test
    void registersMagicExceptionHandlerAdviceInReactiveContext() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.class,
                        MagicAPIWebFluxAutoConfiguration.class))
                .withPropertyValues("magic-api.resource.location=target/magic-api-webflux-exception-advice-test")
                .run(context -> assertThat(context)
                        .hasSingleBean(MagicAPIWebFluxExceptionHandler.class));
    }

    @Test
    void mapsMagicControllerExceptionsWithWebFluxExceptionAdvice() {
        WebTestClient client = WebTestClient.bindToController(new ThrowingMagicController())
                .controllerAdvice(new MagicAPIWebFluxExceptionHandler())
                .build();

        client.get().uri("/magic-test/invalid")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(-10)
                .jsonPath("$.message").isEqualTo("无权限操作.");

        client.get().uri("/magic-test/error")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(-1)
                .jsonPath("$.message").isEqualTo("boom");
    }

    @Test
    void registersPluginControllerRoutesInReactiveHandlerMapping() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.class,
                        MagicAPIWebFluxAutoConfiguration.class,
                        MagicAPIDebeziumConfiguration.class))
                .withPropertyValues("magic-api.resource.location=target/magic-api-webflux-plugin-routes-test")
                .run(context -> {
                    org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping mapping =
                            context.getBean("requestMappingHandlerMapping",
                                    org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping.class);
                    assertThat(mapping.getHandlerMethods().keySet())
                            .anyMatch(info -> hasPath(info, "/magic-api/web/debezium/datasource/connector-options"));
                });
    }

    @Test
    void registersMcpControllerRoutesInReactiveHandlerMapping() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.class,
                        MagicAPIWebFluxAutoConfiguration.class,
                        MagicAPIMcpConfiguration.class))
                .withPropertyValues("magic-api.resource.location=target/magic-api-webflux-mcp-routes-test")
                .run(context -> {
                    org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping mapping =
                            context.getBean("requestMappingHandlerMapping",
                                    org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping.class);
                    assertThat(mapping.getHandlerMethods().keySet())
                            .anyMatch(info -> hasPath(info, "/mcp"))
                            .anyMatch(info -> hasPath(info, "/mcp/diagnostics/execute"));
                });
    }

    @Test
    void keepsDruidServletPluginOutOfReactiveWebContext() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MagicAPIDruidConfiguration.class))
                .run(context -> assertThat(context)
                        .doesNotHaveBean(ServletRegistrationBean.class)
                        .doesNotHaveBean(FilterRegistrationBean.class));
    }

    @Test
    void filtersUpstreamMvcAutoConfigurationOnlyInReactiveApplications() {
        List<AutoConfigurationImportFilter> filters = SpringFactoriesLoader.loadFactories(
                AutoConfigurationImportFilter.class, getClass().getClassLoader());
        AutoConfigurationImportFilter filter = filters.stream()
                .filter(candidate -> candidate.getClass().getName().equals(
                        "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.MagicAPIWebFluxImportFilter"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("MagicAPI WebFlux import filter is not registered"));
        String[] candidates = new String[]{
                "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration"
        };

        ((EnvironmentAware) filter).setEnvironment(new StandardReactiveWebEnvironment());
        assertThat(filter.match(candidates, null)).containsExactly(false);

        ((EnvironmentAware) filter).setEnvironment(new StandardEnvironment());
        assertThat(filter.match(candidates, null)).containsExactly(true);
    }

    @Test
    void doesNotRegisterWebFluxControllerInServletContext() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MagicAPIWebFluxAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(MagicAPIWebFluxController.class));
    }

    @Test
    void doesNotRegisterControllerWhenMagicApiIsDisabled() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MagicAPIWebFluxAutoConfiguration.class))
                .withPropertyValues("fw.plus.magicapi.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MagicAPIWebFluxController.class));
    }

    @Test
    void exposesConcreteOptionListPayload() {
        MagicConfiguration configuration = configuration(noLoginAuthorization());
        WebTestClient client = client(managementService(configuration, optionProvider(
                new MagicJdbcClassOptions(Collections.<String>emptyList(), Collections.<String>emptyList()))), configuration);

        client.get().uri("/magic-api/web/optionList")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(1)
                .jsonPath("$.data[0][0]").isEqualTo("wrap_request_parameter")
                .jsonPath("$.data[0][1]").isEqualTo("包装请求参数到一个变量中");
    }

    @Test
    void exposesConcreteDatasourceOptionPayloadWithValidToken() {
        MagicJdbcClassOptions options = new MagicJdbcClassOptions(
                Arrays.asList("example.Driver"), Arrays.asList("example.DataSource"));
        MagicConfiguration configuration = configuration(loginAuthorization());
        WebTestClient client = client(managementService(configuration, optionProvider(options)), configuration);

        client.get().uri("/magic-api/web/datasource/options")
                .header(Constants.MAGIC_TOKEN_HEADER, "valid-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.jdbcDrivers[0]").isEqualTo("example.Driver")
                .jsonPath("$.data.datasourceTypes[0]").isEqualTo("example.DataSource");
    }

    @Test
    void rejectsEveryProtectedEndpointWithoutLoginToken() {
        MagicConfiguration configuration = configuration(loginAuthorization());
        configuration.setMagicResourceService(mock(MagicResourceService.class));
        WebTestClient client = client(new MagicAPIManagementService(configuration,
                optionProvider(new MagicJdbcClassOptions(Collections.<String>emptyList(), Collections.<String>emptyList()))),
                configuration);

        expectLoginRejected(client.get().uri("/magic-api/web/datasource/options"));
        expectLoginRejected(client.get().uri("/magic-api/web/api/reloadResource"));
        expectLoginRejected(client.get().uri("/magic-api/web/api/reload/resource-id"));
        expectLoginRejected(client.post().uri("/magic-api/web/datasource/myjdbc/test")
                .bodyValue("{}")
                .header("Content-Type", "application/json"));
    }

    @Test
    void exposesLoginUserAndLogoutManagementContract() {
        MagicConfiguration configuration = configuration(loginAuthorization());
        WebTestClient client = client(managementService(configuration, optionProvider(
                new MagicJdbcClassOptions(Collections.<String>emptyList(), Collections.<String>emptyList()))), configuration);

        String token = client.post().uri("/magic-api/web/login?username=tester&password=secret")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(Constants.MAGIC_TOKEN_HEADER, "valid-token")
                .expectBody()
                .jsonPath("$.data").isEqualTo(true)
                .returnResult()
                .getResponseHeaders()
                .getFirst(Constants.MAGIC_TOKEN_HEADER);

        client.post().uri("/magic-api/web/user")
                .header(Constants.MAGIC_TOKEN_HEADER, token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.username").isEqualTo("tester");

        client.post().uri("/magic-api/web/logout")
                .header(Constants.MAGIC_TOKEN_HEADER, token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(1);
    }

    @Test
    void mapsPermissionFailuresToMagicApiJsonBean() {
        MagicConfiguration configuration = configuration(denyAuthorization());
        WebTestClient client = client(managementService(configuration, optionProvider(
                new MagicJdbcClassOptions(Collections.<String>emptyList(), Collections.<String>emptyList()))), configuration);

        client.get().uri("/magic-api/web/datasource/options")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(-10)
                .jsonPath("$.message").isEqualTo("无权限操作.");
    }

    @Test
    void reloadUsesGenericMagicResourceServiceWithoutClassCast() {
        MagicResourceService resourceService = mock(MagicResourceService.class);
        MagicConfiguration configuration = configuration(noLoginAuthorization());
        configuration.setMagicResourceService(resourceService);
        MagicAPIManagementService service = new MagicAPIManagementService(configuration,
                optionProvider(new MagicJdbcClassOptions(Collections.<String>emptyList(), Collections.<String>emptyList())));

		assertThat(service.reloadResource("api", access("/magic-api/web/api/reloadResource")).getData()).isEqualTo(true);
        verify(resourceService).refresh();
    }

    @Test
    void singleReloadUsesGenericMagicResourceServiceWithoutClassCast() {
        MagicResourceService resourceService = mock(MagicResourceService.class);
        MagicEntity entity = new MagicEntity();
        entity.setId("resource-id");
        entity.setGroupId("group-id");
        Group group = new Group();
        group.setId("group-id");
        group.setType("api");
        when(resourceService.file("resource-id")).thenReturn(entity);
        when(resourceService.getGroup("group-id")).thenReturn(group);
        MagicConfiguration configuration = configuration(noLoginAuthorization());
        configuration.setMagicResourceService(resourceService);
        MagicAPIManagementService service = new MagicAPIManagementService(configuration,
                optionProvider(new MagicJdbcClassOptions(Collections.<String>emptyList(), Collections.<String>emptyList())));

		assertThat(service.reloadSingleResource("api", "resource-id",
				access("/magic-api/web/api/reload/resource-id")).getData()).isEqualTo(true);
        verify(resourceService).refresh();
    }

    @Test
    void schedulesManagementCallsOnBoundedElastic() {
        MagicAPIManagementService service = mock(MagicAPIManagementService.class);
        AtomicReference<String> threadName = new AtomicReference<String>();
        when(service.options()).thenAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            return new JsonBean<Object>(Collections.emptyList());
        });

        client(service, configuration(noLoginAuthorization())).get().uri("/magic-api/web/optionList")
                .exchange()
                .expectStatus().isOk();

        assertThat(threadName.get()).startsWith("boundedElastic-");
    }

    @Test
    void mapsWebFluxRequestDataAndUsesNonNullSafeFallbacks() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/magic-api/web/datasource/options?key=one&key=two")
                .header("X-Console-Token", "first", "second")
                .cookie(new HttpCookie("session", "cookie-value"))
                .remoteAddress(InetSocketAddress.createUnresolved("console.example", 8080))
                .build());
        WebFluxMagicAPIRequestAccess access = new WebFluxMagicAPIRequestAccess(exchange);

        assertThat(Collections.list(access.getHeaders("X-Console-Token"))).containsExactly("first", "second");
        assertThat(access.getParameterValues("key")).containsExactly("one", "two");
        assertThat(access.getRequestURI()).isEqualTo("/magic-api/web/datasource/options");
        assertThat(access.getMethod()).isEqualTo("GET");
        assertThat(access.getRemoteAddr()).isEqualTo("console.example");
        assertThat(access.getCookies()).hasSize(1);
        assertThat(access.getCookies()[0].getName()).isEqualTo("session");
        assertThat(access.getCookies()[0].getValue()).isEqualTo("cookie-value");
        assertThat(access.getSession()).isNotNull();
        assertThat(access.getInputStream().read()).isEqualTo(-1);
        assertThat(access.getHttpInputMessage()).isNotNull();
        assertThat(access.getHttpInputMessage().getBody().read()).isEqualTo(-1);
        assertThatThrownBy(access::resolveMultipart).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(access::getUserPrincipal).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(access::getRequest).isInstanceOf(UnsupportedOperationException.class);
    }

    private WebTestClient client(MagicAPIManagementService service, MagicConfiguration configuration) {
        return WebTestClient.bindToController(new MagicAPIWebFluxController(service, configuration)).build();
    }

    private MagicAPIManagementService managementService(MagicConfiguration configuration,
                                                         MagicJdbcClassOptionProvider optionProvider) {
        return new MagicAPIManagementService(configuration, optionProvider);
    }

    private MagicConfiguration configuration(AuthorizationInterceptor authorizationInterceptor) {
        MagicConfiguration configuration = new MagicConfiguration();
        configuration.setAuthorizationInterceptor(authorizationInterceptor);
        return configuration;
    }

    private MagicJdbcClassOptionProvider optionProvider(MagicJdbcClassOptions options) {
        MagicJdbcClassOptionProvider provider = mock(MagicJdbcClassOptionProvider.class);
        when(provider.getOptions()).thenReturn(options);
        return provider;
    }

    private AuthorizationInterceptor noLoginAuthorization() {
        return new AuthorizationInterceptor() {
        };
    }

    private AuthorizationInterceptor loginAuthorization() {
        return new AuthorizationInterceptor() {
            @Override
            public boolean requireLogin() {
                return true;
            }

            @Override
            public MagicUser getUserByToken(String token) throws MagicLoginException {
                if ("valid-token".equals(token)) {
                    return new MagicUser("1", "tester", token);
                }
                throw new MagicLoginException("token无效");
            }

            @Override
            public MagicUser login(String username, String password) throws MagicLoginException {
                if ("tester".equals(username) && "secret".equals(password)) {
                    return new MagicUser("1", "tester", "valid-token");
                }
                throw new MagicLoginException("用户名或密码不正确");
            }
        };
    }

    private AuthorizationInterceptor denyAuthorization() {
        return new AuthorizationInterceptor() {
            @Override
            public boolean allowVisit(MagicUser magicUser,
                                      org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest request,
                                      Authorization authorization) {
                return false;
            }
        };
    }

    private WebFluxMagicAPIRequestAccess access(String uri) {
        return new WebFluxMagicAPIRequestAccess(MockServerWebExchange.from(MockServerHttpRequest.get(uri).build()));
    }

    private void expectLoginRejected(WebTestClient.RequestHeadersSpec<?> request) {
        request.exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("token无效");
    }

    private boolean hasPath(org.springframework.web.reactive.result.method.RequestMappingInfo info,
                            String path) {
        return info.getPatternsCondition().getDirectPaths().contains(path)
                || info.getPatternsCondition().getPatterns().stream()
                        .anyMatch(pattern -> path.equals(pattern.getPatternString()));
    }

    @RequestMapping("/magic-test")
    static class ThrowingMagicController extends MagicController {

        ThrowingMagicController() {
            super(new MagicConfiguration());
        }

        @GetMapping("/invalid")
        public JsonBean<Void> invalid() {
            throw new InvalidArgumentException(JsonCodeConstants.PERMISSION_INVALID);
        }

        @GetMapping("/error")
        public JsonBean<Void> error() {
            throw new IllegalStateException("boom");
        }
    }
}
