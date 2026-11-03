package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** 多路融合：WEIGHTED 按权重线性相加；RRF 按排名倒数相加（k=60）。 */
public final class Fusion {

    private static final int RRF_K = 60;

    private Fusion() {
    }

    /**
     * @param lists   每路结果（已按各自 topK 截断）
     * @param weights 与 lists 对齐的权重，缺省 1.0
     * @param keyFn   条目去重键（如 Document::getId）
     */
    public static <T> List<Retrieved<T>> fuse(List<List<Retrieved<T>>> lists, List<Double> weights,
                                              FusionMode mode, int topK, Function<T, String> keyFn) {
        Map<String, Retrieved<T>> byId = new LinkedHashMap<String, Retrieved<T>>();
        Map<String, Double> scores = new HashMap<String, Double>();
        if (lists == null) {
            return new ArrayList<Retrieved<T>>();
        }
        FusionMode m = mode == null ? FusionMode.WEIGHTED : mode;
        for (int li = 0; li < lists.size(); li++) {
            List<Retrieved<T>> list = lists.get(li);
            if (list == null) {
                continue;
            }
            double w = weights != null && li < weights.size() && weights.get(li) != null ? weights.get(li) : 1.0;
            for (int i = 0; i < list.size(); i++) {
                Retrieved<T> hit = list.get(i);
                if (hit == null || hit.getItem() == null) {
                    continue;
                }
                String key = keyFn == null ? String.valueOf(hit.getItem())
                        : String.valueOf(keyFn.apply(hit.getItem()));
                byId.putIfAbsent(key, hit);
                double add = m == FusionMode.RRF ? w / (RRF_K + (i + 1)) : w * hit.getScore();
                scores.merge(key, add, Double::sum);
            }
        }
        List<Retrieved<T>> out = new ArrayList<Retrieved<T>>();
        for (Map.Entry<String, Retrieved<T>> e : byId.entrySet()) {
            out.add(e.getValue().withScore(scores.get(e.getKey())));
        }
        out.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (topK > 0 && out.size() > topK) {
            return new ArrayList<Retrieved<T>>(out.subList(0, topK));
        }
        return out;
    }
}
