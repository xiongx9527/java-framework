package com.xiongx9527.fw.plus.easytrans.service.util;

import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class MateEasyTransUtilsTest {

    @Test
    void transOneReturnsNullForNullInput() throws Exception {
        Object result = MateEasyTransUtils.transOne(null, new CountingTransService(), false,
                new ArrayList<>(), null, null);

        assertNull(result);
    }

    @Test
    void transBatchReturnsEmptyCollectionAsIs() throws Exception {
        List<Object> source = new ArrayList<>();

        Collection result = MateEasyTransUtils.transBatch(source, new CountingTransService(), false,
                new ArrayList<>(), null, null);

        assertSame(source, result);
        assertTrue(result.isEmpty());
    }

    @Test
    void transBatchSkipsLeadingNullAndTranslatesLaterVo() throws Exception {
        CountingTransService transService = new CountingTransService();
        TestVo vo = new TestVo(1L);
        List<Object> source = new ArrayList<>(Arrays.asList(null, vo));

        Collection result = MateEasyTransUtils.transBatch(source, transService, false,
                new ArrayList<>(), null, null);

        assertSame(source, result);
        assertEquals(1, transService.transOneCount);
        assertSame(vo, transService.lastVo);
    }

    @Test
    void transBatchProxyKeepsDuplicateAndPreviouslyVisitedVoSlots() throws Exception {
        TestVo vo = new TestVo(2L);
        List<Object> source = new ArrayList<>(Arrays.asList(vo, null, vo));
        ArrayList<Object> hasTransObjs = new ArrayList<>();
        hasTransObjs.add(vo);

        Collection result = MateEasyTransUtils.transBatch(source, new CountingTransService(), true,
                hasTransObjs, null, null);

        List resultList = new ArrayList(result);
        assertEquals(3, resultList.size());
        assertSame(vo, resultList.get(0));
        assertNull(resultList.get(1));
        assertSame(vo, resultList.get(2));
    }

    @Test
    void transFieldsTranslatesFinalListWhenFirstElementIsNull() throws Exception {
        CountingTransService transService = new CountingTransService();
        TestVo vo = new TestVo(4L);
        bsFinalListHolder holder = new bsFinalListHolder(null, vo);

        MateEasyTransUtils.transFields(holder, transService, false,
                new ArrayList<>(), null, null);

        assertEquals(1, transService.transOneCount);
        assertSame(vo, transService.lastVo);
    }

    @Test
    void transOneDoesNotRecurseForeverOnCycle() throws Exception {
        CountingTransService transService = new CountingTransService();
        Node node = new Node();
        node.self = node;

        Object result = MateEasyTransUtils.transOne(node, transService, false,
                new ArrayList<>(), null, null);

        assertSame(node, result);
    }

    @Test
    void transOneAllowsNullVisitedList() throws Exception {
        Node node = new Node();
        node.self = node;

        Object result = assertDoesNotThrow(() -> MateEasyTransUtils.transOne(node, new CountingTransService(), false,
                null, null, null));

        assertSame(node, result);
    }

    @Test
    void createProxyVoReturnsOriginalVoWhenProxyCannotBeCreated() {
        TestVo vo = new TestVo(2L);
        vo.getTransMap().put("dynamicField", "value");

        Object result = MateEasyTransUtils.createProxyVo(vo);

        assertTrue(result instanceof VO);
    }

    @Test
    void createProxyVoReturnsOriginalVoWithoutAssignmentErrorWhenGeneratedClassIsNull(CapturedOutput output) throws Exception {
        TestVo vo = new TestVo(3L);
        vo.getTransMap().put("dynamicField", "value");
        proxyClassMap().put(TestVo.class, TestVo.class);
        proxyClassFieldMap().put(TestVo.class, new java.util.LinkedHashSet<>(Arrays.asList("otherField")));

        Object result = MateEasyTransUtils.createProxyVo(vo);

        assertSame(vo, result);
        assertTrue(output.getOut().contains("生成新class错误"));
        assertTrue(!output.getOut().contains("easy trans 赋值错误"));
    }

    @Test
    void booleanAndEnumServicesRemainInstantiable() {
        assertTrue(new com.xiongx9527.fw.plus.easytrans.service.MateEasyTransBooleanTransService()
                .isValid(Boolean.class, "", Boolean.TRUE));
        assertTrue(new com.xiongx9527.fw.plus.easytrans.service.MateEasyTransEnumTransService()
                .isValid(SampleEnum.class, "code", "A"));
    }

    static class CountingTransService extends TransService {
        int transOneCount;
        int transMoreCount;
        VO lastVo;

        @Override
        public void transOne(VO obj, Set<String> includeFields, Set<String> excludeFields) {
            transOneCount++;
            lastVo = obj;
            obj.getTransMap().put("translated", "yes");
        }

        @Override
        public void transMore(List<? extends VO> objList, Set<String> includeFields, Set<String> excludeFields) {
            transMoreCount++;
            if (objList.isEmpty() || objList.get(0) == null) {
                return;
            }
            for (VO vo : objList) {
                if (vo != null) {
                    transOne(vo, includeFields, excludeFields);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Class, Class> proxyClassMap() throws Exception {
        Field field = MateEasyTransUtils.class.getDeclaredField("proxyClassMap");
        field.setAccessible(true);
        return (Map<Class, Class>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<Class, Set<String>> proxyClassFieldMap() throws Exception {
        Field field = MateEasyTransUtils.class.getDeclaredField("proxyClassFieldMap");
        field.setAccessible(true);
        return (Map<Class, Set<String>>) field.get(null);
    }

    static class TestVo implements VO {
        private final Long id;
        private final Map<String, Object> transMap = new java.util.LinkedHashMap<>();

        TestVo(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        @Override
        public Map<String, Object> getTransMap() {
            return transMap;
        }
    }

    static class bsFinalListHolder {
        final List<Object> vos;

        bsFinalListHolder(Object... vos) {
            this.vos = new ArrayList<>(Arrays.asList(vos));
        }
    }

    static class Node {
        Node self;
    }

    enum SampleEnum {
        A("A", "启用");

        private final String code;
        private final String label;

        SampleEnum(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }
}
