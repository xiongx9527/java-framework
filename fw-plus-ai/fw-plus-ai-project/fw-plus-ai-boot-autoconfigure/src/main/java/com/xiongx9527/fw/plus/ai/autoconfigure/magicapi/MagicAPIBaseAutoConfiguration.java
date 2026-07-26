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
@AutoConfigureAfter(name = "com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.EmbeddingStoreAutoConfiguration")
//@ConditionalOnClass(name={"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration"})
@Import({MagicApiPluginAutoConfiguration.class})
public class MagicAPIBaseAutoConfiguration {
}
