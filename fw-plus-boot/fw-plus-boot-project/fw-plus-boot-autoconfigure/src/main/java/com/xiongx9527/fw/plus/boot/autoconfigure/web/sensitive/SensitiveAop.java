package com.xiongx9527.fw.plus.boot.autoconfigure.web.sensitive;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;

/**
 * @Author: wanyang
 * @Date: 2024/12/23 20:35
 * @Modified By:
 * @Description:
 */
@Slf4j
@Aspect
public class SensitiveAop extends SensitiveAopBase {

	/**
	 * 敏感字段忽略标志位配置
	 * 用于控制是否忽略敏感字段的处理
	 * 默认值为true，表示忽略敏感字段
	 * 可通过配置文件fw.plus.boot.web.fieldIgnoreSensitiveFlag属性进行覆盖
	 */
	@Value("${fw.plus.boot.web.fieldIgnoreSensitiveFlag:true}")
	private boolean fieldIgnoreSensitiveFlag = true;

	/**
	 * 控制器字段敏感信息忽略的环绕通知处理方法
	 * <p>
	 * 该方法通过AOP环绕通知，在执行控制器中的请求映射方法前后进行操作，
	 * 主要用于处理字段敏感信息的忽略功能。
	 * </p>
	 * <p>
	 * 切点表达式匹配规则：
	 * - 公共方法：execution(public * com.xiongx9527..*.*(..))
	 * - 控制器类：@target(org.springframework.web.bind.annotation.RestController) 或 @target(org.springframework.stereotype.Controller)
	 * - 请求映射方法：@annotation(org.springframework.web.bind.annotation.RequestMapping) 或其他HTTP方法注解
	 * </p>
	 *
	 * @param joinPoint 连接点对象，包含目标方法的信息
	 * @return 原方法的执行结果
	 * @throws Throwable 目标方法可能抛出的异常
	 */
	@Around("(execution(public * com.xiongx9527..*.*(..)) && (@target(org.springframework.web.bind.annotation.RestController)   " +
			" || @target(org.springframework.stereotype.Controller)) && (@annotation(org.springframework.web.bind.annotation.RequestMapping)  " +
			" || @annotation(org.springframework.web.bind.annotation.PostMapping)    " +
			" || @annotation(org.springframework.web.bind.annotation.PutMapping)     " +
			" || @annotation(org.springframework.web.bind.annotation.DeleteMapping)  " +
			" || @annotation(org.springframework.web.bind.annotation.GetMapping))) ")
	public Object controllerFieldIgnoreSensitive(ProceedingJoinPoint joinPoint) throws Throwable {
		// 根据配置标志决定是否执行字段敏感信息忽略处理
		if (Boolean.TRUE.equals(fieldIgnoreSensitiveFlag)) {
			super.doControllerFieldIgnoreSensitive(joinPoint);
		}
		// 执行原方法
		return joinPoint.proceed();
	}


}
