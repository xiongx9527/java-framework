package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.support.LcVectors;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.List;

/** 把 LangChain4j 0.35 的 {@link EmbeddingModel} 适配成 base 的 {@link EmbeddingPort}。 */
public class LangChain4jEmbeddingPort implements EmbeddingPort {

    private final EmbeddingModel model;

    public LangChain4jEmbeddingPort(EmbeddingModel model) {
        if (model == null) {
            throw new IllegalArgumentException("embedding model is required");
        }
        this.model = model;
    }

    @Override
    public double[] embed(String text) {
        if (text == null) {
            return null;
        }
        Response<Embedding> response = model.embed(text);
        return response == null || response.content() == null
                ? null : LcVectors.toDouble(response.content());
    }

    @Override
    public List<double[]> embedAll(List<String> texts) {
        List<double[]> out = new ArrayList<double[]>();
        if (texts == null || texts.isEmpty()) {
            return out;
        }
        List<TextSegment> segments = new ArrayList<TextSegment>();
        for (String text : texts) {
            segments.add(TextSegment.from(text == null ? "" : text));
        }
        Response<List<Embedding>> response = model.embedAll(segments);
        if (response == null || response.content() == null) {
            for (int i = 0; i < texts.size(); i++) {
                out.add(null);
            }
            return out;
        }
        for (Embedding embedding : response.content()) {
            out.add(LcVectors.toDouble(embedding));
        }
        return out;
    }

    @Override
    public int dimension() {
        return model.dimension();
    }
}
