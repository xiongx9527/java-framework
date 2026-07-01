package com.xx9527.fw.plus.easytrans.service;

import com.fhs.core.trans.anno.IgnoreTrans;
import com.fhs.core.trans.anno.TransMethodResult;
import com.fhs.core.trans.anno.TransSett;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.advice.EasyTransResponseBodyAdvice;
import com.fhs.trans.service.impl.TransService;
import com.fhs.trans.utils.TransUtil;
import com.xx9527.fw.plus.easytrans.service.util.MateEasyTransUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.*;

@Slf4j
@ControllerAdvice
//@RestControllerAdvice
@ConditionalOnProperty(
        name = {"fw.plus.mate.easy-trans.is-enable-global"},
        havingValue = "true"
)
//RequestBodyAdvice
//HandlerMethodArgumentResolver
public class MateEasyTransResponseBodyAdvice extends EasyTransResponseBodyAdvice implements ResponseBodyAdvice {

    /**
     * 是否启用瓦片功能的配置属性
     * 从配置文件中读取fw.plus.mate.easy-trans.is-enable-tile属性值，默认为true
     */
    @Value("${fw.plus.mate.easy-trans.is-enable-tile:true}")
    private Boolean isEnableTile;

    /**
     * 翻译服务实例
     * 用于执行具体的翻译业务逻辑
     */
    private final TransService transService;

    /**
     * 构造函数，初始化MateEasyTransResponseBodyAdvice实例
     *
     * @param transService 翻译服务实例，用于处理翻译相关业务逻辑
     */
    public MateEasyTransResponseBodyAdvice(TransService transService) {
        this.transService = transService;
    }


    /**
     * Determines if a given {@link MethodParameter} and target class are supported.
     * <p>
     * This method always returns {@code true}, indicating that any {@link MethodParameter} and target class are unconditionally supported.
     * This behavior may be useful in scenarios where support for all parameter types and target classes is desired, but specific processing logic may vary.
     *
     * @param methodParameter The method parameter to be evaluated, wrapped in a {@link MethodParameter} object.
     * @param aClass          The target class type, represented by a {@link Class} object.
     * @return {@code true} always, indicating support for any parameter and target class.
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        return methodParameter.getExecutable().isAnnotationPresent(TransMethodResult.class);
    }

    /**
     * 在写入响应体之前对对象进行处理
     *
     * @param o                  要写入响应体的对象
     * @param methodParameter    方法参数信息
     * @param mediaType          媒体类型
     * @param aClass             对象的类
     * @param serverHttpRequest  服务器HTTP请求
     * @param serverHttpResponse 服务器HTTP响应
     * @return 返回处理后的对象
     */
    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        // 检查方法是否标记为忽略转换
        if (methodParameter.getExecutable().isAnnotationPresent(IgnoreTrans.class)) {
            return o;
        } else {
            Set<String> includeFields = null;
            Set<String> excludeFields = null;
            // 检查方法是否包含转换集注解
            if (methodParameter.getExecutable().isAnnotationPresent(TransSett.class)) {
                TransSett transSett = (TransSett) methodParameter.getExecutable().getAnnotation(TransSett.class);
                // 根据注解配置获取包含或排除字段
                if (transSett.include().length != 0) {
                    includeFields = new HashSet(Arrays.asList(transSett.include()));
                } else {
                    excludeFields = new HashSet(Arrays.asList(transSett.exclude()));
                }
            }
            Object result = null;
            try {
                // 调用工具类进行对象转换
                result = MateEasyTransUtils.transOne(o, this.transService, this.isEnableTile, new ArrayList(), includeFields, excludeFields);
            } catch (Throwable e) {
                log.error("翻译错误", e);
            }
            // 返回转换后的对象，如果转换失败则返回原始对象
            return result == null ? o : result;
        }
    }
}