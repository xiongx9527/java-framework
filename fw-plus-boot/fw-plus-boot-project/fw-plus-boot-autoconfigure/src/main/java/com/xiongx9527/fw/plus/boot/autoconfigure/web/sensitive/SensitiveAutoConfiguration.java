package com.xiongx9527.fw.plus.boot.autoconfigure.web.sensitive;

import com.xiongx9527.fw.plus.boot.common.CommonAutoConfiguration;
import com.xiongx9527.fw.plus.common.databind.ISensitiveStrategy;
import com.xiongx9527.fw.plus.common.databind.SensitiveSerializer;
import com.xiongx9527.fw.plus.common.strategy.SensitiveStrategy;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * 敏感信息自动配置类
 * @author EDY
 */
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
public class SensitiveAutoConfiguration extends CommonAutoConfiguration implements InitializingBean {

    /**
     * 敏感数据处理策略接口注入
     * 通过Spring的@Autowired注解实现依赖注入，required = false表示该依赖不是必须的，
     * 当容器中不存在ISensitiveStrategy类型的Bean时不会抛出异常
     */
    @Autowired(required = false)
    private ISensitiveStrategy sensitiveStrategy;


    /**
     * 创建一个ControllerFieldIgnoreSensitive bean
     * 该bean的作用是处理控制器层的字段忽略敏感信息
     *
     * @return ControllerFieldIgnoreSensitive实例，用于全局处理敏感字段忽略逻辑
     */
    @Bean
    @ConditionalOnClass({Aspect.class})
    public SensitiveAop controllerAop() {
        // 初始化ControllerFieldIgnoreSensitive，并设置其忽略敏感信息的属性为true
        return new SensitiveAop();
    }

    /**
     * 在所有依赖属性设置完成后执行此方法
     * 该方法主要用于在当前对象的所有属性都被设置后，进行一些额外的初始化操作
     * 具体来说，如果敏感信息处理策略（sensitiveStrategy）不为空，则将其设置为全局敏感信息序列化策略
     * 这样做是为了确保在整个应用程序中，敏感信息的处理方式是一致的
     *
     * @throws Exception 如果敏感策略的设置过程中出现异常，将会抛出此异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        SensitiveSerializer.setSensitiveStrategy(null != this.sensitiveStrategy?
                this.sensitiveStrategy: new SensitiveStrategy() );
    }
}
