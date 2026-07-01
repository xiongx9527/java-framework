package com.xiongx9527.fw.plus.boot.autoconfigure.mp;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

@ConditionalOnProperty(prefix = "fw.plus.mp", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@ConditionalOnClass(name = {"org.apache.ibatis.session.SqlSessionFactory",
                            "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
                            "com.github.yulichang.autoconfigure.conditional.JoinSqlInjectorCondition",
                            "org.mybatis.spring.SqlSessionFactoryBean"})
@ConditionalOnSingleCandidate(DataSource.class)
@AutoConfigureAfter(name={"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
"com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration"})
@Import({MpBaseAutoConfiguration.class, MpJoinAutoConfiguration.class,
        MpDataChangeAutoConfiguration.class,MpEncryptAutoConfiguration.class,
        MpDataScopeAutoConfiguration.class})
public class MpAutoConfiguration {
}
