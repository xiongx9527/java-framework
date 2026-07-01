package com.xiongx9527.fw.plus.common.encrypt;

import com.xiongx9527.fw.plus.common.mate.annotation.Algorithm;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import lombok.Data;

import java.io.Serializable;

@Data
public class DefaultEncryptorProperties implements Serializable {
    /**
     * 盐值，用于加密算法中增加随机性，防止彩虹表攻击
     */
    private String salt;

    /**
     * 公钥，用于非对称加密算法中的加密操作
     */
    private String publicKey;

    /**
     * 私钥，用于非对称加密算法中的解密操作
     */
    private String privateKey;

    /**
     * 初始化向量，用于对称加密算法中确保相同明文加密后产生不同的密文
     */
    private String iv;

    /**
     * 加密算法枚举，指定使用的具体加密算法类型
     */
    private Algorithm algorithm;


    /**
     * 默认构造函数
     */
    public DefaultEncryptorProperties() {
    }

    /**
     * 获取盐值配置
     *
     * @param fieldEncrypt 字段加密注解对象，用于获取注解中配置的盐值
     * @return 返回优先级更高的盐值：如果当前对象的salt不为空且注解中的salt为空字符串，则返回当前对象的salt，否则返回注解中的salt
     */
    public String salt(FieldEncrypt fieldEncrypt) {
        String salt = fieldEncrypt.salt();
        return null != this.salt && "".equals(salt) ? this.salt : salt;
    }

    /**
     * 获取加密算法配置
     *
     * @param fieldEncrypt 字段加密注解对象，用于获取注解中配置的加密算法
     * @return 返回优先级更高的加密算法：如果当前对象的algorithm不为空且注解中的算法为PBEWithMD5AndDES，则返回当前对象的algorithm，否则返回注解中的algorithm
     */
    public Algorithm algorithm(FieldEncrypt fieldEncrypt) {
        Algorithm algorithm = fieldEncrypt.algorithm();
        return null != this.algorithm && algorithm == Algorithm.PBEWithMD5AndDES ? this.algorithm : algorithm;
    }

}
