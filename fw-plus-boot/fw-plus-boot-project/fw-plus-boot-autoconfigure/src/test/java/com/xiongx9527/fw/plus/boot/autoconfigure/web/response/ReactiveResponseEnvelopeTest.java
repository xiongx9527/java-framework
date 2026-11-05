package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.common.exception.BizException;
import com.xiongx9527.fw.plus.common.model.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactiveResponseEnvelopeTest {

    private static ResponseProperties properties() {
        ResponseProperties properties = new ResponseProperties();
        properties.setEnabled(true);
        return properties;
    }

    @Test
    void filterWrapsJsonBody() throws Exception {
        ResponseProperties properties = properties();
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties);
        ReactiveResponseEnvelopeFilter filter =
                new ReactiveResponseEnvelopeFilter(properties, factory, new ObjectMapper());

        JsonNode node = new ObjectMapper().readTree(filter.wrapText("{\"name\":\"a\"}"));

        assertEquals(0, node.get("code").asInt());
        assertEquals("a", node.get("data").get("name").asText());
    }

    @Test
    void reactiveHandlerMapsBizException() {
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties());
        ReactiveGlobalExceptionHandler handler = new ReactiveGlobalExceptionHandler(factory);

        ResponseEntity<JsonResult> response = handler.handleBiz(BizException.biz(101, "库存不足"));

        assertEquals(400, response.getStatusCodeValue());
        assertEquals(40_005_101, response.getBody().get("code"));
    }

    @Test
    void reactiveHandlerHidesInternalMessage() {
        ResponseEnvelopeFactory factory = new ResponseEnvelopeFactory(properties());
        ReactiveGlobalExceptionHandler handler = new ReactiveGlobalExceptionHandler(factory);

        ResponseEntity<JsonResult> response = handler.handleThrowable(new IllegalStateException("secret"));

        assertEquals(500, response.getStatusCodeValue());
        assertTrue("系统繁忙，请稍后重试".equals(response.getBody().get("message")));
    }
}
