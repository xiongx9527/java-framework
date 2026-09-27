package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.MagicApiPluginAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Configuration
@AutoConfigureAfter(name = {
        "com.xiongx9527.fw.plus.ai.autoconfigure.model.AiModelAutoConfiguration",
        "com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding.AiEmbeddingStoreAutoConfiguration",
        // 插件配置带 @ConditionalOnBean(MagicResourceService.class)，必须在 magic-api starter
        // 之后评估，否则该 bean 尚未注册，插件不生效。
        "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration"
})
//@ConditionalOnClass(name={"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration"})
@Import({MagicApiPluginAutoConfiguration.class})
public class MagicAPIBaseAutoConfiguration {
}
