package com.xiongx9527.fw.plus.boot.autoconfigure.web.sensitive;

import com.xiongx9527.fw.plus.common.databind.RequestDataTransfer;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldIgnoreSensitive;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: jugui.li
 */
@Slf4j
public abstract class SensitiveAopBase {

    /**
     * 处理控制器方法上的 FieldIgnoreSensitive 注解
     * @param joinPoint
     * @throws Throwable
     */
    protected void doControllerFieldIgnoreSensitive(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 获取控制器类
            Class<?> controllerClass = joinPoint.getTarget().getClass();
            // 获取方法签名
            MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
            // 获取控制器方法
            Method controllerMethod = methodSignature.getMethod();
            // 获取类上的 FieldIgnoreSensitive 注解
            FieldIgnoreSensitive classFieldIgnoreSensitive = (FieldIgnoreSensitive)controllerClass.getAnnotation(FieldIgnoreSensitive.class);
            // 获取方法上的 FieldIgnoreSensitive 注解
            FieldIgnoreSensitive methodFieldIgnoreSensitive = (FieldIgnoreSensitive)controllerMethod.getAnnotation(FieldIgnoreSensitive.class);
            // 输出日志，判断注解是否存在
            log.debug("null != classFieldIgnoreSensitive:{}",(null != classFieldIgnoreSensitive));
            log.debug("null != methodFieldIgnoreSensitive:{}",(null != methodFieldIgnoreSensitive));
            log.debug("null != classFieldIgnoreSensitive || null != methodFieldIgnoreSensitive:{}",(null != classFieldIgnoreSensitive || null != methodFieldIgnoreSensitive));
            // 如果类或方法上有 FieldIgnoreSensitive 注解，则处理忽略字段
            if (null != classFieldIgnoreSensitive || null != methodFieldIgnoreSensitive) {
                Set<String> ignoreFieldSet = new HashSet<>();
                // 处理方法上的忽略字段
                if (null != methodFieldIgnoreSensitive && null != methodFieldIgnoreSensitive.values() && methodFieldIgnoreSensitive.values().length > 0) {
                    log.debug("method ignoreFieldSet:{}",methodFieldIgnoreSensitive.values());
                    ignoreFieldSet.addAll(Arrays.asList(methodFieldIgnoreSensitive.values()));
                }else if (null != classFieldIgnoreSensitive && null != classFieldIgnoreSensitive.values() && classFieldIgnoreSensitive.values().length > 0) {
                    //// 处理类上的忽略字段
                    log.debug("class ignoreFieldSet:{}",classFieldIgnoreSensitive.values());
                    ignoreFieldSet.addAll(Arrays.asList(classFieldIgnoreSensitive.values()));
                }
                // 日志输出所有忽略字段
                log.debug("all ignoreFieldSet:{}",ignoreFieldSet);
                // 根据忽略字段集合决定是否跳过敏感字段处理
                if (CollectionUtils.isEmpty(ignoreFieldSet)) {
                    RequestDataTransfer.skipSensitive();
                } else {
                    String ignoreFieldStr = ignoreFieldSet.stream().collect(Collectors.joining(","));
                    RequestDataTransfer.put(RequestDataTransfer.getSkipSensitiveKey(), ignoreFieldStr);
                }
            }
        } catch (Throwable e) {
            // 异常处理，输出错误日志
            log.error(e.getMessage(),e);
        }
    }
}
