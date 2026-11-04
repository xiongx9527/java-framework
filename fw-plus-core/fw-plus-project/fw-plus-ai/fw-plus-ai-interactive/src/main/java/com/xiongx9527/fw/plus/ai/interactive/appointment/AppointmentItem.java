package com.xiongx9527.fw.plus.ai.interactive.appointment;

import com.xiongx9527.fw.plus.ai.interactive.ClarifyOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 预约项目（被预约的对象 / 资源）：如会议室、某医生门诊、某节课。
 *
 * <p>预约项目由业务侧提供，框架只承载其"可预约窗口"（{@link #getSlots()}）。
 * {@link AppointmentFlow} 从这些窗口生成时段单选选项，并把 {@link #getId()} 透传到
 * {@link AppointmentRequest#getItemId()}，业务据此知道预约的是哪个项目、调用哪个落库接口。</p>
 */
public final class AppointmentItem {

    private final String id;
    private final String name;
    private final List<ClarifyOption> slots;

    private AppointmentItem(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.slots = builder.slots == null ? Collections.<ClarifyOption>emptyList()
                : Collections.unmodifiableList(new ArrayList<ClarifyOption>(builder.slots));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** 可预约窗口（每个 option 的 id 为时段标识，label 为展示，如 "11-10 09:00"）。 */
    public List<ClarifyOption> getSlots() {
        return slots;
    }

    public static final class Builder {

        private String id;
        private String name;
        private List<ClarifyOption> slots;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder slots(List<ClarifyOption> value) {
            this.slots = value;
            return this;
        }

        public AppointmentItem build() {
            if (id == null) {
                throw new IllegalStateException("AppointmentItem.id is required");
            }
            return new AppointmentItem(this);
        }
    }
}
