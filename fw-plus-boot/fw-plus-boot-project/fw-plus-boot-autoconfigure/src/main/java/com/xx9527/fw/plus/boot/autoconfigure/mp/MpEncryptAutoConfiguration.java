package com.xx9527.fw.plus.boot.autoconfigure.mp;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.xx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xx9527.fw.plus.mp.encrypt.FieldDecryptInterceptor;
import com.xx9527.fw.plus.mp.encrypt.FieldEncryptInterceptor;
import com.xx9527.fw.plus.mp.encrypt.config.EncryptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@Slf4j
//@ConditionalOnClass({com.xx9527.fw.plus.mp.encrypt.FieldEncryptInterceptor.class,
//       com.xx9527.fw.plus.mp.encrypt.FieldDecryptInterceptor.class})
//@ConditionalOnBean({SqlSessionFactory.class})
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ConditionalOnProperty(prefix = "fw.plus.mp-encrypt", name = "enabled", havingValue = "true",matchIfMissing = true)
@Lazy(false)
public class MpEncryptAutoConfiguration {

    /**
     * 创建并配置一个默认的加密器 bean
     * 此方法利用 Spring 框架的 @Bean 注解来定义一个 bean，使得在应用程序上下文中可用
     * 使用 @ConditionalOnMissingBean 注解确保只有在当前上下文中没有其他同类型 bean 的情况下才会创建这个 bean
     * 这样做是为了避免 bean 的重复创建，以及提供一个默认实现，以便用户可以自定义实现
     *
     * @return DefaultEncryptor 实例，用于数据加密
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultEncryptor defaultEncryptor() {
        return new DefaultEncryptor();
    }

    /**
     * 配置加密器属性
     *
     * 该方法通过Spring的@Bean注解定义了一个bean，使得该bean可以在Spring容器中被管理和注入
     * 同时，使用@ConfigurationProperties注解，将配置文件中以"fw.plus.mp-encrypt"为前缀的属性绑定到EncryptorProperties类中
     * 这样做便于管理和配置加密相关的属性，提高配置的灵活性和可维护性
     *
     * @return EncryptorProperties实例，包含从配置文件中读取的加密器属性
     */
    @Bean
    @ConfigurationProperties(
            prefix = "fw.plus.mp-encrypt"
    )
    public EncryptorProperties encryptorProperties() {
        return new EncryptorProperties();
    }

    /**
     * 创建FieldEncryptInterceptor拦截器的Bean
     * 该拦截器用于在字段加密和解密处理中，根据配置属性和加密器进行操作
     *
     * @param encryptor 字段加密器，用于执行实际的加密和解密操作
     * @param encryptorProperties 加密器的配置属性，包含加密密钥等信息
     * @return 返回配置好的FieldEncryptInterceptor拦截器实例
     */
    @Bean
    //@ConditionalOnBean({ EncryptorProperties.class,FieldEncrypt.IEncryptor.class,})
    public FieldEncryptInterceptor fieldEncryptInterceptor(FieldEncrypt.IEncryptor encryptor,
                                                           EncryptorProperties encryptorProperties) {
        FieldEncryptInterceptor fieldEncryptInterceptor = new FieldEncryptInterceptor(encryptor,encryptorProperties);
        return fieldEncryptInterceptor;
    }


    /**
     * 创建并配置一个字段解密拦截器
     * 该方法在满足特定条件时被调用：即当EncryptorProperties类和FieldEncrypt.IEncryptor接口的实现同时存在时
     * 它的主要作用是初始化一个字段解密拦截器，并将其配置信息和加密器注入进去
     *
     * @param encryptor 负责字段加密和解密的加密器
     * @param encryptorProperties 加密器的配置属性
     * @return 返回配置好的字段解密拦截器实例
     */
    @Bean
    //@ConditionalOnBean({ EncryptorProperties.class,FieldEncrypt.IEncryptor.class,})
    public FieldDecryptInterceptor fieldDecryptInterceptor(FieldEncrypt.IEncryptor encryptor,
                                                           EncryptorProperties encryptorProperties) {
        FieldDecryptInterceptor fieldDecryptInterceptor = new FieldDecryptInterceptor(encryptor,encryptorProperties);
        return fieldDecryptInterceptor;
    }


}
