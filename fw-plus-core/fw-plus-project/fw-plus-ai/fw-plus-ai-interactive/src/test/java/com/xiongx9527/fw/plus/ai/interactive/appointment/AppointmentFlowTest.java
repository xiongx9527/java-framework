package com.xiongx9527.fw.plus.ai.interactive.appointment;

import com.xiongx9527.fw.plus.ai.interactive.ClarifyOption;
import com.xiongx9527.fw.plus.ai.interactive.CollectionResult;
import com.xiongx9527.fw.plus.ai.interactive.Form;
import com.xiongx9527.fw.plus.ai.interactive.Question;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppointmentFlowTest {

    private AppointmentItem sampleItem() {
        List<ClarifyOption> slots = Arrays.asList(
                ClarifyOption.builder().id("s1").label("11-10 09:00").build(),
                ClarifyOption.builder().id("s2").label("11-10 14:00").build());
        return AppointmentItem.builder().id("room-a").name("会议室A").slots(slots).build();
    }

    // 预约模板是不定的：这里演示一个"会议室"模板（时段 + 参与人 + 备注），
    // 换成"门诊"模板时只需改这里传入的 Form，框架不变。
    private Form meetingTemplate(AppointmentItem item) {
        return Form.builder().id("appointment:room").titleKey("appointment.form.title")
                .questions(Arrays.asList(
                        AppointmentFlow.timeSlotQuestion(item),
                        Question.builder().name("attendees").kind(Question.Kind.TEXT)
                                .titleKey("appointment.q.attendees").required(false).build(),
                        Question.builder().name("note").kind(Question.Kind.TEXT)
                                .titleKey("appointment.q.note").required(false).build()))
                .build();
    }

    @Test
    void collectsAndMapsToRequest() {
        AppointmentItem item = sampleItem();
        AppointmentFlow flow = AppointmentFlow.create(item, meetingTemplate(item));

        CollectionResult r1 = flow.submitAnswer(AppointmentFlow.Q_TIME_SLOT, "s1");
        assertEquals(CollectionResult.Status.COMPLETE, r1.getStatus());

        AppointmentRequest req = flow.toRequest();
        assertNotNull(req);
        assertEquals("room-a", req.getItemId());
        assertEquals("s1", req.getTimeSlotId());
        assertEquals("s1", req.getAnswers().get(AppointmentFlow.Q_TIME_SLOT));
    }

    @Test
    void incompleteReturnsNullRequest() {
        AppointmentItem item = sampleItem();
        AppointmentFlow flow = AppointmentFlow.create(item, meetingTemplate(item));
        assertNull(flow.toRequest());
    }
}
