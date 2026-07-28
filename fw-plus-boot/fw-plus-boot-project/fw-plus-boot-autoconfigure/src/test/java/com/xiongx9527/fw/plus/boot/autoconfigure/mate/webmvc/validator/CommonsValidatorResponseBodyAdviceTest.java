package com.xiongx9527.fw.plus.boot.autoconfigure.mate.webmvc.validator;

import com.xiongx9527.fw.plus.common.validator.CommonsValidator;
import com.xiongx9527.fw.plus.common.validator.CommonsValidatorHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class CommonsValidatorResponseBodyAdviceTest {

    private final CommonsValidatorResponseBodyAdvice advice = new CommonsValidatorResponseBodyAdvice();

    @AfterEach
    void tearDown() {
        CommonsValidatorHolder.clear();
    }

    @Test
    void supportsMethodLevelResponseBody() throws Exception {
        assertTrue(advice.supports(returnType("methodResponseBody"), null));
    }

    @Test
    void supportsRestControllerMetaAnnotation() throws Exception {
        assertTrue(advice.supports(returnType(RestControllerSample.class, "restControllerMethod"), null));
    }

    @Test
    void beforeBodyWriteClearsContext() throws Exception {
        CommonsValidatorHolder.set(CommonsValidator.FORM_DATA_KEY, "stale");

        Object result = advice.beforeBodyWrite("body", returnType("methodResponseBody"), null, null, null, null);

        assertEquals("body", result);
        assertNull(CommonsValidatorHolder.get(CommonsValidator.FORM_DATA_KEY));
    }

    private MethodParameter returnType(String methodName) throws Exception {
        return returnType(SampleController.class, methodName);
    }

    private MethodParameter returnType(Class<?> controllerType, String methodName) throws Exception {
        Method method = controllerType.getDeclaredMethod(methodName);
        return new MethodParameter(method, -1);
    }

    private static class SampleController {
        @ResponseBody
        String methodResponseBody() {
            return "ok";
        }
    }

    @RestController
    private static class RestControllerSample extends SampleController {
        String restControllerMethod() {
            return "ok";
        }
    }
}
