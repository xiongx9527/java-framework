package com.xx9527.fw.plus.easytrans.service;

import com.alibaba.fastjson2.JSON;
import com.fhs.core.trans.anno.TransSett;
import com.fhs.trans.aop.TransMethodResultAop;
import com.fhs.trans.service.impl.TransService;
import com.xx9527.fw.plus.easytrans.service.util.MateEasyTransUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * MateEasyTransTransMethodArgAop 类
 *
 * 这是一个继承自 TransMethodArgAop 的服务类，用于处理传输方法参数相关的业务逻辑。
 *
 * 继承关系：
 * - 父类：TransMethodArgAop
 *
 * 主要功能：
 * - 提供传输方法参数的基础实现
 * - 可以扩展父类的功能来满足特定的业务需求
 */
@Aspect
@Slf4j
public class MateEasyTransTransMethodArgAop {
    /**
     * 构造函数，初始化TransService对象
     */
    private final TransService transService;

    /**
     * 构造函数，初始化TransService对象
     * @param transService
     */
    public MateEasyTransTransMethodArgAop(TransService transService) {
        this.transService = transService;
    }


    /**
     * 将单个对象的指定字段进行翻译
     * 此方法用于处理用户行为日志中的参数翻译，根据方法上的注解确定翻译的字段
     *
     * @param joinPoint 切入点对象，用于获取方法参数和注解信息
     * @return 翻译后的参数数组
     */
    private Object[] transOne(JoinPoint joinPoint) {
        //获取方法参数
        Object[] args = joinPoint.getArgs();
        //如果参数不为空，进行翻译处理
        if (ArrayUtils.isNotEmpty(args)) {
            try {
                //用于存储需要翻译的字段列表
                Set<String> includeFields = null;
                //用于存储不需要翻译的字段列表
                Set<String> excludeFields = null;
                //1.获取用户行为日志(ip,username,operation,method,params,time,createdTime)
                //获取类的字节码对象，通过字节码对象获取方法信息
                Class<?> targetCls = joinPoint.getTarget().getClass();
                //获取方法签名(通过此签名获取目标方法信息)
                MethodSignature ms = (MethodSignature) joinPoint.getSignature();
                //获取目标方法上的注解指定的操作名称
                Method targetMethod =
                        targetCls.getDeclaredMethod(
                                ms.getName(),
                                ms.getParameterTypes());
                //检查方法上是否有TransSett注解，用于指定翻译的字段
                if (targetMethod.isAnnotationPresent(TransSett.class)) {
                    TransSett transSett = targetMethod.getAnnotation(TransSett.class);
                    //根据注解配置，确定需要翻译的字段列表或不需要翻译的字段列表
                    if (transSett.include().length != 0) {
                        includeFields = new HashSet<>(Arrays.asList(transSett.include()));
                    }else{
                        excludeFields = new HashSet<>(Arrays.asList(transSett.exclude()));
                    }
                }
                //如果参数不为空，对每个参数进行翻译处理
                if (ArrayUtils.isNotEmpty(args)) {
                    for (Object arg : args) {
                        try {
                            //调用MateEasyTransUtils的transOne方法进行翻译
                            MateEasyTransUtils.transOne(arg, transService, false, new ArrayList<>(),includeFields,excludeFields);
                        } catch (Throwable e) {
                            //记录翻译过程中的错误日志
                            log.error("翻译错误", e);
                        }
                    }
                }
            } catch (Throwable e) {
                //记录翻译过程中的错误日志
                log.error("翻译错误", e);
            }
        }
        //返回翻译后的参数数组
        return args;
    }

    // 使用AOP切面，拦截标注了@TransMethodArg注解的方法，并且该方法所属的类标注了@RestController或@Controller注解
    @Around("( @annotation(com.xx9527.fw.plus.easytrans.annotation.TransMethodArg) && ( @target(org.springframework.web.bind.annotation.RestController) || @target(org.springframework.stereotype.Controller))  )")
    public Object transArgAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 调用transOne方法对参数进行转换
        Object[] args = transOne(joinPoint);
        // 记录转换后的参数日志
        log.info("transArgAround:{}", JSON.toJSONString(args));
        // 继续执行原始方法，并传入转换后的参数
        return ArrayUtils.isNotEmpty(args)?joinPoint.proceed(args):joinPoint.proceed();
    }

}
