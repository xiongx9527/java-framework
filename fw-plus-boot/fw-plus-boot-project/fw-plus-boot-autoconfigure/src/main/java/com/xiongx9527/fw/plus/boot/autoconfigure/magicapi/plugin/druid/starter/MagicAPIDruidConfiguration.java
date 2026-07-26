package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.properties.DruidStatProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidFilterConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidSpringAopConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidStatViewServletConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidWebStatFilterConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

@Slf4j
@ConditionalOnProperty(prefix = "magic-api.plugin.druid", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"com.alibaba.druid.pool.DruidDataSource"})
@Configuration
@EnableConfigurationProperties({DruidStatProperties.class})
@Import({DruidSpringAopConfiguration.class,
        DruidStatViewServletConfiguration.class,
        DruidWebStatFilterConfiguration.class,
        DruidFilterConfiguration.class})
public class MagicAPIDruidConfiguration implements MagicPluginConfiguration {
	private static final String JAVASCRIPT_FILENAME = "magic-druid.1.0.0.iife.js";
	private static final String FRONTEND_RESOURCE = "magic-editor/plugins/" + JAVASCRIPT_FILENAME;

    /**
     * 获取插件实例
     *
     * @return 返回名为"druid"的插件对象
     */
    @Override
    public Plugin plugin() {
		if (MagicAPIDruidConfiguration.class.getClassLoader().getResource(FRONTEND_RESOURCE) == null) {
			return new Plugin("druid");
		}
		return new Plugin("druid", "MagicDruid", JAVASCRIPT_FILENAME);
    }


}
