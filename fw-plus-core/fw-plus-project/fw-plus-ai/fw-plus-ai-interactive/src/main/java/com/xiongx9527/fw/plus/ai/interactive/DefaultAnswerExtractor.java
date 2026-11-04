package com.xiongx9527.fw.plus.ai.interactive;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则版答案抽取（默认策略）。覆盖常见题型：
 * 填空按正则或整句取；单选恰好命中一个选项才确定；多选收集所有命中 id。
 * 更智能的抽取（如自然语言日期归一）由模型版实现替换，接口不变。
 */
public class DefaultAnswerExtractor implements AnswerExtractor {

    @Override
    public ExtractionResult extract(Question question, String utterance) {
        if (utterance == null) {
            return ExtractionResult.uncertain();
        }
        String u = utterance.trim();
        if (u.isEmpty()) {
            return ExtractionResult.uncertain();
        }
        switch (question.getKind()) {
            case MULTI_CHOICE:
                return extractMulti(question, u);
            case SINGLE_CHOICE:
                return extractSingle(question, u);
            case TEXT:
            default:
                return extractText(question, u);
        }
    }

    private ExtractionResult extractText(Question question, String u) {
        String regex = question.getPattern();
        if (regex != null && !regex.isEmpty()) {
            Matcher m = Pattern.compile(regex).matcher(u);
            if (m.find()) {
                String v = m.groupCount() >= 1 && m.group(1) != null ? m.group(1) : m.group(0);
                return ExtractionResult.certain(v);
            }
            return ExtractionResult.uncertain();
        }
        return ExtractionResult.certain(u);
    }

    private ExtractionResult extractSingle(Question question, String u) {
        String lower = u.toLowerCase(Locale.ROOT);
        String hitId = null;
        int hits = 0;
        for (ClarifyOption opt : question.getOptions()) {
            if (matchesOption(opt, lower)) {
                hitId = opt.getId();
                hits++;
            }
        }
        if (hits == 1) {
            return ExtractionResult.certain(hitId);
        }
        return ExtractionResult.uncertain();
    }

    private ExtractionResult extractMulti(Question question, String u) {
        String lower = u.toLowerCase(Locale.ROOT);
        List<String> ids = new ArrayList<String>();
        for (ClarifyOption opt : question.getOptions()) {
            if (matchesOption(opt, lower)) {
                ids.add(opt.getId());
            }
        }
        if (ids.isEmpty()) {
            return ExtractionResult.uncertain();
        }
        return ExtractionResult.certain(ids);
    }

    private static boolean matchesOption(ClarifyOption opt, String lower) {
        String label = opt.getLabel();
        if (label != null && !label.isEmpty()) {
            if (lower.contains(label.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        String id = opt.getId();
        if (id != null && !id.isEmpty()) {
            if (lower.contains(id.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
