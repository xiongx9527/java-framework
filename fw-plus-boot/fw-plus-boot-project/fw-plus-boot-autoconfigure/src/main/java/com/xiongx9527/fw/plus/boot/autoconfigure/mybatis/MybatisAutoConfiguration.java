package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis;

import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datachange.MybatisDataChangeAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datascope.MybatisDataScopeAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf.MfDataChangeAdapterAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf.MfDataScopeAdapterAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf.MfEncryptAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp.MpAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp.MpDataChangeAdapterAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp.MpEncryptAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * MyBatis 生态自动配置聚合入口。
 *
 * <p>仅当 classpath 存在 mybatis-spring（{@code SqlSessionFactoryBean}）时启用整个 MyBatis 模块。
 * 通过 {@link Import} 聚合 MyBatis-Plus / MyBatis-Flex / 通用数据变更追踪等子配置，
 * 使 {@code spring.factories} 中 MyBatis 模块从 5 行收敛为 1 行。</p>
 *
 * <p>子配置原有的 {@code @AutoConfigureAfter} 在 {@code @Import} 场景下不再生效，
 * 由本类的 {@link AutoConfigureAfter} 统一声明在 DataSource / Mybatis(Plus/Flex) 之后处理，
 * 使 {@code @Import} 的子配置在求值条件时依赖 bean 已就绪。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.mybatis.spring.SqlSessionFactoryBean")
@AutoConfigureAfter(name = {
		"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration",
		"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
		"com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
		"com.mybatisflex.spring.boot.MybatisFlexAutoConfiguration"
})
@Import({
		MpAutoConfiguration.class,
		MpEncryptAutoConfiguration.class,
		MybatisDataChangeAutoConfiguration.class,
		MybatisDataScopeAutoConfiguration.class,
		MpDataChangeAdapterAutoConfiguration.class,
		MfDataChangeAdapterAutoConfiguration.class,
		MfDataScopeAdapterAutoConfiguration.class,
		MfEncryptAutoConfiguration.class
})
public class MybatisAutoConfiguration {
}
