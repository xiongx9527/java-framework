package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.module.Langchain4jToolProviderModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

public class MagicDynamicToolProvider implements ToolProvider {

    private final Langchain4jToolProviderModule toolProvider;

    private final String key;

    public MagicDynamicToolProvider(Langchain4jToolProviderModule toolProvider, String key) {
        this.toolProvider = toolProvider;
        this.key = key;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        try{
            return toolProvider.getDynamicAttribute(key).provideTools(request);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(ToolProvider.class).provideTools(request);
            }
            return SpringUtils.getBean(key,ToolProvider.class).provideTools(request);
        }
    }
}
