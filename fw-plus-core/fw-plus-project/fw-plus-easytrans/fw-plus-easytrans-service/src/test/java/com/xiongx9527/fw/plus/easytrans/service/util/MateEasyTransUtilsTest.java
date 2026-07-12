package com.xiongx9527.fw.plus.easytrans.service.util;

import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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
    void transBatchPassesOnlyNonNullVosToTransMore() throws Exception {
        StrictTransService transService = new StrictTransService();
        TestVo vo = new TestVo(1L);
        List<Object> source = new ArrayList<>(Arrays.asList(null, vo));

        Collection result = MateEasyTransUtils.transBatch(source, transService, false,
                new ArrayList<>(), null, null);

        assertSame(source, result);
        assertEquals(1, transService.transMoreCount);
        assertEquals(1, transService.transOneCount);
        assertSame(vo, transService.lastVo);
    }

    @Test
    void transFieldsTranslatesFinalListWhenFirstItemIsNull() throws Exception {
        CountingTransService transService = new CountingTransService();
        TestVo vo = new TestVo(1L);
        bsHolder holder = new bsHolder(new ArrayList<>(Arrays.asList(null, vo)));

        MateEasyTransUtils.transOne(holder, transService, false, new ArrayList<>(), null, null);

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
    void createProxyVoReturnsOriginalVoWithoutAssignmentErrorWhenProxyClassCannotBeCreated() {
        FinalVo vo = new FinalVo();
        vo.getTransMap().put("dynamicField", "value");
        Logger logger = (Logger) LoggerFactory.getLogger(MateEasyTransUtils.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            Object result = MateEasyTransUtils.createProxyVo(vo);

            assertSame(vo, result);
            assertTrue(appender.list.stream().noneMatch(event ->
                    event.getLevel().isGreaterOrEqual(Level.ERROR)
                            && event.getFormattedMessage().contains("easy trans 赋值错误")));
        } finally {
            logger.detachAppender(appender);
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
            for (VO vo : objList) {
                if (vo != null) {
                    transOne(vo, includeFields, excludeFields);
                }
            }
        }
    }

    static class StrictTransService extends CountingTransService {
        @Override
        public void transMore(List<? extends VO> objList, Set<String> includeFields, Set<String> excludeFields) {
            for (VO vo : objList) {
                if (vo == null) {
                    throw new NullPointerException("transMore received null VO");
                }
            }
            super.transMore(objList, includeFields, excludeFields);
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

    static class bsHolder {
        private final List<Object> list;

        bsHolder(List<Object> list) {
            this.list = list;
        }
    }

    static final class FinalVo implements VO {
        private final Map<String, Object> transMap = new java.util.LinkedHashMap<>();

        @Override
        public Map<String, Object> getTransMap() {
            return transMap;
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
