package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.starter.MagicAPIAiaConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.starter.MagicAPIQdrantConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.starter.MagicAPIWeaviateConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MagicAPIAiaConfiguration.class,
        MagicAPIQdrantConfiguration.class,
        MagicAPIWeaviateConfiguration.class
})
public class MagicApiPluginAutoConfiguration {
}
