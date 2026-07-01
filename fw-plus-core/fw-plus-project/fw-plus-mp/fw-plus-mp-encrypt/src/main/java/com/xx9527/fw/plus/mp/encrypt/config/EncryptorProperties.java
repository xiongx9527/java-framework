package com.xx9527.fw.plus.mp.encrypt.config;

import com.xx9527.fw.plus.common.encrypt.DefaultEncryptorProperties;
import com.xx9527.fw.plus.common.mate.annotation.Algorithm;
import com.xx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import lombok.Data;

import java.io.Serializable;

/**
 * 加密器配置属性类
 *
 * <p>该类继承自DefaultEncryptorProperties，用于定义和管理加密器的相关配置属性。
 * 通过@Data注解自动生成getter、setter、toString等方法，简化配置属性的访问和操作。</p>
 *
 * @since 1.0
 */
@Data
public class EncryptorProperties extends DefaultEncryptorProperties {
}
