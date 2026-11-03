package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.ArrayList;
import java.util.List;

/** 向量模型端口：由 product 注入具体实现（OpenAI / DashScope / BGE 等）。 */
public interface EmbeddingPort {
    float[] embed(String text);

    default List<float[]> embedAll(List<String> texts) {
        List<float[]> out = new ArrayList<>();
        if (texts == null) {
            return out;
        }
        for (String t : texts) {
            out.add(embed(t));
        }
        return out;
    }
}
