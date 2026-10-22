package com.xiongx9527.fw.plus.boot.autoconfigure.web.i18n;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;

/**
 * 把容器中的 {@link MessageSource} 注入静态解析器 {@link I18nMessageResolver}，供异常类在任意位置取文案。
 *
 * <p>独立成一个配置类并在 {@link I18nAutoConfiguration} 之后装配，避免在同一个声明了
 * {@code messageSource} {@code @Bean} 的配置类内用 {@code @Autowired} 索取 {@code messageSource}
 * 而触发的循环依赖（messageSource 正在创建时又被 initResolver 索取）。</p>
 *
 * <p>用容器注入而非 {@code SpringUtils.getBean}：自动配置初始化时静态 context 可能尚未就绪，直接取会 NPE。</p>
 */
@Configuration
@AutoConfigureAfter(I18nAutoConfiguration.class)
public class I18nResolverConfiguration {

    /**
     * 将容器中的 MessageSource 注入静态解析器。
     *
     * @param messageSource 容器中的 MessageSource；缺失时为 null（I18nMessageResolver 退回默认行为）
     */
    @Autowired(required = false)
    public void initResolver(MessageSource messageSource) {
        I18nMessageResolver.setMessageSource(messageSource);
    }
}
