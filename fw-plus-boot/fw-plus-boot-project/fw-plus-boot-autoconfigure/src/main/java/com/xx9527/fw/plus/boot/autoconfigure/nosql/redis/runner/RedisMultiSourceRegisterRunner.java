package com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.runner;


import com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.ApplicationContextHelper;
import com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.RedisHelper;
import com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.register.RedisDataSourceRegister;
import com.xx9527.fw.plus.nosql.redis.constant.DynamicRedisConstants;
import com.xx9527.fw.plus.nosql.redis.util.EnvironmentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;

import java.util.Set;

/**
 * Redis多数据源注册runner
 * 一个数据源对应一个RedisHelper，一个redisHelper（动态）中包含着多个RedisTemplate（每个db一个RedisTemplate）
 * 该runner的作用是，当容器启动完毕后，将容器中所有的多数据源的RedisHelper和RedisTemplate缓存起来
 *
 * @author Mr_wenpan@163.com 2021/09/06 10:33
 */
@Slf4j
@ConditionalOnProperty(prefix = "spring.redis", name = "dynamic-datasource",
        havingValue = "true", matchIfMissing = true)
@Configuration
public class RedisMultiSourceRegisterRunner implements CommandLineRunner, EnvironmentAware {

    /**
     * 环境对象，用于获取应用程序的环境配置信息
     * 该对象提供了访问配置属性、环境变量等环境相关信息的功能
     */
    private Environment environment;


    /**
     * 运行方法，用于注册多数据源的RedisHelper和RedisTemplate
     * 该方法会从环境配置中加载Redis数据源名称，并注册默认数据源和多数据源的Redis相关组件
     *
     * @param args 命令行参数，可变参数字符串数组
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void run(String... args) {
        // 获取所有数据源名称（注意：只包含spring.redis.datasource下的数据源）
        Set<String> dataSourceNames = EnvironmentUtil.loadRedisDataSourceName((AbstractEnvironment) environment);
        if (dataSourceNames.size() < 1) {
            log.warn("no multi datasource config, register multi datasource failed. please check config.");
            return;
        }

        // 注册默认数据源的redisHelper和redisTemplate(这两个bean在自动配置类中已经注入了)
        RedisHelper defaultRedisHelper = ApplicationContextHelper.getContext()
                .getBean(DynamicRedisConstants.DefaultRedisHelperName.REDIS_HELPER, RedisHelper.class);
        RedisTemplate defaultRedisTemplate = ApplicationContextHelper.getContext()
                .getBean(DynamicRedisConstants.DefaultRedisTemplateName.REDIS_TEMPLATE, RedisTemplate.class);
        RedisDataSourceRegister.registerRedisHelper(DynamicRedisConstants.MultiSource.DEFAULT_SOURCE, defaultRedisHelper);
        RedisDataSourceRegister.registerRedisTemplate(DynamicRedisConstants.MultiSource.DEFAULT_SOURCE, defaultRedisTemplate);

        // 遍历数据源名称，注册多数据源的RedisHelper和RedisTemplate
        dataSourceNames.forEach(name -> {
            String realTemplateName = name + DynamicRedisConstants.MultiSource.REDIS_TEMPLATE;
            String realHelperName = name + DynamicRedisConstants.MultiSource.REDIS_HELPER;
            // 通过数据源名称获取bean
            RedisTemplate redisTemplate = ApplicationContextHelper.getContext().getBean(realTemplateName, RedisTemplate.class);
            // 如果开启动态切换db则创建动态redisHelper，反之则创建静态redisHelper
            RedisHelper redisHelper = ApplicationContextHelper.getContext().getBean(realHelperName, RedisHelper.class);
            // 注册RedisTemplate
            RedisDataSourceRegister.registerRedisTemplate(name, redisTemplate);
            // 注册RedisHelper
            RedisDataSourceRegister.registerRedisHelper(name, redisHelper);
        });
    }

    /**
     * 设置环境对象
     *
     * @param environment 环境对象，不能为空
     */
    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

}
