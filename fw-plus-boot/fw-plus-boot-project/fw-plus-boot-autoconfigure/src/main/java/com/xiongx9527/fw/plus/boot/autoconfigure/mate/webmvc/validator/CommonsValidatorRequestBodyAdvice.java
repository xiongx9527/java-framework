package com.xiongx9527.fw.plus.boot.autoconfigure.mate.webmvc.validator;

import com.xiongx9527.fw.plus.common.validator.CommonsValidator;
import com.xiongx9527.fw.plus.common.validator.CommonsValidatorHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Type;

@Slf4j
@ControllerAdvice
public class CommonsValidatorRequestBodyAdvice implements RequestBodyAdvice {
    /**
     * 判断是否支持给定的方法参数、目标类型和转换器类型
     *
     * @param methodParameter 方法参数信息，用于检查参数注解
     * @param targetType 目标类型，未直接用于支持判断
     * @param converterType 转换器类型，未直接用于支持判断
     * @return 如果方法参数具有 @RequestBody 注解，并且具有 @Valid 或 @Validated 注解，则返回 true
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        /**
        // 检查方法参数是否有 @RequestBody 注解
        return methodParameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestBody.class)
                // 检查方法参数是否有 @Valid 或 @Validated 注解，以确定是否需要进行数据验证
                && (
                    methodParameter.hasParameterAnnotation(javax.validation.Valid.class) ||
                            methodParameter.hasParameterAnnotation(org.springframework.validation.annotation.Validated.class)
                );
         **/
        return methodParameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestBody.class);
    }

    /**
     * 在请求体读取前进行处理
     *
     * 此方法的目的是在实际读取请求体内容之前，对输入消息进行预处理或检查
     * 它允许开发者在请求体被解析前，访问和修改输入消息，以便于实现自定义的逻辑
     *
     * @param inputMessage 输入消息，包含了请求的所有信息，如请求头和请求体
     * @param parameter 方法参数，表示当前处理的控制器方法的参数
     * @param targetType 目标类型，表示请求体应被转换为的类型
     * @param converterType 转换器类型，表示将用于解析请求体的HttpMessageConverter的类型
     * @return 返回处理后的输入消息，本例中直接返回原始输入消息不做任何处理
     * @throws IOException 如果在处理输入消息时发生I/O错误
     */
    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return inputMessage;
    }

    /**
     * 在读取请求体之后进行一些处理
     * 例如，日志记录、数据转换等
     *
     * @param body 请求体的内容，可以是任意类型
     * @param inputMessage Http输入消息，包含请求的元数据
     * @param parameter 方法参数，表示处理后的请求体应绑定到的方法参数
     * @param targetType 目标类型，表示处理后的请求体应转换为的类型
     * @param converterType 转换器类型，表示用于转换请求体的HttpMessageConverter的类型
     * @return 处理后的请求体内容
     */
    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (ObjectUtils.isNotEmpty(body)) {
            CommonsValidatorHolder.set(CommonsValidator.FORM_DATA_KEY, body);
            CommonsValidatorHolder.set(CommonsValidator.METHOD_PARAMETER_KEY, parameter);
        }
        return body;
    }

    /**
     * 处理空请求体的情况
     * 当请求体为空时，此方法将被调用，允许开发者对空请求体进行特殊处理
     *
     * @param body 请求体内容，如果为空，则为null
     * @param inputMessage Http输入消息，包含请求的详细信息
     * @param parameter 方法参数，表示当前处理的参数信息
     * @param targetType 目标类型，表示参数的类型
     * @param converterType 转换器类型，表示用于转换请求体到参数的HttpMessageConverter的类型
     * @return Object 返回处理后的请求体，可以是任何类型，具体取决于实现
     */
    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }
}