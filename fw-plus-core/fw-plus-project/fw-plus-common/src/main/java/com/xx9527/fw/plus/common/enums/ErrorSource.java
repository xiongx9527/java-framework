package com.xx9527.fw.plus.common.enums;

import lombok.Getter;

/** 错误来源 — 状态码第1-2位 */
@Getter
public enum ErrorSource {

    CLIENT(40, "客户端错误"),
    SERVER(50, "服务端错误");

    private final int code;
    private final String desc;

    ErrorSource(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
