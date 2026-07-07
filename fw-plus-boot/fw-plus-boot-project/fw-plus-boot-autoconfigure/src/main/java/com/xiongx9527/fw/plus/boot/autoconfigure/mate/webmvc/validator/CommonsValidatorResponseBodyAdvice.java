package com.xiongx9527.fw.plus.boot.autoconfigure.mate.webmvc.validator;

import com.xiongx9527.fw.plus.common.validator.CommonsValidatorHolder;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;

@ControllerAdvice
public class CommonsValidatorResponseBodyAdvice implements ResponseBodyAdvice {
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        Method method = returnType.getMethod();
        return method != null && (AnnotatedElementUtils.hasAnnotation(method, ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class));
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        CommonsValidatorHolder.clear();
        return body;
    }
}
