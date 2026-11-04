package com.xiongx9527.fw.plus.ai.interactive.appointment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次预约请求（智能预约场景的最终产出）。
 *
 * <p>由 {@link AppointmentFlow#toRequest()} 从收集结果映射得到：预约项目 id + 所选时段 id +
 * 其余收集字段（{@link #getAnswers()}）。由于预约模板是不定的（不同预约项目收集不同字段），
 * 具体字段由业务按题干名自行解释并提交；框架只保证 {@code itemId} / {@code timeSlotId} 透传。</p>
 */
public final class AppointmentRequest {

    private final String itemId;
    private final String timeSlotId;
    private final Map<String, Object> answers;

    private AppointmentRequest(Builder builder) {
        this.itemId = builder.itemId;
        this.timeSlotId = builder.timeSlotId;
        this.answers = builder.answers == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.answers));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 预约项目 id（业务据此区分预约的是哪个项目、调用哪个落库接口）。 */
    public String getItemId() {
        return itemId;
    }

    /** 所选时段 id（来自预约项目的可预约窗口）。 */
    public String getTimeSlotId() {
        return timeSlotId;
    }

    /** 全部收集字段：题干名 → 答案（含时段；业务按名称解释）。 */
    public Map<String, Object> getAnswers() {
        return answers;
    }

    public static final class Builder {

        private String itemId;
        private String timeSlotId;
        private Map<String, Object> answers;

        public Builder itemId(String value) {
            this.itemId = value;
            return this;
        }

        public Builder timeSlotId(String value) {
            this.timeSlotId = value;
            return this;
        }

        public Builder answers(Map<String, Object> value) {
            this.answers = value;
            return this;
        }

        public AppointmentRequest build() {
            return new AppointmentRequest(this);
        }
    }
}
