package com.xx9527.fw.plus.mp.encrypt;

import com.xx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xx9527.fw.plus.mp.encrypt.config.EncryptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

@Slf4j
public class FieldDecryptor {

    /**
     * 私有构造函数，用于创建FieldDecryptor实例
     *
     * 此构造函数被声明为私有，表明此类可能是一个工具类或单例类，
     * 不允许外部直接通过new关键字创建实例
     */
    private FieldDecryptor() {
    }

    /**
     * 处理字段解密和数据绑定
     *
     * @param encryptor        加密器实例
     * @param properties       加密属性配置
     * @param metaObject       MyBatis 的 MetaObject
     * @param fieldSetProperty 字段属性
     */
    public static void processField(FieldEncrypt.IEncryptor encryptor, EncryptorProperties properties, MetaObject metaObject, FieldSetProperty fieldSetProperty) {
        String fieldName = fieldSetProperty.getFieldName();
        Object fieldValue = metaObject.getValue(fieldName);

        if (fieldValue != null) {
            // 处理解密逻辑
            if (encryptor != null && fieldValue instanceof String) {
                try {
                    FieldEncrypt fieldEncrypt = fieldSetProperty.getFieldEncrypt();
                    if (fieldEncrypt != null && fieldEncrypt.executeDecrypt()) {
                        fieldValue = FieldEncryptorHandler.getEncryptor(encryptor, fieldEncrypt.encryptor())
                                .decrypt(properties.algorithm(fieldEncrypt), properties.getPrivateKey(),
                                        properties.getIv(),properties.salt(fieldEncrypt), (String) fieldValue);
                    }
                } catch (Throwable e) {
                    log.error("Field decryption failed: {}", e.getMessage(), e);
                }
            }
            metaObject.setValue(fieldName, fieldValue);
        }
    }
}