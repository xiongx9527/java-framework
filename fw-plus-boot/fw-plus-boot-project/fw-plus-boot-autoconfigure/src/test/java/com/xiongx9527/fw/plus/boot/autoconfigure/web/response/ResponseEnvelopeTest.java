package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import com.xiongx9527.fw.plus.common.exception.BizException;
import com.xiongx9527.fw.plus.common.model.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseEnvelopeTest {

    private static ResponseProperties properties() {
        ResponseProperties properties = new ResponseProperties();
        properties.setEnabled(true);
        return properties;
    }

    private static MethodParameter parameter() throws Exception {
        Method method = ResponseEnvelopeTest.class.getDeclaredMethod("sample");
        return new MethodParameter(method, -1);
    }

    @SuppressWarnings("unused")
    private Map<String, Object> sample() {
        return new LinkedHashMap<String, Object>();
    }

    @Test
    void factoryBuildsSuccessEnvelopeWithConfiguredKeys() {
        ResponseProperties properties = properties();
        properties.setMessageKey("msg");
        properties.setSuccessKey("");
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties);

        JsonResult result = factory.success("payload");

        assertEquals(0, result.get("code"));
        assertEquals("payload", result.get("data"));
        assertTrue(result.containsKey("msg"));
        assertNull(result.get("msg"));
        assertFalse(result.containsKey("success"), "success-key 置空时不应输出 success 字段");
        assertFalse(result.containsKey("message"), "message 应已重命名为 msg");
    }

    @Test
    void factoryKeepsSuccessFlagWhenConfigured() {
        ResponseProperties properties = properties();
        properties.setSuccessMessage("ok");
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties);
        JsonResult result = factory.success(1);
        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals("ok", result.get("message"));
    }

    @Test
    void successMessageDefaultsToNull() {
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties());
        JsonResult result = factory.success(1);
        assertTrue(result.containsKey("message"));
        assertNull(result.get("message"));
    }

    @Test
    void httpStatusDerivedFromCodePrefix() {
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties());
        assertEquals(400, factory.httpStatus(40_001_100));
        assertEquals(500, factory.httpStatus(50_010_100));
        assertEquals(200, factory.httpStatus(0));
    }

    @Test
    void adviceWrapsPlainBody() throws Exception {
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties());
        ResponseEnvelopeAdvice advice = new ResponseEnvelopeAdvice(properties(), factory);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("name", "a");
        Object wrapped = advice.beforeBodyWrite(body, parameter(), MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class, null, null);

        assertTrue(wrapped instanceof JsonResult);
        assertEquals(body, ((JsonResult) wrapped).get("data"));
    }

    @Test
    void adviceSkipsWrappedStringAndNonJson() throws Exception {
        ResponseProperties properties = properties();
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties);
        ResponseEnvelopeAdvice advice = new ResponseEnvelopeAdvice(properties, factory);

        JsonResult already = factory.success("x");
        assertSame(already, advice.beforeBodyWrite(already, parameter(), MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class, null, null));

        assertEquals("text", advice.beforeBodyWrite("text", parameter(), MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class, null, null));

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("k", "v");
        assertSame(body, advice.beforeBodyWrite(body, parameter(), MediaType.TEXT_EVENT_STREAM,
                MappingJackson2HttpMessageConverter.class, null, null));
    }

    @Test
    void adviceDisabledByDefault() throws Exception {
        ResponseProperties properties = new ResponseProperties();
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties);
        ResponseEnvelopeAdvice advice = new ResponseEnvelopeAdvice(properties, factory);
        assertFalse(advice.supports(parameter(), MappingJackson2HttpMessageConverter.class));
    }

    @Test
    void handlerMapsBizExceptionToItsOwnCode() {
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties());
        GlobalExceptionHandler handler = new GlobalExceptionHandler(factory);

        ResponseEntity<JsonResult> response = handler.handleBiz(BizException.biz(101, "库存不足"));

        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(40_005_101, response.getBody().get("code"));
        assertEquals("库存不足", response.getBody().get("message"));
    }

    @Test
    void handlerHidesInternalMessage() {
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties());
        GlobalExceptionHandler handler = new GlobalExceptionHandler(factory);

        ResponseEntity<JsonResult> response = handler.handleThrowable(new IllegalStateException("db password=xxx"));

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("系统繁忙，请稍后重试", response.getBody().get("message"));
    }
}
