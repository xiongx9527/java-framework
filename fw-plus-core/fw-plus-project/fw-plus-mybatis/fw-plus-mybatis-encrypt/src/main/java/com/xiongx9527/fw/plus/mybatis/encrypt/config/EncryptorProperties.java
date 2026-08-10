package com.xiongx9527.fw.plus.mybatis.encrypt.config;

import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptorProperties;
import lombok.Data;

/**
 * 加密器配置属性。
 *
 * <p>继承 {@link DefaultEncryptorProperties}，提供密钥、IV、盐值与算法等配置。
 * Boot 自动配置以 {@code fw.plus.mp-encrypt} / {@code fw.plus.mf-encrypt} 前缀绑定。</p>
 */
@Data
public class EncryptorProperties extends DefaultEncryptorProperties {
}
