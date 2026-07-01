package com.xx9527.fw.plus.boot.autoconfigure.mate.webmvc;

import com.xx9527.fw.plus.boot.autoconfigure.mate.webmvc.validator.CommonsValidatorRequestBodyAdvice;
import com.xx9527.fw.plus.boot.autoconfigure.mate.webmvc.validator.CommonsValidatorResponseBodyAdvice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
@ConditionalOnProperty(prefix = "fw.plus.mate.validator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MateWebMvcValidatorAutoConfiguration {


    /**
     * 创建并配置一个Bean对象：CommonsValidatorRequestBodyAdvice
     *
     * 该方法通过Spring框架的@Bean注解定义了一个Bean，这个Bean是一个CommonsValidatorRequestBodyAdvice实例
     * 它的作用是全局性地处理请求体的验证逻辑，确保所有传入的请求体都符合预期的格式和规则
     *
     * @return CommonsValidatorRequestBodyAdvice的实例，用于全局请求体验证
     */
    @Bean
    public CommonsValidatorRequestBodyAdvice commonsValidatorRequestBodyAdvice() {
        return new CommonsValidatorRequestBodyAdvice();
    }

    /**
     * 创建并配置一个Bean对象：CommonsValidatorResponseBodyAdvice
     * @return
     */
    @Bean
    public CommonsValidatorResponseBodyAdvice commonsValidatorResponseBodyAdvice() {
        return new CommonsValidatorResponseBodyAdvice();
    }


}
