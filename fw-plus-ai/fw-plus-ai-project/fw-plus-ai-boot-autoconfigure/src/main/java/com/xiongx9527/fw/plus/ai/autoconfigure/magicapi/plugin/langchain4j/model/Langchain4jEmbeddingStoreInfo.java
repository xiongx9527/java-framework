package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** embedding-store 资源：定义一个 EmbeddingStore<TextSegment> 实例，必须搭配一个 embedding-model。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jEmbeddingStoreInfo extends Langchain4jModelInfo {
    /** 必填：引用的 embedding-model key */
    private String embeddingModelKey;
}
