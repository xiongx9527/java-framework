package com.xx9527.fw.plus.common.mate.annotation;

/**
 * 算法枚举类，定义了系统支持的各种加密算法和编码方式
 * 该枚举包含了常见的哈希算法、对称加密算法、非对称加密算法、国密算法和编码方式
 */
public enum Algorithm {
    /** MD5 32位摘要算法 */
    MD5_32,
    /** MD5 16位摘要算法 */
    MD5_16,
    /** AES对称加密算法 */
    AES,
    /** DES对称加密算法 */
    DES,
    /** SM2国密非对称加密算法 */
    SM2,
    /** SM3国密哈希算法 */
    SM3,
    /** SM4国密对称加密算法 */
    SM4,
    /** RSA非对称加密算法 */
    RSA,
    /** BASE64编码方式 */
    BASE64,
    /** 基于MD5和DES的PBE加密算法 */
    PBEWithMD5AndDES,
    /** 基于SHA1和DESede的PBE加密算法 */
    PBEWithSHA1AndDESede,
    /** 基于SHA1和RC2的PBE加密算法 */
    PBEWithSHA1AndRC2_40;

    /**
     * 算法枚举的构造函数
     * 私有构造函数，防止外部实例化
     */
    private Algorithm() {
    }
}
