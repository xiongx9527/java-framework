package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.MagicAPIManagementService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptions;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.exception.InvalidArgumentException;
import org.ssssssss.magicapi.core.exception.MagicLoginException;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.Callable;

@RestController
@RequestMapping("${magic-api.web:/magic-api/web}")
public class MagicAPIWebFluxController {

    private final MagicAPIManagementService managementService;
    private final MagicConfiguration configuration;

    public MagicAPIWebFluxController(MagicAPIManagementService managementService,
                                     MagicConfiguration configuration) {
        this.managementService = managementService;
        this.configuration = configuration;
    }

    @GetMapping("/optionList")
    public Mono<JsonBean<List<List<String>>>> options() {
        return invoke(managementService::options);
    }

    @PostMapping("/login")
    public Mono<JsonBean<Boolean>> login(@RequestParam(value = "username", required = false) String username,
                                         @RequestParam(value = "password", required = false) String password,
                                         ServerWebExchange exchange) {
        return invoke(() -> {
            if (configuration.getAuthorizationInterceptor().requireLogin()) {
                if (isBlank(username) && isBlank(password)) {
                    try {
                        configuration.getAuthorizationInterceptor().getUserByToken(token(exchange));
                    } catch (MagicLoginException ignored) {
                        return new JsonBean<>(false);
                    }
                } else {
                    MagicUser user = configuration.getAuthorizationInterceptor().login(username, password);
                    exchange.getResponse().getHeaders().set(Constants.MAGIC_TOKEN_HEADER, user.getToken());
                    exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                            Constants.MAGIC_TOKEN_HEADER);
                }
            }
            return new JsonBean<>(true);
        });
    }

    @PostMapping("/user")
    public Mono<JsonBean<MagicUser>> user(ServerWebExchange exchange) {
        return invoke(() -> {
            if (configuration.getAuthorizationInterceptor().requireLogin()) {
                try {
                    return new JsonBean<>(configuration.getAuthorizationInterceptor().getUserByToken(token(exchange)));
                } catch (MagicLoginException ignored) {
                    return new JsonBean<>(MagicUser.guest());
                }
            }
            return new JsonBean<>(MagicUser.guest());
        });
    }

    @PostMapping("/logout")
    public Mono<JsonBean<Void>> logout(ServerWebExchange exchange) {
        return invoke(() -> {
            configuration.getAuthorizationInterceptor().logout(token(exchange));
            return new JsonBean<Void>();
        });
    }

    @GetMapping("/datasource/options")
    public Mono<JsonBean<MagicJdbcClassOptions>> datasourceOptions(ServerWebExchange exchange) {
        return invokeProtected(exchange, () -> managementService.datasourceOptions(
                new WebFluxMagicAPIRequestAccess(exchange)));
    }

    @GetMapping("/{resource}/reloadResource")
    public Mono<JsonBean<Boolean>> reloadResource(@PathVariable("resource") String resource, ServerWebExchange exchange) {
        return invokeProtected(exchange, () -> managementService.reloadResource(resource,
                new WebFluxMagicAPIRequestAccess(exchange)));
    }

    @GetMapping("/{type}/reload/{id}")
    public Mono<JsonBean<Boolean>> reloadSingleResource(@PathVariable("type") String type,
                                                         @PathVariable("id") String id,
                                                         ServerWebExchange exchange) {
        return invokeProtected(exchange, () -> managementService.reloadSingleResource(type, id,
                new WebFluxMagicAPIRequestAccess(exchange)));
    }

    @PostMapping("/datasource/myjdbc/test")
    public Mono<JsonBean<String>> test(@RequestBody Mono<DataSourceInfo> info, ServerWebExchange exchange) {
        return info.flatMap(dataSourceInfo -> invokeProtected(exchange,
                () -> managementService.test(dataSourceInfo)));
    }

    private <T> Mono<T> invoke(Callable<T> invocation) {
        return Mono.fromCallable(invocation).subscribeOn(Schedulers.boundedElastic());
    }

    private <T> Mono<T> invokeProtected(ServerWebExchange exchange, Callable<T> invocation) {
        return invoke(() -> {
            authenticate(exchange);
            return invocation.call();
        });
    }

    private void authenticate(ServerWebExchange exchange) throws MagicLoginException {
        if (configuration.getAuthorizationInterceptor().requireLogin()) {
            exchange.getAttributes().put(Constants.ATTRIBUTE_MAGIC_USER,
                    configuration.getAuthorizationInterceptor().getUserByToken(token(exchange)));
        }
    }

    private String token(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst(Constants.MAGIC_TOKEN_HEADER);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @ExceptionHandler(MagicLoginException.class)
    public JsonBean<Void> invalidLogin(MagicLoginException exception) {
        return new JsonBean<>(401, exception.getMessage());
    }

    @ExceptionHandler(InvalidArgumentException.class)
    public JsonBean<Void> invalidArgument(InvalidArgumentException exception) {
        return new JsonBean<>(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public JsonBean<Void> exception(Exception exception) {
        return new JsonBean<>(-1, exception.getMessage());
    }
}
