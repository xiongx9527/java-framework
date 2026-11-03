package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 多路融合：WEIGHTED 按权重线性相加；RRF 按排名倒数相加（k=60）。 */
public final class Fusion {

    private static final int RRF_K = 60;

    private Fusion() {
    }

    public static List<ScoredDocument> fuse(List<List<ScoredDocument>> lists, List<Double> weights,
                                            FusionMode mode, int topK) {
        Map<String, ScoredDocument> byId = new LinkedHashMap<>();
        Map<String, Double> scores = new HashMap<>();
        if (lists == null) {
            return new ArrayList<>();
        }
        FusionMode m = mode == null ? FusionMode.WEIGHTED : mode;
        for (int li = 0; li < lists.size(); li++) {
            List<ScoredDocument> list = lists.get(li);
            if (list == null) {
                continue;
            }
            double w = weights != null && li < weights.size() && weights.get(li) != null ? weights.get(li) : 1.0;
            for (int i = 0; i < list.size(); i++) {
                ScoredDocument sd = list.get(i);
                if (sd == null || sd.getDocument() == null) {
                    continue;
                }
                String id = sd.getDocument().getId() == null
                        ? String.valueOf(System.identityHashCode(sd.getDocument())) : sd.getDocument().getId();
                byId.putIfAbsent(id, sd);
                double add = m == FusionMode.RRF ? w / (RRF_K + (i + 1)) : w * sd.getScore();
                scores.merge(id, add, Double::sum);
            }
        }
        List<ScoredDocument> out = new ArrayList<>();
        for (Map.Entry<String, ScoredDocument> e : byId.entrySet()) {
            out.add(new ScoredDocument(e.getValue().getDocument(), scores.get(e.getKey())));
        }
        out.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (topK > 0 && out.size() > topK) {
            return new ArrayList<>(out.subList(0, topK));
        }
        return out;
    }
}
