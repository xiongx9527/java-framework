package com.xx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import com.alibaba.fastjson2.JSON;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.JsonBean;

@Slf4j
public final class ParamLogger {

    /**
     * 私有构造函数，防止外部实例化此类
     */
    private ParamLogger() {}

    /**
     * 记录请求参数日志
     * @param requestEntity 请求实体对象，包含API信息、请求参数、请求体和请求头等信息
     */
    public static void logRequest(RequestEntity requestEntity) {
        if (requestEntity == null) {
            return;
        }
        ApiInfo info = requestEntity.getApiInfo();
        if (ParamConfig.isEnabled(info, Options.PRINT_REQUEST_PARAMETERS.getValue())) {
            log.info("requester={}, url={}, params={}, data={}, headers={}",
                    requestEntity.getRequest().getRemoteAddr(),
                    ParamConfig.getPath(info),
                    requestEntity.getParameters(),
                    requestEntity.getRequestBody(),
                    requestEntity.getHeaders());
        }
    }

    /**
     * 记录响应结果日志
     * @param requestEntity 请求实体对象，用于获取API信息
     * @param returnValue 返回值对象，预期为JsonBean类型
     */
    public static void logResponse(RequestEntity requestEntity, Object returnValue) {
        if (requestEntity == null) {
            return;
        }
        ApiInfo info = requestEntity.getApiInfo();
        if (ParamConfig.isEnabled(info, Options.PRINT_RESPONSE_PARAMETERS.getValue())) {
            if (returnValue instanceof JsonBean) {
                JsonBean<?> jsonBean = (JsonBean<?>) returnValue;
                Object data = jsonBean.getData();
                boolean isSimple = data == null || data instanceof CharSequence || data instanceof Number || data instanceof Boolean;
                Object dataOut = isSimple ? data : JSON.toJSONString(data);
                log.info("url={}, code={}, msg={}, cost={}ms, data={}",
                        ParamConfig.getPath(info),
                        jsonBean.getCode(),
                        jsonBean.getMessage(),
                        jsonBean.getExecuteTime(),
                        dataOut);
            }
        }
    }
}
