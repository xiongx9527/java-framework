package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import lombok.Data;
import org.ssssssss.magicapi.core.context.RequestEntity;

public interface ExceptionResultProvider {

    /**
     * 构建异常结果对象
     *
     * 此方法用于当请求处理过程中发生异常时，构建一个ExceptionResult对象
     * 它封装了异常信息和请求的相关信息，以便于统一处理和响应异常情况
     *
     * @param requestEntity 请求实体，包含请求的相关信息，如URL、请求头等
     * @param throwable 发生的异常或错误，用于封装到结果对象中
     * @return 返回一个ExceptionResult对象，包含封装好的异常信息和请求信息
     */
    ExceptionResult buildException(RequestEntity requestEntity, Throwable throwable);


    /**
     * 异常结果类，用于封装异常相关信息
     */
    @Data
    public static class ExceptionResult {
        /**
         * 错误代码，用于标识具体的错误类型
         */
        private int code;

        /**
         * 错误消息，提供更详细的错误描述
         */
        private String message;
    }


}
