package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.properties.DruidStatProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidFilterConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidSpringAopConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidStatViewServletConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidWebStatFilterConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

@Slf4j
@ConditionalOnProperty(prefix = "magic-api.plugin.druid", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = {"com.alibaba.druid.pool.DruidDataSource"})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Configuration
@EnableConfigurationProperties({DruidStatProperties.class})
@Import({DruidSpringAopConfiguration.class,
        DruidStatViewServletConfiguration.class,
        DruidWebStatFilterConfiguration.class,
        DruidFilterConfiguration.class})
public class MagicAPIDruidConfiguration implements MagicPluginConfiguration {
	private static final String JAVASCRIPT_FILENAME = "magic-druid.1.0.0.iife.js";

    /**
     * 获取插件实例。
     *
     * <p>始终上报完整三元组（标识 + 全局名 + 前端产物名），不按前端产物是否落在 classpath
     * 上改变上报内容——产物在不在 classpath 是使用方的依赖配置问题，编码进注册逻辑只会让
     * 「依赖漏声明」在运行期表现为「插件静默消失」，排查成本远高于控制台直接报 404。
     *
     * @return 返回名为"druid"的插件对象
     */
    @Override
    public Plugin plugin() {
		return new Plugin("druid", "MagicDruid", JAVASCRIPT_FILENAME);
    }


}
