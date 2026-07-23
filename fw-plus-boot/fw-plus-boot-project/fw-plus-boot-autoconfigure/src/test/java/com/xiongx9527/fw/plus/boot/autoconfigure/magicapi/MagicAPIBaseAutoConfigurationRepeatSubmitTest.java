package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.LocalLockModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.LockModuleConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.DefaultRepeatSubmitKeySuffixProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.RepeatSubmitKeyResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.RepeatSubmitKeySuffixProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.RepeatSubmitRequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIBaseAutoConfigurationRepeatSubmitTest {

	@Test
	void repeatSubmitInfrastructureBeanMethodsAreRegistered() throws Exception {
		assertBeanMethod("defaultRepeatSubmitKeySuffixProvider");
		assertBeanMethod("repeatSubmitKeyResolver", ApplicationContext.class, RepeatSubmitKeySuffixProvider.class);
		assertBeanMethod("repeatSubmitRequestInterceptor", Lock.class, ResultProvider.class, RepeatSubmitKeyResolver.class);
	}

	@Test
	void createsRepeatSubmitInfrastructure() {
		MagicAPIBaseAutoConfiguration configuration = configuration();
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		RepeatSubmitKeySuffixProvider suffixProvider = configuration.defaultRepeatSubmitKeySuffixProvider();
		RepeatSubmitKeyResolver resolver = configuration.repeatSubmitKeyResolver(applicationContext, suffixProvider);
		RepeatSubmitRequestInterceptor interceptor = configuration.repeatSubmitRequestInterceptor(
				mock(Lock.class), mock(ResultProvider.class), resolver);

		assertThat(suffixProvider).isInstanceOf(DefaultRepeatSubmitKeySuffixProvider.class);
		assertThat(resolver).isNotNull();
		assertThat(interceptor).isNotNull();
	}

	@Test
	void lockModuleConfigurationProvidesLocalLockWhenNoLockTemplateExists() {
		LocalLockModule localLockModule = new LockModuleConfiguration().localLockModule();

		assertThat(localLockModule).isInstanceOf(Lock.class);
	}

	private void assertBeanMethod(String name, Class<?>... parameterTypes) throws Exception {
		Method method = MagicAPIBaseAutoConfiguration.class.getMethod(name, parameterTypes);

		assertThat(method.getAnnotation(Bean.class)).isNotNull();
	}

	@SuppressWarnings("unchecked")
	private MagicAPIBaseAutoConfiguration configuration() {
		return new MagicAPIBaseAutoConfiguration(
				mock(ObjectProvider.class),
				mock(ApplicationContext.class));
	}
}
