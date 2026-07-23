package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options.MagicS3PlatformOptionProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MagicAPIS3ConfigurationTest {

	@Test
	void shouldNotDirectlyDependOnPlatformOptionProviderBean() {
		for (Field field : MagicAPIS3Configuration.class.getDeclaredFields()) {
			assertFalse(MagicS3PlatformOptionProvider.class.isAssignableFrom(field.getType()));
		}
	}
}
