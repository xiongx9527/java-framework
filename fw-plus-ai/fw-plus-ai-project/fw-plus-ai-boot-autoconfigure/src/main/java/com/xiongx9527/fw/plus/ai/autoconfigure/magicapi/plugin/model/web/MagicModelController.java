package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class MagicModelController extends MagicController implements MagicExceptionHandler {

    private final MagicDynamicModelDataSource dataSource;
    private final LangChain4jModelFactory modelFactory;
    private final List<LangChain4jModelProviderAdapter> providerAdapters;

    public MagicModelController(MagicConfiguration configuration) {
        this(configuration, null, null, null);
    }

    public MagicModelController(MagicConfiguration configuration, LangChain4jModelFactory modelFactory,
                                List<LangChain4jModelProviderAdapter> providerAdapters) {
        this(configuration, null, modelFactory, providerAdapters);
    }

    public MagicModelController(MagicConfiguration configuration, MagicDynamicModelDataSource dataSource,
                                LangChain4jModelFactory modelFactory,
                                List<LangChain4jModelProviderAdapter> providerAdapters) {
        super(configuration);
        this.dataSource = dataSource;
        this.modelFactory = modelFactory;
        this.providerAdapters = providerAdapters;
    }

    @RequestMapping("/model/datasource/list")
    @ResponseBody
    public JsonBean<List<ModelDataSourceInfo>> list() {
        List<ModelDataSourceInfo> result = new ArrayList<ModelDataSourceInfo>();
        for (ModelDataSourceInfo info : getDataSource().datasourceNodes()) {
            result.add(info.simple());
        }
        return new JsonBean<List<ModelDataSourceInfo>>(result);
    }

    @RequestMapping("/model/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody ModelDataSourceInfo definition) {
        try {
            validate(definition);
            getModelFactory().create(definition.toDefinition(), getProviderAdapters());
            return new JsonBean<Object>("ok");
        } catch (Exception e) {
            log.error("测试 Model 数据源失败，数据源 key: {}", safeKey(definition));
            return new JsonBean<Object>(-1, "模型数据源测试失败", null);
        }
    }

    private void validate(ModelDataSourceInfo definition) {
        if (definition == null) {
            throw new IllegalArgumentException("model datasource must not be null");
        }
        validateRequired(definition.getKey(), "key");
        validateRequired(definition.getProvider(), "provider");
        if (definition.getModelType() == null) {
            throw new IllegalArgumentException("modelType must not be null");
        }
    }

    private void validateRequired(String value, String field) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private MagicDynamicModelDataSource getDataSource() {
        return dataSource == null ? SpringUtils.getBean(MagicDynamicModelDataSource.class) : dataSource;
    }

    private LangChain4jModelFactory getModelFactory() {
        return modelFactory == null ? SpringUtils.getBean(LangChain4jModelFactory.class) : modelFactory;
    }

    private List<LangChain4jModelProviderAdapter> getProviderAdapters() {
        if (providerAdapters != null) {
            return providerAdapters;
        }
        Map<String, LangChain4jModelProviderAdapter> adapters = SpringUtils.getBeansOfType(LangChain4jModelProviderAdapter.class);
        return adapters == null ? Collections.<LangChain4jModelProviderAdapter>emptyList()
                : new ArrayList<LangChain4jModelProviderAdapter>(adapters.values());
    }

    private String safeKey(ModelDataSourceInfo definition) {
        return definition == null || definition.getKey() == null ? "unknown" : definition.getKey();
    }
}
