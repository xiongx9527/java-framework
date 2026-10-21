package com.xiongx9527.fw.plus.auth.core;

/**
 * 业务自定义授权实现 SPI：声明一个 Spring Bean 即可同时接入 mcp server 与 websocket 两套认证入口。
 * <p>
 * 业务只需提供一个 {@code @Bean CustomAuthService} 实现，返回自定义 provider key 并实现
 * {@link #authenticate}；框架在自动配置阶段将其注册到 {@link InstanceAuthRegistry}，
 * 业务在 mcp/ws 的 {@code auth.provider} 配置中引用 {@link #getProviderKey()} 即可命中。
 * </p>
 * <p>
 * 与 {@link AuthService} 的关系：本接口扩展 {@link AuthService}，{@link InstanceAuthRegistry#resolve}
 * 解析出的自定义 provider 对外仍表现为普通 {@link AuthService}（mcp 侧经
 * {@code McpAuthService.adapt} 适配，ws 侧经 {@code WsAuthSupport} 调用）。
 * </p>
 */
public interface CustomAuthService extends AuthService {

    /**
     * 自定义 provider key：业务在 mcp/ws 的 {@code auth.provider} 配置中引用此值。
     * <p>
     * 不得与内置 provider（{@code permit-all}、{@code sa-token-*}）重名；
     * 冲突时内置优先，自定义注册会被跳过并告警。
     * </p>
     *
     * @return 非空且 trim 后非空的 provider key
     */
    String getProviderKey();
}
