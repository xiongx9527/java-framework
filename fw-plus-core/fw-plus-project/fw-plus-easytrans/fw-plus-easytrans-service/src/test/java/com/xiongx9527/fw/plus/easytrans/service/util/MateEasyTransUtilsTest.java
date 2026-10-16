package com.xiongx9527.fw.plus.easytrans.service.util;

import org.dromara.core.trans.vo.VO;
import org.dromara.trans.service.impl.TransService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void transFieldsKeepsFinalListSlotsWhenTranslationReturnsOriginalList() throws Exception {
        CountingTransService transService = new CountingTransService();
        TestVo vo = new TestVo(5L);
        bsFinalListHolder holder = new bsFinalListHolder(null, vo);

        MateEasyTransUtils.transFields(holder, transService, false,
                new ArrayList<>(), null, null);

        assertEquals(2, holder.vos.size());
        assertNull(holder.vos.get(0));
        assertSame(vo, holder.vos.get(1));
        assertEquals("yes", vo.getTransMap().get("translated"));
    }

    @Test
    void transBatchSkipsNonVoItemsInMixedVoCollection() throws Exception {
        CountingTransService transService = new CountingTransService();
        TestVo vo = new TestVo(6L);
        List<Object> source = new ArrayList<>(Arrays.asList(null, vo, "x"));

        Collection result = assertDoesNotThrow(() -> MateEasyTransUtils.transBatch(source, transService, false,
                new ArrayList<>(), null, null));

        assertSame(source, result);
        assertEquals(3, source.size());
        assertNull(source.get(0));
        assertSame(vo, source.get(1));
        assertEquals("x", source.get(2));
        assertEquals("yes", vo.getTransMap().get("translated"));
    }

    @Test
    void transBatchProxyKeepsNonVoItemsInMixedVoCollection() throws Exception {
        CountingTransService transService = new CountingTransService();
        TestVo vo = new TestVo(9L);
        List<Object> source = new ArrayList<>(Arrays.asList(vo, "x"));

        Collection result = assertDoesNotThrow(() -> MateEasyTransUtils.transBatch(source, transService, true,
                new ArrayList<>(), null, null));

        List resultList = new ArrayList(result);
        assertEquals(2, resultList.size());
        assertTrue(resultList.get(0) instanceof VO);
        assertEquals("x", resultList.get(1));
        assertEquals("yes", vo.getTransMap().get("translated"));
    }

    @Test
    void transBatchAddsVoRootsToVisitedContextBeforeRecursingFields() throws Exception {
        CountingTransService transService = new CountingTransService();
        TestVo first = new TestVo(7L);
        TestVo second = new TestVo(8L);
        BatchRoot root = new BatchRoot(Arrays.asList(first, second));
        first.other = second;
        second.other = first;

        MateEasyTransUtils.transFields(root, transService, false,
                new ArrayList<>(), null, null);

        assertEquals(1, transService.transMoreCount);
        assertEquals(2, transService.transOneCount);
        assertSame(second, first.other);
        assertSame(first, second.other);
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
    void genNewClassUsesOriginalPackageForPackagePrivateVo() throws Exception {
        TestVo vo = new TestVo(10L);
        vo.getTransMap().put("dynamicField", "value");
        Map<Class, Class> proxyClassMap = proxyClassMap();
        Map<Class, Set<String>> proxyClassFieldMap = proxyClassFieldMap();
        Class originalProxyClass = proxyClassMap.get(TestVo.class);
        Set<String> originalProxyFields = proxyClassFieldMap.get(TestVo.class);
        Class generatedClass = null;
        try {
            proxyClassMap.remove(TestVo.class);

            generatedClass = MateEasyTransUtils.genNewClass(vo);

            assertNotNull(generatedClass);
            assertEquals(TestVo.class.getPackage().getName(), generatedClass.getPackage().getName());
            assertTrue(TestVo.class.isAssignableFrom(generatedClass));
            assertTrue(proxyClassFieldMap.get(generatedClass).contains("dynamicField"));
        } finally {
            if (generatedClass != null) {
                proxyClassFieldMap.remove(generatedClass);
            }
            if (originalProxyClass == null) {
                proxyClassMap.remove(TestVo.class);
            } else {
                proxyClassMap.put(TestVo.class, originalProxyClass);
            }
            if (originalProxyFields == null) {
                proxyClassFieldMap.remove(TestVo.class);
            } else {
                proxyClassFieldMap.put(TestVo.class, originalProxyFields);
            }
        }
    }

    @Test
    void createProxyVoReturnsOriginalVoWithoutAssignmentErrorWhenGeneratedClassIsNull(CapturedOutput output) throws Exception {
        FinalVo vo = new FinalVo();
        vo.getTransMap().put("dynamicField", "value");
        Map<Class, Class> proxyClassMap = proxyClassMap();
        Map<Class, Set<String>> proxyClassFieldMap = proxyClassFieldMap();
        Class originalProxyClass = proxyClassMap.get(FinalVo.class);
        Set<String> originalProxyFields = proxyClassFieldMap.get(FinalVo.class);
        try {
            proxyClassMap.put(FinalVo.class, FinalVo.class);
            proxyClassFieldMap.put(FinalVo.class, new java.util.LinkedHashSet<>(Arrays.asList("otherField")));

            Object result = MateEasyTransUtils.createProxyVo(vo);

            assertSame(vo, result);
            String outputText = output.getOut() + output.getErr();
            assertTrue(outputText.contains("Cannot subclass primitive, array or final types"));
            assertTrue(!outputText.contains("easy trans 赋值错误"));
        } finally {
            if (originalProxyClass == null) {
                proxyClassMap.remove(FinalVo.class);
            } else {
                proxyClassMap.put(FinalVo.class, originalProxyClass);
            }
            if (originalProxyFields == null) {
                proxyClassFieldMap.remove(FinalVo.class);
            } else {
                proxyClassFieldMap.put(FinalVo.class, originalProxyFields);
            }
        }
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
        TestVo other;

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

    static final class FinalVo implements VO {
        private final Map<String, Object> transMap = new java.util.LinkedHashMap<>();

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

    static class BatchRoot {
        List<TestVo> vos;

        BatchRoot(List<TestVo> vos) {
            this.vos = vos;
        }
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
