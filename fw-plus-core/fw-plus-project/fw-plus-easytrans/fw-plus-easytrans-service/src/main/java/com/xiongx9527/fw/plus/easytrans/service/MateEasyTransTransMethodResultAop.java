package com.xiongx9527.fw.plus.easytrans.service;

import com.fhs.core.trans.anno.TransSett;
import com.fhs.trans.aop.TransMethodResultAop;
import com.fhs.trans.service.impl.TransService;
import com.fhs.trans.utils.TransUtil;
import com.xiongx9527.fw.plus.easytrans.service.util.MateEasyTransUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * MateEasyTransTransMethodResultAop 类
 *
 * 这是一个继承自 TransMethodResultAop 的服务类，用于处理传输方法结果相关的业务逻辑。
 *
 * 继承关系：
 * - 父类：TransMethodResultAop
 *
 * 主要功能：
 * - 提供传输方法结果的基础实现
 * - 可以扩展父类的功能来满足特定的业务需求
 */
@Aspect
@Slf4j
public class MateEasyTransTransMethodResultAop {//extends TransMethodResultAop  {

    @Value("${fw.plus.mate.easy-trans.is-enable-tile:true}")
    private Boolean isEnableTile;

    /**
     * 构造函数，初始化TransService对象
     */
    private final TransService transService;

    /**
     * 构造函数，初始化TransService对象
     * @param transService
     */
    public MateEasyTransTransMethodResultAop(TransService transService) {
        this.transService = transService;
    }

    /**
     * 该方法使用AOP切面，在执行带有@TransMethodResult注解的方法时进行拦截
     * 其目的是在方法执行后，对返回结果进行翻译处理
     *
     * @param joinPoint 切入点对象，包含被拦截方法的信息
     * @return 被拦截方法的执行结果，经过翻译处理
     * @throws Throwable 如果方法执行或翻译过程中抛出异常
     */
    @Around("( @annotation(com.fhs.core.trans.anno.TransMethodResult) && ( @target(org.springframework.web.bind.annotation.RestController) || @target(org.springframework.stereotype.Controller))  )")
    public Object transResult(ProceedingJoinPoint joinPoint) throws Throwable {
        //执行原始方法
        Object proceed = joinPoint.proceed();
        try {
            Set<String> includeFields = null;
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
            //调用TransUtil进行翻译处理
            return MateEasyTransUtils.transOne(proceed, transService, isEnableTile, new ArrayList<>(),includeFields,excludeFields);
        } catch (Throwable e) {
            //记录翻译过程中的错误日志
            log.error("翻译错误", e);
        }
        //如果翻译过程中出现异常，直接返回原始方法的执行结果
        return proceed;
    }

}
