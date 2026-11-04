package com.xiongx9527.fw.plus.ai.interactive.appointment;

import com.xiongx9527.fw.plus.ai.interactive.AnswerExtractor;
import com.xiongx9527.fw.plus.ai.interactive.ClarifyOption;
import com.xiongx9527.fw.plus.ai.interactive.CollectionResult;
import com.xiongx9527.fw.plus.ai.interactive.Form;
import com.xiongx9527.fw.plus.ai.interactive.FormCollector;
import com.xiongx9527.fw.plus.ai.interactive.FormCollector.CollectedField;
import com.xiongx9527.fw.plus.ai.interactive.Question;

import java.util.Map;

/**
 * 智能预约场景流（复用交互通用原语 {@link FormCollector}）。
 *
 * <p>要点：</p>
 * <ul>
 *   <li>预约先要有{@link AppointmentItem}（预约项目，携带可预约窗口 {@link AppointmentItem#getSlots()}）；</li>
 *   <li>预约模板是不定的：业务侧提供 {@link Form} 模板（不同项目收集不同字段），框架不硬编码；
 *       用 {@link #timeSlotQuestion(AppointmentItem)} 把窗口接成"模型列出、用户选一个"的单选；</li>
 *   <li>收集齐后 {@link #toRequest()} 映射为带 itemId 的 {@link AppointmentRequest} 交业务方提交；</li>
 *   <li>提交前的"确认"由 {@code ...interactive.confirm.ConfirmationService} 负责。</li>
 * </ul>
 */
public final class AppointmentFlow {

    public static final String Q_TIME_SLOT = "timeSlot";

    private final String itemId;
    private final FormCollector collector;

    private AppointmentFlow(String itemId, FormCollector collector) {
        this.itemId = itemId;
        this.collector = collector;
    }

    /** 用预约项目 + 业务自定义模板构造收集流。 */
    public static AppointmentFlow create(AppointmentItem item, Form template) {
        if (item == null) {
            throw new IllegalArgumentException("appointment item is required");
        }
        if (template == null) {
            throw new IllegalArgumentException("appointment template is required");
        }
        return new AppointmentFlow(item.getId(), new FormCollector(template));
    }

    /** 标准时段单选题：选项来自预约项目的可预约窗口（模型列出、用户选一个）。 */
    public static Question timeSlotQuestion(AppointmentItem item) {
        if (item == null) {
            throw new IllegalArgumentException("appointment item is required");
        }
        return Question.builder().name(Q_TIME_SLOT).kind(Question.Kind.SINGLE_CHOICE)
                .titleKey("appointment.q.timeSlot").required(true)
                .options(item.getSlots()).build();
    }

    public CollectionResult submitAnswer(String name, Object value) {
        return collector.submitAnswer(name, value);
    }

    public CollectionResult submitUtterance(String utterance, AnswerExtractor extractor) {
        return collector.submitUtterance(utterance, extractor);
    }

    public CollectionResult proceed() {
        return collector.proceed();
    }

    public boolean isComplete() {
        return collector.isComplete();
    }

    public Map<String, Object> getAnswers() {
        return collector.getAnswers();
    }

    public java.util.List<CollectedField> summary() {
        return collector.summary();
    }

    /** 收集完成后映射为预约请求（含 itemId）；未完成时返回 null。 */
    public AppointmentRequest toRequest() {
        if (!collector.isComplete()) {
            return null;
        }
        Map<String, Object> a = collector.getAnswers();
        return AppointmentRequest.builder()
                .itemId(itemId)
                .timeSlotId(str(a.get(Q_TIME_SLOT)))
                .answers(a)
                .build();
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
