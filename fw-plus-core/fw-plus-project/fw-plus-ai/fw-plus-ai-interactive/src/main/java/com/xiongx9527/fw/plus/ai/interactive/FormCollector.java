package com.xiongx9527.fw.plus.ai.interactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 参数收集状态机（确定性反问引擎，对应文档 §6.31.6 / §6.32.5）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>维护槽位 / 答案状态，独立于对话历史（§6.31.4 规则 1）；</li>
 *   <li>缺参由代码判定（不交给模型）：必填且未填 / 低置信则反问；</li>
 *   <li>落实五条硬约束：一次最多问 3 个、同一 slot 不连续问两次、文案走 i18n、选项可校验、最大轮次降级；</li>
 *   <li>抽取（{@link AnswerExtractor}）与校验（{@link QuestionValidator}）均为可替换策略。</li>
 * </ul>
 *
 * <p>完成后业务方通过 {@link #getAnswers()} 拿到 题干名 → 答案 的映射，自行调用接口 / 工具提交。</p>
 */
public final class FormCollector {

    private final Form form;
    private final int maxFieldsPerFrame;
    private final int maxRounds;
    private final QuestionValidator validator = new QuestionValidator();
    private final Map<String, Object> answers = new LinkedHashMap<String, Object>();
    private final Set<String> lastAsked = new HashSet<String>();
    private int round;

    public FormCollector(Form form) {
        this(form, 3, 3);
    }

    public FormCollector(Form form, int maxFieldsPerFrame, int maxRounds) {
        if (form == null) {
            throw new IllegalArgumentException("form is required");
        }
        if (maxFieldsPerFrame < 1) {
            throw new IllegalArgumentException("maxFieldsPerFrame must be >= 1");
        }
        if (maxRounds < 1) {
            throw new IllegalArgumentException("maxRounds must be >= 1");
        }
        this.form = form;
        this.maxFieldsPerFrame = maxFieldsPerFrame;
        this.maxRounds = maxRounds;
    }

    /** 直接给定一道题的答案（已抽取 / 已校验的值），校验通过后写入并推进。 */
    public CollectionResult submitAnswer(String name, Object value) {
        Question q = find(name);
        if (q == null) {
            throw new InteractiveException("unknown question: " + name);
        }
        ValidationResult vr = validator.validate(q, value);
        if (!vr.isValid()) {
            throw new InteractiveException("invalid answer for " + name + ": " + vr.getKey());
        }
        answers.put(name, value);
        return proceed();
    }

    /**
     * 从用户一句话里抽取并校验各缺失必填项，再推进。
     * 抽取不确定或校验不通过的题保持缺失，下一轮继续反问。
     */
    public CollectionResult submitUtterance(String utterance, AnswerExtractor extractor) {
        if (extractor == null) {
            extractor = new DefaultAnswerExtractor();
        }
        for (Question q : form.getQuestions()) {
            if (isAnswered(q)) {
                continue;
            }
            ExtractionResult er = extractor.extract(q, utterance);
            if (er.isCertain()) {
                ValidationResult vr = validator.validate(q, er.getValue());
                if (vr.isValid()) {
                    answers.put(q.getName(), er.getValue());
                }
            }
        }
        return proceed();
    }

    /** 基于当前已收集答案计算下一步：完成 / 反问帧 / 降级。 */
    public CollectionResult proceed() {
        round++;
        List<Question> missing = missingRequired();
        if (missing.isEmpty()) {
            return CollectionResult.of(CollectionResult.Status.COMPLETE, null, snapshot(), null);
        }
        List<Question> askable = new ArrayList<Question>();
        for (Question q : missing) {
            if (lastAsked.contains(q.getName()) && !isAnswered(q)) {
                continue;
            }
            askable.add(q);
        }
        if (askable.isEmpty()) {
            // 仅剩上一轮已问但依旧未答的题：强制重问（换问法 / 给示例），避免遗漏卡死
            askable = new ArrayList<Question>(missing);
        }
        if (round > maxRounds) {
            return CollectionResult.of(CollectionResult.Status.DEGRADED, null, snapshot(), missing);
        }
        List<Question> asked = askable.subList(0, Math.min(maxFieldsPerFrame, askable.size()));
        lastAsked.clear();
        for (Question q : asked) {
            lastAsked.add(q.getName());
        }
        List<ClarifyField> fields = new ArrayList<ClarifyField>();
        for (Question q : asked) {
            fields.add(ClarifyField.fromQuestion(q).build());
        }
        ClarifyFrame frame = ClarifyFrame.builder()
                .reason(ClarifyReason.MISSING)
                .fields(fields)
                .build();
        return CollectionResult.of(CollectionResult.Status.INCOMPLETE, frame, snapshot(), missing);
    }

    /** 抽取结果：题干名 → 答案（业务方据此提交）。 */
    public Map<String, Object> getAnswers() {
        return snapshot();
    }

    /** 摘要：已收集字段的结构化清单（用于确认复述 / 观测），含题干 key 与展示值。 */
    public List<CollectedField> summary() {
        List<CollectedField> out = new ArrayList<CollectedField>();
        for (Question q : form.getQuestions()) {
            Object v = answers.get(q.getName());
            if (!validator.isBlank(v)) {
                out.add(new CollectedField(q.getName(), q.getTitleKey(), v));
            }
        }
        return out;
    }

    public boolean isComplete() {
        return missingRequired().isEmpty();
    }

    public int getRound() {
        return round;
    }

    private Map<String, Object> snapshot() {
        return new LinkedHashMap<String, Object>(answers);
    }

    private List<Question> missingRequired() {
        List<Question> m = new ArrayList<Question>();
        for (Question q : form.getQuestions()) {
            if (q.isRequired() && !isAnswered(q)) {
                m.add(q);
            }
        }
        return m;
    }

    private boolean isAnswered(Question q) {
        Object v = answers.get(q.getName());
        return v != null && !validator.isBlank(v);
    }

    private Question find(String name) {
        for (Question q : form.getQuestions()) {
            if (q.getName().equals(name)) {
                return q;
            }
        }
        return null;
    }

    /** 已收集字段的摘要单元：题干名、i18n title key、答案值。 */
    public static final class CollectedField {

        private final String name;
        private final String titleKey;
        private final Object value;

        CollectedField(String name, String titleKey, Object value) {
            this.name = name;
            this.titleKey = titleKey;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getTitleKey() {
            return titleKey;
        }

        public Object getValue() {
            return value;
        }
    }
}
