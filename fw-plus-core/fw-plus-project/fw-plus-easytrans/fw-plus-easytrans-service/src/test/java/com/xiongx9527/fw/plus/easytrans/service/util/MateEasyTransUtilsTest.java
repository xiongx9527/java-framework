package com.xiongx9527.fw.plus.easytrans.service.util;

import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import org.junit.jupiter.api.Test;

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
    void transOneDoesNotRecurseForeverOnCycle() throws Exception {
        CountingTransService transService = new CountingTransService();
        Node node = new Node();
        node.self = node;

        Object result = MateEasyTransUtils.transOne(node, transService, false,
                new ArrayList<>(), null, null);

        assertSame(node, result);
    }

    @Test
    void transOneTracksCycleWhenHistoryIsNull() throws Exception {
        CountingTransService transService = new CountingTransService();
        Node node = new Node();
        node.self = node;

        Object result = assertDoesNotThrow(() -> MateEasyTransUtils.transOne(node, transService, false,
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
            for (VO vo : objList) {
                if (vo != null) {
                    transOne(vo, includeFields, excludeFields);
                }
            }
        }
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

    static class Node {
        Node self;
    }
}
