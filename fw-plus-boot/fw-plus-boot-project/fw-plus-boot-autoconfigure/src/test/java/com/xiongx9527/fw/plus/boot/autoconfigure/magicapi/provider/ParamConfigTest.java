package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Option;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ParamConfigTest {

	private GenericApplicationContext context;

	@AfterEach
	void closeContext() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void externalMagicApiPathOptionOverridesInterfaceOption() {
		context = new GenericApplicationContext();
		TestPropertyValues.of("magic.api./order/create.options.avoid_repeat_submit_enable=false").applyTo(context);
		context.refresh();
		new SpringUtils().setApplicationContext(context);

		ApiInfo info = api("/order/create", Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue(), "true");

		assertThat(ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue())).isEqualTo("false");
		assertThat(ParamConfig.isEnabled(info, Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue())).isFalse();
	}

	@Test
	void interfaceOptionIsUsedWhenExternalConfigIsMissing() {
		context = new GenericApplicationContext();
		context.refresh();
		new SpringUtils().setApplicationContext(context);

		ApiInfo info = api("order/create", Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue(), "2500");

		assertThat(ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue())).isEqualTo("2500");
		assertThat(ParamConfig.getPath(info)).isEqualTo("/order/create");
	}

	private ApiInfo api(String path, String optionName, String optionValue) {
		ApiInfo info = new ApiInfo();
		info.setPath(path);
		info.setOption(Collections.singletonList(new Option(optionName, optionValue)));
		return info;
	}
}
