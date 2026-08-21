package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf;

import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mf.encrypt.MybatisFlexFieldNameResolver;
import com.xiongx9527.fw.plus.mybatis.encrypt.config.EncryptorProperties;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldDecryptInterceptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldEncryptInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 字段加解密自动配置。
 *
 * <p>当 classpath 存在 Flex 加密适配器时注册字段名解析器与加解密拦截器。
 * 加解密配置与默认加密器复用现有 Bean（如 {@code MpEncryptAutoConfiguration}
 * 注册的 {@code fw.plus.mp-encrypt} 配置）；不存在时使用默认值兜底。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({FieldEncryptInterceptor.class, FieldDecryptInterceptor.class, MybatisFlexFieldNameResolver.class})
@ConditionalOnProperty(prefix = "fw.plus.mf-encrypt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MfEncryptAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public MybatisFlexFieldNameResolver mybatisFlexFieldNameResolver() {
		return new MybatisFlexFieldNameResolver();
	}

	@Bean
	@ConditionalOnMissingBean
	public FieldEncryptInterceptor fieldEncryptInterceptor(ObjectProvider<FieldEncrypt.IEncryptor> encryptorProvider,
			ObjectProvider<EncryptorProperties> encryptorPropertiesProvider,
			MybatisFlexFieldNameResolver fieldNameResolver) {
		return new FieldEncryptInterceptor(encryptorProvider.getIfAvailable(DefaultEncryptor::new),
				encryptorPropertiesProvider.getIfAvailable(EncryptorProperties::new), fieldNameResolver);
	}

	@Bean
	@ConditionalOnMissingBean
	public FieldDecryptInterceptor fieldDecryptInterceptor(ObjectProvider<FieldEncrypt.IEncryptor> encryptorProvider,
			ObjectProvider<EncryptorProperties> encryptorPropertiesProvider,
			MybatisFlexFieldNameResolver fieldNameResolver) {
		return new FieldDecryptInterceptor(encryptorProvider.getIfAvailable(DefaultEncryptor::new),
				encryptorPropertiesProvider.getIfAvailable(EncryptorProperties::new), fieldNameResolver);
	}
}
