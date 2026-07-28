package com.xiongx9527.fw.plus.ai.model;

public interface AiModel {

    String getKey();

    String getFramework();

    String getProvider();

    AiModelType getType();
}
