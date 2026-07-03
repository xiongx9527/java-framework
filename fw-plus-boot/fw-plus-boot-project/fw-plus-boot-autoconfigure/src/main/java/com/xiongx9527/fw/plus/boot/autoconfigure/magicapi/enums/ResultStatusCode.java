package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums;

import org.ssssssss.magicapi.core.model.JsonBean;

public enum ResultStatusCode {
    FORBIDDEN(403, "禁止访问");

    /**
     * 构造函数，用于创建ResultStatusCode实例
     * @param code 状态码
     * @param message 状态消息
     */
    ResultStatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private int code;

    private String message;

    /**
     * 将当前状态码和消息封装成JsonBean对象
     * @return 包含状态码和消息的JsonBean对象
     */
    public JsonBean json(){
        return new JsonBean<>(this.code, this.message);
    }

}
