package com.xiongx9527.fw.plus.common.mate.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface FieldEncrypt {
    /**
     * 获取盐值
     * @return 盐值字符串，默认为空字符串
     */
    String salt() default "";

    /**
     * 获取加密算法
     * @return 加密算法枚举值，默认为AES算法
     */
    Algorithm algorithm() default Algorithm.AES;

    /**
     * 是否执行加密操作
     * @return 布尔值，true表示执行加密，false表示不执行加密，默认为true
     */
    boolean executeEncrypt() default true;

    /**
     * 是否执行解密操作
     * @return 布尔值，true表示执行解密，false表示不执行解密，默认为true
     */
    boolean executeDecrypt() default true;

    /**
     * 获取加密器实现类
     * @return 继承自IEncryptor接口的加密器类，默认为IEncryptor.class
     */
    Class<? extends IEncryptor> encryptor() default IEncryptor.class;

    /**
     * 加密算法
     */
    public static interface IEncryptor {
        /**
         * 加密方法
         *
         * @param algorithm 加密算法
         * @param key       加密密钥
         * @param iv        初始化向量（如果需要）
         * @param salt      盐值（如果需要）
         * @param data      要加密的数据
         * @return 加密后的字符串
         * @throws Exception 加密过程中可能抛出的异常
         */
        String  encrypt(Algorithm algorithm, String key, String iv, String salt, Object data) throws Exception;

        /**
         * 解密方法
         *
         * @param algorithm     解密算法
         * @param key           解密密钥
         * @param iv            初始化向量（如果需要）
         * @param salt          盐值（如果需要）
         * @param encryptedData 要解密的数据
         * @return 解密后的字符串
         * @throws Exception 解密过程中可能抛出的异常
         */
        String decrypt(Algorithm algorithm, String key, String iv, String salt, Object encryptedData) throws Exception;
    }

}