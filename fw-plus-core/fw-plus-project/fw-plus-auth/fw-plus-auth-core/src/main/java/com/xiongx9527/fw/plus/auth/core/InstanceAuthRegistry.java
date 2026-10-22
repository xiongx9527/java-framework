package com.xiongx9527.fw.plus.auth.core;

import com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties;
import com.xiongx9527.fw.plus.auth.core.util.ClassUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按实例认证配置解析认证服务：内置 SaToken/permit-all provider，以及业务经
 * {@link #registerProvider} 注册的自定义 provider。
 * <p>
 * provider 清单固定：{@code permit-all}、{@code sa-token-login}、
 * {@code sa-token-oauth2}、{@code sa-token-jwt}、{@code sa-token-apikey}、{@code sa-token-sso}。
 * Sa-Token 各模块均为 optional 依赖：模块缺失时对应 provider 不出现在 {@link #availableProviders()}
 * （不可配置、控制台不可选）；实例一旦显式配置该 provider，解析结果为 fail-closed 拒绝服务
 * （恒 {@link AuthPrincipal#ANONYMOUS}，请求 401/握手拒绝），不回退、不使启动失败。
 * 未知 provider（含历史 {@code custom}）按配置错误抛出 {@link IllegalArgumentException}。
 * </p>
 * <p>
 * Sa-Token provider 实现类只在各自 holder 内被静态引用：registry 仅依赖 holder 类
 * （始终在 classpath），holder 在通过 classpath 检查后才首次加载，避免 registry 链接期
 * 触发缺失模块的 {@link NoClassDefFoundError}。
 * </p>
 */
@Slf4j
public class InstanceAuthRegistry {

    public static final String PROVIDER_PERMIT_ALL = "permit-all";

    public static final String PROVIDER_SA_TOKEN_LOGIN = "sa-token-login";

    public static final String PROVIDER_SA_TOKEN_OAUTH2 = "sa-token-oauth2";

    public static final String PROVIDER_SA_TOKEN_JWT = "sa-token-jwt";

    public static final String PROVIDER_SA_TOKEN_APIKEY = "sa-token-apikey";

    public static final String PROVIDER_SA_TOKEN_SSO = "sa-token-sso";

    private static final String CLASS_SA_TOKEN_LOGIN = "cn.dev33.satoken.stp.StpUtil";

    private static final String CLASS_SA_TOKEN_OAUTH2 = "cn.dev33.satoken.oauth2.template.SaOAuth2Util";

    private static final String CLASS_SA_TOKEN_JWT = "cn.dev33.satoken.jwt.SaJwtUtil";

    private static final String CLASS_SA_TOKEN_APIKEY = "cn.dev33.satoken.apikey.template.SaApiKeyUtil";

    private static final String CLASS_SA_TOKEN_SSO = "cn.dev33.satoken.sso.template.SaSsoClientUtil";

    private final ClassLoader classLoader;

    /**
     * 业务自定义 provider 映射（key -> AuthService）。
     * <p>
     * 仅启动装配期写入、运行期只读（Spring Bean 创建时经 {@link #registerProvider} 装入）。
     * 与内置 provider key 冲突时内置优先，重复 key 首次注册为准。
     * </p>
     */
    private final Map<String, AuthService> customProviders = new LinkedHashMap<>();

    private static final Set<String> BUILT_IN_PROVIDERS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(PROVIDER_PERMIT_ALL, PROVIDER_SA_TOKEN_LOGIN,
                    PROVIDER_SA_TOKEN_OAUTH2, PROVIDER_SA_TOKEN_JWT, PROVIDER_SA_TOKEN_APIKEY,
                    PROVIDER_SA_TOKEN_SSO)));

    public InstanceAuthRegistry() {
        this(InstanceAuthRegistry.class.getClassLoader());
    }

    /**
     * @param classLoader classpath 可用性检查与 provider 加载使用的类加载器（测试可传入屏蔽类加载器）
     */
    public InstanceAuthRegistry(ClassLoader classLoader) {
        this.classLoader = classLoader != null ? classLoader : InstanceAuthRegistry.class.getClassLoader();
    }

    /**
     * 当前 classpath 可用的 provider 清单；各能力（MCP/WS/Knowledge）校验与控制台选项共用同一份。
     *
     * @param classLoader classpath 检查使用的类加载器
     * @return 不可变有序清单
     */
    public static List<String> availableProviders(ClassLoader classLoader) {
        ClassLoader loader = classLoader != null ? classLoader : InstanceAuthRegistry.class.getClassLoader();
        List<String> providers = new ArrayList<>();
        providers.add(PROVIDER_PERMIT_ALL);
        if (ClassUtils.isPresent(CLASS_SA_TOKEN_LOGIN, loader)) {
            providers.add(PROVIDER_SA_TOKEN_LOGIN);
        }
        if (ClassUtils.isPresent(CLASS_SA_TOKEN_OAUTH2, loader)) {
            providers.add(PROVIDER_SA_TOKEN_OAUTH2);
        }
        if (ClassUtils.isPresent(CLASS_SA_TOKEN_JWT, loader)) {
            providers.add(PROVIDER_SA_TOKEN_JWT);
        }
        if (ClassUtils.isPresent(CLASS_SA_TOKEN_APIKEY, loader)) {
            providers.add(PROVIDER_SA_TOKEN_APIKEY);
        }
        if (ClassUtils.isPresent(CLASS_SA_TOKEN_SSO, loader)) {
            providers.add(PROVIDER_SA_TOKEN_SSO);
        }
        return providers;
    }

    /**
     * 当前实例 classpath 可用的 provider 清单。
     */
    public List<String> availableProviders() {
        return availableProviders(classLoader, customProviders.keySet());
    }

    /**
     * 静态可用 provider 清单 + 调用方提供的额外 key（如业务自定义 provider key）。
     *
     * @param classLoader    classpath 检查使用的类加载器
     * @param extraProviders 额外追加的 provider key（可为空）
     * @return 不可变有序清单
     */
    public static List<String> availableProviders(ClassLoader classLoader, Collection<String> extraProviders) {
        List<String> providers = availableProviders(classLoader);
        if (extraProviders != null) {
            for (String key : extraProviders) {
                if (key != null && !key.trim().isEmpty() && !providers.contains(key)) {
                    providers.add(key);
                }
            }
        }
        return providers;
    }

    /**
     * 注册业务自定义 provider：把业务的 {@link AuthService} 绑定到一个自定义 provider key。
     * <p>
     * 由自动配置在创建 {@link InstanceAuthRegistry} Bean 时遍历
     * {@code ObjectProvider<CustomAuthService>} 调用，运行期不应再注册。
     * </p>
     * <p>
     * 忽略空 key/空实现；与内置 provider key 冲突时跳过（内置优先）并告警；
     * 重复 key 首次注册为准并告警。
     * </p>
     *
     * @param providerKey 自定义 provider key
     * @param authService 业务认证实现
     */
    public void registerProvider(String providerKey, AuthService authService) {
        if (providerKey == null || providerKey.trim().isEmpty() || authService == null) {
            return;
        }
        String key = providerKey.trim();
        if (BUILT_IN_PROVIDERS.contains(key)) {
            log.warn("Skip registering custom auth provider '{}': collides with a built-in provider"
                    + " (built-in wins)", key);
            return;
        }
        if (customProviders.putIfAbsent(key, authService) != null) {
            log.warn("Skip registering custom auth provider '{}': already registered (first wins)", key);
        }
    }

    /**
     * 按实例认证配置解析认证服务，不回退到其它实例或隐式共享服务。
     *
     * @param instanceKey 实例标识，仅用于校验与告警信息
     * @param auth 实例认证配置
     * @return 内置服务；未配置 provider 时返回 {@code null}（未认证，请求 401/握手拒绝）
     * @throws IllegalArgumentException provider 不受支持（含历史 {@code custom}）时
     */
    public AuthService resolve(String instanceKey, InstanceAuthProperties auth) {
        if (auth == null || isBlank(auth.getProvider())) {
            return null;
        }
        String provider = auth.getProvider().trim();
        if (PROVIDER_PERMIT_ALL.equals(provider)) {
            return new PermitAllAuthService();
        }
        if (PROVIDER_SA_TOKEN_LOGIN.equals(provider)) {
            return available(CLASS_SA_TOKEN_LOGIN)
                    ? LoginHolder.create()
                    : reject(instanceKey, provider, CLASS_SA_TOKEN_LOGIN);
        }
        if (PROVIDER_SA_TOKEN_OAUTH2.equals(provider)) {
            return available(CLASS_SA_TOKEN_OAUTH2)
                    ? OAuth2Holder.create()
                    : reject(instanceKey, provider, CLASS_SA_TOKEN_OAUTH2);
        }
        if (PROVIDER_SA_TOKEN_JWT.equals(provider)) {
            return available(CLASS_SA_TOKEN_JWT)
                    ? JwtHolder.create()
                    : reject(instanceKey, provider, CLASS_SA_TOKEN_JWT);
        }
        if (PROVIDER_SA_TOKEN_APIKEY.equals(provider)) {
            return available(CLASS_SA_TOKEN_APIKEY)
                    ? ApiKeyHolder.create()
                    : reject(instanceKey, provider, CLASS_SA_TOKEN_APIKEY);
        }
        if (PROVIDER_SA_TOKEN_SSO.equals(provider)) {
            return available(CLASS_SA_TOKEN_SSO)
                    ? SsoHolder.create()
                    : reject(instanceKey, provider, CLASS_SA_TOKEN_SSO);
        }
        AuthService custom = customProviders.get(provider);
        if (custom != null) {
            return custom;
        }
        throw new IllegalArgumentException("Instance '" + instanceKey
                + "' selects unsupported authentication provider '" + provider + "'");
    }

    private boolean available(String className) {
        return ClassUtils.isPresent(className, classLoader);
    }

    private AuthService reject(String instanceKey, String provider, String requiredClass) {
        log.warn("Instance '{}' selects provider '{}' but {} is missing from classpath;"
                + " requests of this instance will be rejected (fail-closed)", instanceKey, provider, requiredClass);
        return RejectingAuthService.INSTANCE;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 各 provider 的实例化隔离在独立 holder：每个 holder 只引用自身对应模块的实现类，
     * 仅在 classpath 检查通过后才首次加载，registry 本身不静态引用任何 Sa-Token 模块类型。
     * <p>
     * 实际实现类在 {@code fw-plus-auth-satoken} 模块中，这里通过反射实例化。
     * </p>
     */
    private static final class LoginHolder {
        static AuthService create() {
            return instantiate("com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenLoginAuthService");
        }
    }

    private static final class OAuth2Holder {
        static AuthService create() {
            return instantiate("com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenOAuth2AuthService");
        }
    }

    private static final class JwtHolder {
        static AuthService create() {
            return instantiate("com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenJwtAuthService");
        }
    }

    private static final class ApiKeyHolder {
        static AuthService create() {
            return instantiate("com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenApiKeyAuthService");
        }
    }

    private static final class SsoHolder {
        static AuthService create() {
            return instantiate("com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenSsoAuthService");
        }
    }

    private static AuthService instantiate(String className) {
        try {
            return (AuthService) Class.forName(className, true, InstanceAuthRegistry.class.getClassLoader())
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate provider: " + className, e);
        }
    }
}