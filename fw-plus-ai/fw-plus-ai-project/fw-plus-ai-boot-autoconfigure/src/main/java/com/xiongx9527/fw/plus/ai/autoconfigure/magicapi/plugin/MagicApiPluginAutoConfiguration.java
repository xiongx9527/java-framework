package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@AutoConfigureAfter(name = {
        "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration"
})
@Import({MagicAPIMcpConfiguration.class})
public class MagicApiPluginAutoConfiguration {
}
