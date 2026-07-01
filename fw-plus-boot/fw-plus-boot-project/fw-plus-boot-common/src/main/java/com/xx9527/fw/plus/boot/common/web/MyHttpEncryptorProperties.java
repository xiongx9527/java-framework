package com.xx9527.fw.plus.boot.common.web;

import com.xx9527.fw.plus.common.encrypt.DefaultEncryptorProperties;
import lombok.Data;

/**
 * HTTP加密器配置属性类
 * 该类继承自DefaultEncryptorProperties，用于配置HTTP请求和响应的加密解密相关属性
 */
@Data
public class MyHttpEncryptorProperties extends DefaultEncryptorProperties {

    /**
     * 加密器是否启用标识
     * true表示启用加密器，false表示禁用加密器
     */
    private boolean enabled = false;

    /**
     * 加密模式配置
     * 默认值为ALL，表示同时支持加密和解密
     */
    private String encryptionMode = MyHttpEncryptorMode.ALL;

    /**
     * 加密标识头名称
     * 用于在HTTP请求头中标识需要进行加密处理的请求
     * 默认值为"X-Encryption-Flag"
     */
    private String encryptionFlagHeadName = "X-Encryption-Flag";

    /**
     * 解密标识头名称
     * 用于在HTTP请求头中标识需要进行解密处理的请求
     * 默认值为"X-Decryption-Flag"
     */
    private String decryptionFlagHeadName = "X-Decryption-Flag";

    /**
     * 排除路径数组
     * 配置不需要进行加密解密处理的URL路径
     * 默认为空数组，表示不排除任何路径
     */
    private String[] excludePaths = new String[]{};

    /**
     * HTTP加密器模式常量接口
     * 定义了四种加密模式常量，用于控制加密器的行为
     */
    public static interface MyHttpEncryptorMode {
        /**
         * 全模式：同时支持加密和解密
         */
        public static final String ALL = "10";

        /**
         * 解密模式：仅支持解密操作
         */
        public static final String Decryption = "20";

        /**
         * 加密模式：仅支持加密操作
         */
        public static final String Encryption = "30";

        /**
         * 无模式：不进行任何加密解密操作
         */
        public static final String None = "99";
    }
}

