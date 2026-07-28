package com.xiongx9527.fw.plus.boot.autoconfigure.mp;

import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
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

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor optimisticLockerInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
