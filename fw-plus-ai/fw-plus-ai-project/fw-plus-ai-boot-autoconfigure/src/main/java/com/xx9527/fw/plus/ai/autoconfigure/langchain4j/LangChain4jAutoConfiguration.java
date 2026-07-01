package com.xx9527.fw.plus.ai.autoconfigure.langchain4j;

import com.xx9527.fw.plus.ai.autoconfigure.langchain4j.core.rag.RagAutoConfig;
import com.xx9527.fw.plus.ai.autoconfigure.langchain4j.service.AiServiceScannerProcessor;
import com.xx9527.fw.plus.ai.autoconfigure.langchain4j.service.AiServicesAutoConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = {"dev.langchain4j.Experimental","dev.langchain4j.chain.Chain"})
@AutoConfiguration
@Import({
        AiServicesAutoConfig.class,
        RagAutoConfig.class,
        AiServiceScannerProcessor.class
})
public class LangChain4jAutoConfiguration {
}
