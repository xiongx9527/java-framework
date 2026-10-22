package com.xiongx9527.fw.plus.boot.autoconfigure.i18n;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.i18n.I18nAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.i18n.I18nResolverConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.properties.WebProperties;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.MessageSource;

/**
 * {@link I18nAutoConfiguration} 上下文装载回归测试。
 *
 * <p>本测试锁定两项关键契约：</p>
 * <ol>
 *   <li>Servlet Web 上下文能正常装载 {@code I18nAutoConfiguration} + {@link I18nResolverConfiguration}，
 *       不再出现「同配置类内声明 {@code messageSource} {@code @Bean} 又索取它」触发的
 *       {@code BeanCurrentlyInCreationException} 循环依赖；</li>
 *   <li>装载完成后 {@link I18nMessageResolver#getMessageSource()} 已被容器注入为非空的
 *       {@link MessageSource}，证明注入路径生效（而非仅回退 {@code BundleLoader}）。</li>
 * </ol>
 *
 * <p>注意：{@link I18nMessageResolver} 是静态单例，本测试在 {@code @AfterEach} 复位，避免污染其它用例。</p>
 */
@DisplayName("I18nAutoConfiguration 上下文装载")
class I18nAutoConfigurationContextTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withBean(WebProperties.class)
            .withConfiguration(AutoConfigurations.of(
                    I18nAutoConfiguration.class,
                    I18nResolverConfiguration.class));

    @AfterEach
    void resetResolver() {
        I18nMessageResolver.setMessageSource(null);
    }

    @Test
    @DisplayName("上下文装载无循环依赖且 MessageSource 被注入")
    void contextLoadsAndInjectsMessageSource() {
        runner.run(context -> {
            // 1) 上下文成功刷新即证明未发生 BeanCurrentlyInCreationException 循环依赖
            // 2) messageSource bean 存在
            context.getBean(MessageSource.class);
            // 3) 注入确实写入了静态解析器
            if (I18nMessageResolver.getMessageSource() == null) {
                throw new AssertionError("I18nMessageResolver 未被注入 MessageSource");
            }
        });
    }
}
