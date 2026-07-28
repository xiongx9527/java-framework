package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor;

import com.baomidou.dynamic.datasource.processor.DsProcessor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author TaoYu
 * @since 2.5.0
 */
public class DsHeaderProcessor extends DsProcessor {

    /**
     * header prefix
     */
    private static final String HEADER_PREFIX = "#header";

    /**
     * 判断给定的键是否匹配当前数据源路由规则
     *
     * @param key 需要匹配的键值
     * @return 如果键以HEADER_PREFIX开头则返回true，否则返回false
     */
    @Override
    public boolean matches(String key) {
        return StringUtils.startsWith(key, HEADER_PREFIX);
    }

    /**
     * 根据方法调用和键值确定具体的数据源
     *
     * @param invocation 方法调用对象，包含当前执行的方法信息
     * @param key 键值，用于从HTTP请求头中获取对应的数据源标识
     * @return 从HTTP请求头中获取到的数据源名称
     */
    @Override
    public String doDetermineDatasource(MethodInvocation invocation, String key) {
        String headerName = StringUtils.substringAfter(key, HEADER_PREFIX + ".");
        if (StringUtils.isBlank(headerName)) {
            return null;
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        return request.getHeader(headerName);
    }

}
