package com.xiongx9527.fw.plus.boot.autoconfigure.web.i18n;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.WebAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.properties.WebProperties;
import com.xiongx9527.fw.plus.common.i18n.ComposableMessageSource;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 国际化（i18n）自动配置。
 *
 * <h3>能力</h3>
 * <ul>
 *   <li>注册 {@link MessageSource}，扫描类路径下所有模块的 {@code i18n/messages*.properties}，
 *       合并为一个资源（模块分散放置、集中加载，见 {@link ComposableMessageSource}）。</li>
 *   <li>注册 {@link LocaleResolver}：以请求头 {@code Accept-Language} 协商语言，
 *       缺省回退中文；同时支持 URL 参数 {@code ?lang=} 手动切换（便于联调与手工验证）。</li>
 *   <li>把 MessageSource 注入 {@link I18nMessageResolver} 静态入口，供异常类在任意位置取文案。</li>
 * </ul>
 *
 * <h3>配置文件约定</h3>
 * <pre>
 *   fw.plus.web.i18n.enabled        是否启用（默认 true）
 *   fw.plus.web.i18n.locations      资源扫描目录，默认 classpath*:i18n/
 *   fw.plus.web.i18n.default-locale 缺省语言，默认 zh_CN
 *   fw.plus.web.i18n.param-name     手动切换语言的 URL 参数名，默认 lang
 *   fw.plus.web.i18n.cache-seconds  资源缓存秒数，默认 3600
 * </pre>
 *
 * @author xiongxiao
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "fw.plus.web.i18n", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.web.servlet.LocaleResolver")
@AutoConfigureAfter(WebAutoConfiguration.class)
public class I18nAutoConfiguration {

    private final WebProperties webProperties;

    public I18nAutoConfiguration(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    /**
     * 加载各模块分散放置的国际化资源。
     */
    @Bean
    @ConditionalOnMissingBean(name = "messageSource")
    public MessageSource messageSource() {
        WebProperties.I18n i18n = webProperties.getI18n();
        return ComposableMessageSource.create(
                StandardCharsets.UTF_8.name(),
                i18n.resolveDefaultLocale(),
                i18n.getCacheSeconds(),
                i18n.resolveLocations());
    }

    /**
     * 语言解析器：优先 {@code Accept-Language} 请求头，缺省中文。
     */
    @Bean
    @ConditionalOnMissingBean(LocaleResolver.class)
    public LocaleResolver localeResolver(MessageSource messageSource) {
        WebProperties.I18n i18n = webProperties.getI18n();
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(i18n.resolveDefaultLocale());
        List<Locale> supported = new ArrayList<>();
        supported.add(Locale.SIMPLIFIED_CHINESE);
        supported.add(Locale.US);
        supported.add(Locale.ENGLISH);
        resolver.setSupportedLocales(supported);
        return resolver;
    }

    /**
     * 语言切换拦截器：支持 {@code ?lang=en} 之类的显式切换（会话级）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "localeChangeInterceptor")
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName(webProperties.getI18n().getParamName());
        return interceptor;
    }

    /**
     * 注册拦截器。
     */
    @Bean
    public WebMvcConfigurer i18nWebMvcConfigurer(LocaleChangeInterceptor localeChangeInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(localeChangeInterceptor);
            }
        };
    }

    /**
     * 把容器中的 MessageSource 注入静态解析器，供异常类使用。
     */
    @PostConstruct
    public void initResolver() {
        I18nMessageResolver.setMessageSource(SpringUtils.getBean(MessageSource.class));
    }
}
