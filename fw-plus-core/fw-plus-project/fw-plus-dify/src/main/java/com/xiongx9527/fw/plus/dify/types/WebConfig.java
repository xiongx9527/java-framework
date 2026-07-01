package com.xiongx9527.fw.plus.dify.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WebConfig {
    /**
     * Dify Server Address
     */
    private String server;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;
}
