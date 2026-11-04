package com.xiongx9527.fw.plus.ai.interactive.tool;

import com.xiongx9527.fw.plus.ai.interactive.AnswerExtractor;
import com.xiongx9527.fw.plus.ai.interactive.ClarifyOption;
import com.xiongx9527.fw.plus.ai.interactive.CollectionResult;
import com.xiongx9527.fw.plus.ai.interactive.Form;
import com.xiongx9527.fw.plus.ai.interactive.FormCollector;
import com.xiongx9527.fw.plus.ai.interactive.Question;
import com.xiongx9527.fw.plus.ai.interactive.appointment.AppointmentFlow;
import com.xiongx9527.fw.plus.ai.interactive.appointment.AppointmentItem;
import com.xiongx9527.fw.plus.ai.interactive.appointment.AppointmentRequest;
import com.xiongx9527.fw.plus.ai.interactive.questionnaire.AnsweredQuestion;
import com.xiongx9527.fw.plus.ai.interactive.questionnaire.Paper;
import com.xiongx9527.fw.plus.ai.interactive.questionnaire.QuestionnaireInstance;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractiveToolContractTest {

    // ---- 业务侧内联实现：证明接口契约可被组合引擎实现 ----

    static final class InlineClarifyTool implements ClarifyTool {
        private final Map<String, FormCollector> sessions = new ConcurrentHashMap<String, FormCollector>();
        @Override public ClarifySession start(Form form) {
            String id = UUID.randomUUID().toString();
            FormCollector c = new FormCollector(form);
            sessions.put(id, c);
            return ClarifySession.builder().sessionId(id).result(c.proceed()).build();
        }
        @Override public CollectionResult answer(String sid, String name, Object v) {
            return sessions.get(sid).submitAnswer(name, v);
        }
        @Override public CollectionResult answer(String sid, String u, AnswerExtractor e) {
            return sessions.get(sid).submitUtterance(u, e);
        }
        @Override public CollectionResult state(String sid) {
            return sessions.get(sid).proceed();
        }
        @Override public boolean isComplete(String sid) {
            return sessions.get(sid).isComplete();
        }
        @Override public Map<String, Object> finish(String sid) {
            return sessions.get(sid).getAnswers();
        }
    }

    static final class InlineQuestionnaireTool implements QuestionnaireTool {
        private final Map<String, Paper> papers = new ConcurrentHashMap<String, Paper>();
        private final Map<String, QuestionnaireInstance> instances = new ConcurrentHashMap<String, QuestionnaireInstance>();
        void register(Paper p) { papers.put(p.getId(), p); }
        @Override public QuestionnaireSession open(String paperId) {
            QuestionnaireInstance inst = QuestionnaireInstance.open(papers.get(paperId));
            String id = UUID.randomUUID().toString();
            instances.put(id, inst);
            return QuestionnaireSession.builder().instanceId(id).current(inst.current()).build();
        }
        @Override public AnsweredQuestion current(String id) { return instances.get(id).current(); }
        @Override public AnsweredQuestion previous(String id) { return instances.get(id).previous(); }
        @Override public AnsweredQuestion next(String id) { return instances.get(id).next(); }
        @Override public void submit(String id, Object v) { instances.get(id).submitAnswer(v); }
        @Override public void submit(String id, String u, AnswerExtractor e) { instances.get(id).submitAnswer(u, e); }
        @Override public boolean isComplete(String id) { return instances.get(id).isComplete(); }
        @Override public Map<String, Object> finish(String id) { return instances.get(id).finish(); }
    }

    static final class InlineAppointmentTool implements AppointmentTool {
        private final Map<String, AppointmentItem> items = new ConcurrentHashMap<String, AppointmentItem>();
        private final Map<String, AppointmentFlow> flows = new ConcurrentHashMap<String, AppointmentFlow>();
        void register(AppointmentItem it, Form template) {
            items.put(it.getId(), it);
            // 模板与项目绑定：这里用同一个 template；业务可按项目解析不同模板
            flows.put(it.getId() + ":tpl", AppointmentFlow.create(it, template));
        }
        @Override public AppointmentSession open(String itemId) {
            AppointmentFlow flow = flows.get(itemId + ":tpl");
            String sid = UUID.randomUUID().toString();
            flows.put(sid, flow);
            return AppointmentSession.builder().sessionId(sid).result(flow.proceed()).build();
        }
        @Override public CollectionResult answer(String sid, String name, Object v) { return flows.get(sid).submitAnswer(name, v); }
        @Override public CollectionResult answer(String sid, String u, AnswerExtractor e) { return flows.get(sid).submitUtterance(u, e); }
        @Override public CollectionResult state(String sid) { return flows.get(sid).proceed(); }
        @Override public boolean isComplete(String sid) { return flows.get(sid).isComplete(); }
        @Override public AppointmentRequest finish(String sid) { return flows.get(sid).toRequest(); }
    }

    // ---- 测试 ----

    @Test
    void clarifyToolCollects() {
        Form form = Form.builder().id("c").titleKey("t").questions(Arrays.asList(
                Question.builder().name("x").titleKey("qx").required(true).build())).build();
        InlineClarifyTool t = new InlineClarifyTool();
        ClarifySession s = t.start(form);
        assertNotNull(s.getSessionId());
        t.answer(s.getSessionId(), "x", "hi");
        assertTrue(t.isComplete(s.getSessionId()));
        assertEquals("hi", t.finish(s.getSessionId()).get("x"));
    }

    @Test
    void questionnaireToolNavigates() {
        Paper paper = Paper.builder().id("p1").titleKey("t").questions(Arrays.asList(
                Question.builder().name("a").required(true).build(),
                Question.builder().name("b").required(true).build())).build();
        InlineQuestionnaireTool t = new InlineQuestionnaireTool();
        t.register(paper);
        QuestionnaireSession s = t.open("p1");
        assertNotNull(s.getInstanceId());
        assertEquals("a", s.getCurrent().getQuestion().getName());
        t.submit(s.getInstanceId(), "1");
        AnsweredQuestion n = t.next(s.getInstanceId());
        assertEquals("b", n.getQuestion().getName());
        t.submit(s.getInstanceId(), "2");
        assertTrue(t.isComplete(s.getInstanceId()));
        assertEquals("2", t.finish(s.getInstanceId()).get("b"));
    }

    @Test
    void appointmentToolCollectsToRequest() {
        List<ClarifyOption> slots = Arrays.asList(
                ClarifyOption.builder().id("s1").label("11-10 09:00").build());
        AppointmentItem item = AppointmentItem.builder().id("room-a").name("会议室A").slots(slots).build();
        Form tpl = Form.builder().id("ap").titleKey("t").questions(Arrays.asList(
                AppointmentFlow.timeSlotQuestion(item),
                Question.builder().name("note").kind(Question.Kind.TEXT).required(false).build())).build();
        InlineAppointmentTool t = new InlineAppointmentTool();
        t.register(item, tpl);
        AppointmentSession s = t.open("room-a");
        assertNotNull(s.getSessionId());
        t.answer(s.getSessionId(), AppointmentFlow.Q_TIME_SLOT, "s1");
        assertTrue(t.isComplete(s.getSessionId()));
        AppointmentRequest req = t.finish(s.getSessionId());
        assertEquals("room-a", req.getItemId());
        assertEquals("s1", req.getTimeSlotId());
    }
}
