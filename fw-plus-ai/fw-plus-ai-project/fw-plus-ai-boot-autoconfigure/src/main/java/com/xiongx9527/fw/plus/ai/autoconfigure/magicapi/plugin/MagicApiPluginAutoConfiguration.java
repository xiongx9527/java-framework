package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.starter.MagicAPIAiaConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.starter.MagicAPIModelConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.starter.MagicAPIVectorConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@AutoConfigureAfter(name = "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.EmbeddingStoreAutoConfiguration")
@Import({MagicAPIAiaConfiguration.class,
        MagicAPIModelConfiguration.class,
        MagicAPIVectorConfiguration.class
})
public class MagicApiPluginAutoConfiguration {
}
