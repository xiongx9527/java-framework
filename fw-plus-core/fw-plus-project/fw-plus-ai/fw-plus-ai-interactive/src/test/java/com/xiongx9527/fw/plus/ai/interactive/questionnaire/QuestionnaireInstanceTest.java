package com.xiongx9527.fw.plus.ai.interactive.questionnaire;

import com.xiongx9527.fw.plus.ai.interactive.ClarifyOption;
import com.xiongx9527.fw.plus.ai.interactive.InteractiveException;
import com.xiongx9527.fw.plus.ai.interactive.Question;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionnaireInstanceTest {

    private Paper samplePaper() {
        List<ClarifyOption> opts = Arrays.asList(
                ClarifyOption.builder().id("a").label("A").build(),
                ClarifyOption.builder().id("b").label("B").build());
        return Paper.builder().id("sat").titleKey("paper.sat.title").questions(Arrays.asList(
                Question.builder().name("name").kind(Question.Kind.TEXT).titleKey("q.name").required(true).build(),
                Question.builder().name("level").kind(Question.Kind.SINGLE_CHOICE).titleKey("q.level")
                        .required(true).options(opts).build(),
                Question.builder().name("memo").kind(Question.Kind.TEXT).titleKey("q.memo").required(false).build()))
                .build();
    }

    @Test
    void navigatePrevCurrentNextWithAnswers() {
        QuestionnaireInstance inst = QuestionnaireInstance.open(samplePaper());

        AnsweredQuestion c0 = inst.current();
        assertEquals("name", c0.getQuestion().getName());
        assertNull(c0.getAnswer());
        assertTrue(inst.isFirst());

        inst.submitAnswer("张三");
        AnsweredQuestion n1 = inst.next();
        assertNotNull(n1);
        assertEquals("level", n1.getQuestion().getName());
        assertFalse(inst.isLast());

        inst.submitAnswer("a");
        AnsweredQuestion n2 = inst.next();
        assertEquals("memo", n2.getQuestion().getName());
        assertTrue(inst.isLast());

        // 上一题带答案回填
        AnsweredQuestion prev = inst.previous();
        assertEquals("level", prev.getQuestion().getName());
        assertEquals("a", prev.getAnswer());

        // 当前题仍是 level（previous 已回退）
        assertEquals("level", inst.current().getQuestion().getName());
        assertEquals("a", inst.current().getAnswer());
    }

    @Test
    void requiredValidationRejectsBlank() {
        QuestionnaireInstance inst = QuestionnaireInstance.open(samplePaper());
        assertThrows(InteractiveException.class, () -> inst.submitAnswer("  "));
    }

    @Test
    void finishReturnsAllAnswers() {
        QuestionnaireInstance inst = QuestionnaireInstance.open(samplePaper());
        inst.submitAnswer("张三");
        inst.next();
        inst.submitAnswer("b");
        assertTrue(inst.isComplete());

        java.util.Map<String, Object> answers = inst.finish();
        assertEquals("张三", answers.get("name"));
        assertEquals("b", answers.get("level"));
        // memo 未答，不应出现
        assertFalse(answers.containsKey("memo"));
    }

    @Test
    void openByPaperIdViaService() {
        QuestionnaireService svc = new QuestionnaireService();
        svc.register(samplePaper());
        assertTrue(svc.contains("sat"));
        QuestionnaireInstance inst = svc.open("sat");
        assertEquals("sat", inst.getPaperId());

        assertThrows(InteractiveException.class, () -> svc.open("nope"));
    }
}
