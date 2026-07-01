package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat;

import com.alibaba.druid.support.spring.stat.DruidStatInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.properties.DruidStatProperties;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.RegexpMethodPointcutAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * @author lihengming [89921218@qq.com]
 */
@ConditionalOnProperty("magic-api.plugin.druid.aop-patterns")
public class DruidSpringAopConfiguration {

    /**
     * 创建Druid统计拦截器Bean
     *
     * @return DruidStatInterceptor实例，用于拦截并统计方法执行信息
     */
    @Bean
    public Advice advice() {
        return new DruidStatInterceptor();
    }

    /**
     * 创建Advisor Bean，用于定义切面通知
     *
     * @param properties Druid统计属性配置，包含AOP切点表达式等配置信息
     * @return RegexpMethodPointcutAdvisor实例，基于正则表达式的方法切点通知器
     */
    @Bean
    public Advisor advisor(DruidStatProperties properties) {
        return new RegexpMethodPointcutAdvisor(properties.getAopPatterns(), advice());
    }

    /**
     * 创建默认的Advisor自动代理创建器Bean
     * 该Bean仅在spring.aop.auto属性为false时生效，用于手动控制AOP代理创建
     *
     * @return DefaultAdvisorAutoProxyCreator实例，负责创建基于Advisor的代理对象
     */
    @Bean
    @ConditionalOnProperty(name = "spring.aop.auto", havingValue = "false")
    public DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
        // 创建代理创建器并设置使用CGLIB代理而非JDK动态代理
        DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        advisorAutoProxyCreator.setProxyTargetClass(true);
        return advisorAutoProxyCreator;
    }


}
