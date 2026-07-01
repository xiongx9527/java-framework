package com.xx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor;

import com.baomidou.dynamic.datasource.processor.DsProcessor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author TaoYu
 * @since 2.5.0
 */
public class DsSessionProcessor extends DsProcessor {

    /**
     * session开头
     */
    private static final String SESSION_PREFIX = "#session";

    /**
     * 判断给定的键是否匹配会话前缀规则
     *
     * @param key 需要匹配的键字符串
     * @return 如果键以SESSION_PREFIX开头则返回true，否则返回false
     */
    @Override
    public boolean matches(String key) {
        return key.startsWith(SESSION_PREFIX);
    }

    /**
     * 根据方法调用和键确定数据源
     *
     * @param invocation 方法调用对象，包含当前执行的方法信息
     * @param key 用于确定数据源的键值
     * @return 从会话中获取的数据源标识符字符串
     */
    @Override
    public String doDetermineDatasource(MethodInvocation invocation, String key) {
        // 获取当前HTTP请求对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        // 从会话中获取属性值，键为去掉前缀后的字符串
        return request.getSession().getAttribute(key.substring(9)).toString();
    }

}