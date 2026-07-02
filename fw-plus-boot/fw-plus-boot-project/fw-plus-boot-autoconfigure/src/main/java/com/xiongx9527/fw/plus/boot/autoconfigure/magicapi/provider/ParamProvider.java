package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.script.MagicScriptContext;

public class ParamProvider implements RequestInterceptor {
    /**
     * 请求之前执行
     * 形参:
     * info – 接口信息
     * context – 脚本上下文
     * request – HttpServletRequest
     * response – HttpServletResponse
     * 返回值:
     * 当返回对象时，直接将此对象返回到页面，返回null时，继续执行后续操作
     * 抛出:
     * Exception – 处理失败时抛出的异常
     */
    public Object preHandle(ApiInfo info, MagicScriptContext context, MagicHttpServletRequest request, MagicHttpServletResponse response) throws Exception {
        return null;
    }

    /**
     * 执行完毕之后执行
     * 形参:
     * info – 接口信息
     * context – 脚本上下文
     * returnValue – 即将要返回到页面的值
     * request – HttpServletRequest
     * response – HttpServletResponse
     *
     * 返回值:
     * 返回到页面的对象, 当返回null时执行后续拦截器，否则直接返回该值，不执行后续拦截器
     * 抛出:
     * Exception – 处理失败时抛出的异常
     */
    public Object postHandle(ApiInfo info, MagicScriptContext context, Object returnValue, MagicHttpServletRequest request, MagicHttpServletResponse response) throws Exception {
        return null;
    }

    /**
     *
     * 接口执行完毕之后执行
     * 形参:
     * info – 接口信息
     * context – 脚本上下文
     * returnValue – 即将要返回到页面的值
     * request – HttpServletRequest
     * response – HttpServletResponse
     * throwable – 异常对象
     *
     */
    public void afterCompletion(ApiInfo info, MagicScriptContext context, Object returnValue, MagicHttpServletRequest request, MagicHttpServletResponse response, Throwable throwable) {

    }
}
