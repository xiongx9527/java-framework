package com.xiongx9527.fw.plus.boot.autoconfigure.mp;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mp.encrypt.FieldDecryptInterceptor;
import com.xiongx9527.fw.plus.mp.encrypt.FieldEncryptInterceptor;
import com.xiongx9527.fw.plus.mp.encrypt.config.EncryptorProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@ConditionalOnClass({FieldEncryptInterceptor.class, FieldDecryptInterceptor.class})
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ConditionalOnProperty(prefix = "fw.plus.mp-encrypt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MpEncryptAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DefaultEncryptor defaultEncryptor() {
        return new DefaultEncryptor();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "fw.plus.mp-encrypt")
    public EncryptorProperties encryptorProperties() {
        return new EncryptorProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public FieldEncryptInterceptor fieldEncryptInterceptor(FieldEncrypt.IEncryptor encryptor,
                                                           EncryptorProperties encryptorProperties) {
        return new FieldEncryptInterceptor(encryptor, encryptorProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FieldDecryptInterceptor fieldDecryptInterceptor(FieldEncrypt.IEncryptor encryptor,
                                                           EncryptorProperties encryptorProperties) {
        return new FieldDecryptInterceptor(encryptor, encryptorProperties);
    }
}
