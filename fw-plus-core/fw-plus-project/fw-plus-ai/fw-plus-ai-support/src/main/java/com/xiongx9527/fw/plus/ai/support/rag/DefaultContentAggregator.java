package com.xiongx9527.fw.plus.ai.support.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 默认聚合：按文本去重后拼接（保持首次出现顺序）。
 */
public class DefaultContentAggregator implements ContentAggregator {

    @Override
    public List<Content> aggregate(List<List<Content>> contents) {
        List<Content> result = new ArrayList<Content>();
        if (contents == null) {
            return result;
        }
        Set<String> seen = new LinkedHashSet<String>();
        for (List<Content> list : contents) {
            if (list == null) {
                continue;
            }
            for (Content content : list) {
                if (content == null || content.getText() == null) {
                    continue;
                }
                if (seen.add(content.getText())) {
                    result.add(content);
                }
            }
        }
        return result;
    }
}
