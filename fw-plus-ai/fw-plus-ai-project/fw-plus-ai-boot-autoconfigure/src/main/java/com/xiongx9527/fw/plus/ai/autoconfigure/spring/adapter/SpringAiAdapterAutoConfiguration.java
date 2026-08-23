package com.xiongx9527.fw.plus.ai.autoconfigure.spring.adapter;

import com.xiongx9527.fw.plus.ai.autoconfigure.model.AiModelAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureBefore(AiModelAutoConfiguration.class)
@ConditionalOnProperty(prefix = "fw.plus.ai.spring.adapter", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiModelAdapter")
@Conditional(SpringAiModelApiAvailableCondition.class)
@Import({SpringAiModelAdapterAutoConfiguration.class, SpringAiAlibabaModelAdapterAutoConfiguration.class})
public class SpringAiAdapterAutoConfiguration {
}
