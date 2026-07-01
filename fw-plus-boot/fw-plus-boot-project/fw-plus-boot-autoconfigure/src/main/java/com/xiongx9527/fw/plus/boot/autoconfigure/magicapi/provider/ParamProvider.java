package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import org.ssssssss.magicapi.core.context.RequestContext;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.script.MagicScriptContext;

public class ParamProvider implements RequestInterceptor {

    @Override
    public Object preHandle(ApiInfo info, MagicScriptContext context,
                            MagicHttpServletRequest request, MagicHttpServletResponse response) {
        RequestEntity entity = RequestContext.getRequestEntity();
        ParamLogger.logRequest(entity);
        return null;
    }

    @Override
    public Object postHandle(ApiInfo info, MagicScriptContext context, Object returnValue,
                             MagicHttpServletRequest request, MagicHttpServletResponse response) {
        return null;
    }

    @Override
    public void afterCompletion(ApiInfo info, MagicScriptContext context, Object returnValue,
                                MagicHttpServletRequest request, MagicHttpServletResponse response,
                                Throwable throwable) {
        ParamLogger.logResponse(RequestContext.getRequestEntity(), returnValue);
    }
}
