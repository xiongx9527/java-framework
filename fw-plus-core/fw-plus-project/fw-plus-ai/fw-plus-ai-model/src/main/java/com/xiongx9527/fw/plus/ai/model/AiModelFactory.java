package com.xiongx9527.fw.plus.ai.model;

import java.util.List;

public interface AiModelFactory {

    AiModel create(AiModelDefinition definition, List<AiModelAdapter> adapters);
}
