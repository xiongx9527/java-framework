package com.xiongx9527.fw.plus.ai.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 样例启动类。
 *
 * <p>本类必须放在 {@code com.xiongx9527.fw.plus.ai.sample} 而不是 {@code com.xiongx9527.fw.plus.ai}，
 * 也不能配 {@code scanBasePackages = "com.xiongx9527.fw.plus.ai"}：那样会把框架 jar 里的
 * {@code com.xiongx9527.fw.plus.ai.autoconfigure.**} 整棵树扫成组件，把本该由 spring.factories
 * 自动配置加载的 {@code @Configuration} 提前到「组件扫描」阶段注册。
 *
 * <p>组件扫描阶段早于自动配置阶段，而 {@code @ConditionalOnBean} 只查已注册的 bean 定义——
 * 于是 {@code KnowledgeConfiguration} 上对 {@code SqlSessionFactory} 的判断永远为 false
 * （{@code MybatisPlusAutoConfiguration} 还没轮到），knowledge 插件静默不注册：
 * 控制台看不到入口，条件评估报告里也只有一行「did not find any beans」，不报错。
 *
 * <p>放到子包后，{@code @SpringBootApplication} 默认扫描 {@code ...ai.sample.**}（只有本类），
 * 框架配置类全部走自动配置路径，{@code @AutoConfigureAfter} 的顺序保证才生效。
 */
@SpringBootApplication
public class FwPlusAISampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(FwPlusAISampleApplication.class, args);
    }
}
