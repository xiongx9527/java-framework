package com.xiongx9527.fw.plus.boot.autoconfigure.uid.jdbc;

import com.xiongx9527.fw.plus.boot.autoconfigure.uid.jdbc.worker.DisposableWorkerIdAssigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import com.xiongx9527.fw.plus.common.uid.worker.WorkerIdAssigner ;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnClass(name="org.springframework.jdbc.core.JdbcTemplate")
@AutoConfigureAfter({DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
@Configuration
public class UIDJdbcAutoConfiguration {


    /**
     * 创建并配置WorkerIdAssigner实例
     * 该方法利用Spring的依赖注入功能，生成一个WorkerIdAssigner的实例
     * 主要用于在分布式系统中分配唯一的worker ID
     *
     * @param jdbcTemplate Spring JdbcTemplate实例，用于执行SQL查询和更新
     * @return WorkerIdAssigner实例，具体实现为DisposableWorkerIdAssigner
     *
     * @ConditionalOnMissingBean 注解说明只有在当前上下文中不存在指定的Bean时，
     *         才会创建并注册当前的Bean，这里确保不会重复创建WorkerIdAssigner的实例
     */
    @Bean
    @ConditionalOnMissingBean(WorkerIdAssigner.class)
    public WorkerIdAssigner workerIdAssigner(@Autowired(required = false) org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return jdbcTemplate != null ? new DisposableWorkerIdAssigner(jdbcTemplate):null;
    }



}
