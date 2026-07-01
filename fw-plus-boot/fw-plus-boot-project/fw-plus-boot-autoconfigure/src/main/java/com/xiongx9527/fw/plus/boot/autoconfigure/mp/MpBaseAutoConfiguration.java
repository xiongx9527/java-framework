package com.xiongx9527.fw.plus.boot.autoconfigure.mp;

import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.OptimisticLockerInterceptor;
import com.github.yulichang.autoconfigure.conditional.JoinSqlInjectorCondition;
import com.xiongx9527.fw.plus.mp.base.MpSqlInjector;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * MyBatis-Plus基础自动配置类
 * 提供MyBatis-Plus核心功能的自动配置，包括SQL注入器和乐观锁拦截器
 */
@Slf4j
public class MpBaseAutoConfiguration {
    /**
     * 创建并配置自定义的MyBatis-Plus SQL注入器
     *
     * 条件注解说明：
     * - @JoinSqlInjectorCondition: 自定义条件，判断是否需要创建此注入器
     * - @Order(Integer.MIN_VALUE): 确保此Bean具有最高优先级
     * - @ConditionalOnBean: 确保ISqlInjector类型的Bean存在时才创建
     * - @ConditionalOnMissingBean: 确保MpSqlInjector类型的Bean不存在时才创建
     * - @ConditionalOnClass: 确保相关类存在时才创建
     *
     * @param sqlInjector MyBatis-Plus提供的SQL注入器
     * @return 自定义的SQL注入器实例
     */
    @Bean
    @JoinSqlInjectorCondition
    @Order(Integer.MIN_VALUE)
    @ConditionalOnBean({ISqlInjector.class})
    @ConditionalOnMissingBean(MpSqlInjector.class)
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public MpSqlInjector mpSqlInjector(ISqlInjector sqlInjector) {
        // 日志记录：初始化Mybatis Plus的SqlInjector
        log.info("fw plus mybatis plus SqlInjector init");
        return new MpSqlInjector(sqlInjector);
    }

    /**
     * 创建并配置乐观锁拦截器
     *
     * 此方法通过Spring的@Bean注解定义了一个Bean，确保在Spring容器中创建并管理这个Bean的生命周期
     * 使用@ConditionalOnMissingBean注解，只有在Spring容器中不存在其他符合条件的OptimisticLockerInterceptor Bean时，才会创建当前Bean
     * 这样做可以避免Bean的重复创建，同时确保应用在需要时能够正确使用乐观锁功能
     *
     * @return 返回配置好的乐观锁拦截器实例
     */
    @Bean
    @ConditionalOnMissingBean({OptimisticLockerInterceptor.class})
    public com.baomidou.mybatisplus.extension.plugins.OptimisticLockerInterceptor optimisticLockerInterceptor() {
        com.baomidou.mybatisplus.extension.plugins.OptimisticLockerInterceptor optimisticLockerInterceptor = new com.baomidou.mybatisplus.extension.plugins.OptimisticLockerInterceptor();
        return optimisticLockerInterceptor;
    }

}
