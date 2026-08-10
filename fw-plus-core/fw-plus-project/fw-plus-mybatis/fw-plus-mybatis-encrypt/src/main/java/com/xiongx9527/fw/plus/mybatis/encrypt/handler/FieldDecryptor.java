package com.xiongx9527.fw.plus.mybatis.encrypt.handler;

import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mybatis.encrypt.config.EncryptorProperties;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldSetProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

/**
 * 字段解密工具。
 */
@Slf4j
public class FieldDecryptor {

	private FieldDecryptor() {
	}

	/**
	 * 解密并写回字段值。
	 *
	 * @param encryptor        加密器
	 * @param properties       加密配置
	 * @param metaObject       MyBatis MetaObject
	 * @param fieldSetProperty 字段属性
	 */
	public static void processField(FieldEncrypt.IEncryptor encryptor, EncryptorProperties properties,
			MetaObject metaObject, FieldSetProperty fieldSetProperty) {
		String fieldName = fieldSetProperty.getFieldName();
		Object fieldValue = metaObject.getValue(fieldName);
		if (fieldValue != null) {
			if (encryptor != null && fieldValue instanceof String) {
				try {
					FieldEncrypt fieldEncrypt = fieldSetProperty.getFieldEncrypt();
					if (fieldEncrypt != null && fieldEncrypt.executeDecrypt()) {
						fieldValue = FieldEncryptorHandler.getEncryptor(encryptor, fieldEncrypt.encryptor())
								.decrypt(properties.algorithm(fieldEncrypt), properties.getPrivateKey(),
										properties.getIv(), properties.salt(fieldEncrypt), (String) fieldValue);
					}
				} catch (Throwable e) {
					log.error("Field decryption failed: {}", e.getMessage(), e);
				}
			}
			metaObject.setValue(fieldName, fieldValue);
		}
	}
}
