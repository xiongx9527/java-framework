package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@MagicModule("model")
public class ModelModule implements DynamicAttribute<ModelModule, ModelModule>, DynamicModule<ModelModule> {

    private final MagicDynamicModelDataSource dataSource;
    private final AiModel model;

    public ModelModule(MagicDynamicModelDataSource dataSource) {
        this(dataSource, null);
    }

    private ModelModule(MagicDynamicModelDataSource dataSource, AiModel model) {
        this.dataSource = dataSource;
        this.model = model;
    }

    public AiModel getModelObject() {
        return model == null ? dataSource.getDefault() : model;
    }

    public AiModel getModel() {
        return getModelObject();
    }

    @Override
    public ModelModule getDynamicAttribute(String key) {
        return new ModelModule(dataSource, dataSource.getModel(key));
    }

    @Override
    public ModelModule getDynamicModule(MagicScriptContext context) {
        String key = context == null ? null : context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        return isBlank(key) ? new ModelModule(dataSource, dataSource.getDefault()) : getDynamicAttribute(key);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
