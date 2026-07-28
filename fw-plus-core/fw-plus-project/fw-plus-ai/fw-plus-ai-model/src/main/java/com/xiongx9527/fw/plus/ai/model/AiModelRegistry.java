package com.xiongx9527.fw.plus.ai.model;

import java.util.Set;

public interface AiModelRegistry {

    AiModel get(String key);

    AiModel getDefault();

    String getDefaultKey();

    boolean contains(String key);

    Set<String> keys();
}
