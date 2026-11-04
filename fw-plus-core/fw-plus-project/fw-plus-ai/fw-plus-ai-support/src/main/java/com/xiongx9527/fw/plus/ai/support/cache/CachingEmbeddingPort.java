package com.xiongx9527.fw.plus.ai.support.cache;

import com.xiongx9527.fw.plus.ai.base.cache.CachePort;
import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import java.util.ArrayList;
import java.util.List;

/**
 * 给 {@link EmbeddingPort} 加缓存：同一段文本重复编码（同一批文档反复召回）时不再打上游。
 *
 * <p>缓存键拼入 {@link CachePort#epoch()}，一旦数据变更 {@code bumpEpoch}，所有旧向量键自然失效。</p>
 */
public final class CachingEmbeddingPort implements EmbeddingPort {

    private final EmbeddingPort delegate;

    private final CachePort cache;

    private final long ttlMillis;

    public CachingEmbeddingPort(EmbeddingPort delegate, CachePort cache, long ttlMillis) {
        this.delegate = delegate;
        this.cache = cache;
        this.ttlMillis = ttlMillis <= 0 ? 3_600_000L : ttlMillis;
    }

    @Override
    public double[] embed(String text) {
        if (text == null) {
            return delegate.embed(null);
        }
        String key = "emb:" + cache.epoch() + ":" + text;
        Object hit = cache.get(key);
        if (hit instanceof double[]) {
            return (double[]) hit;
        }
        double[] vector = delegate.embed(text);
        if (vector != null) {
            cache.put(key, vector, ttlMillis);
        }
        return vector;
    }

    @Override
    public List<double[]> embedAll(List<String> texts) {
        List<double[]> out = new ArrayList<double[]>(texts == null ? 0 : texts.size());
        if (texts == null) {
            return out;
        }
        for (String text : texts) {
            out.add(embed(text));
        }
        return out;
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    @Override
    public int dimension() {
        return delegate.dimension();
    }
}
