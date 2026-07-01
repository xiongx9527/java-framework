package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.starter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.properties.DruidStatProperties;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidFilterConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidSpringAopConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidStatViewServletConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat.DruidWebStatFilterConfiguration;
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

    /**
     * 获取插件实例
     *
     * @return 返回名为"druid"的插件对象
     */
    @Override
    public Plugin plugin() {
        return new Plugin("druid");
    }


}
