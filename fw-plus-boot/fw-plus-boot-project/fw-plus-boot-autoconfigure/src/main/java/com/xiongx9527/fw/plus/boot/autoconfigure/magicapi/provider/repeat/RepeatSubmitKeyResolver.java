package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.ParamConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.key.MagicApiRequestKeyResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Option;

import java.util.ArrayList;
import java.util.List;

public class RepeatSubmitKeyResolver {

	private static final String PREFIX = "magicapi:repeat:submit";

	private final ApplicationContext applicationContext;
	private final RepeatSubmitKeySuffixProvider defaultSuffixProvider;
	private final MagicApiRequestKeyResolver requestKeyResolver;

	public RepeatSubmitKeyResolver(ApplicationContext applicationContext,
								   RepeatSubmitKeySuffixProvider defaultSuffixProvider) {
		this.applicationContext = applicationContext;
		this.defaultSuffixProvider = defaultSuffixProvider;
		this.requestKeyResolver = new MagicApiRequestKeyResolver();
	}

	public String resolve(ApiInfo info, RequestEntity requestEntity) {
		String main = resolveMainKey(info, requestEntity);
		String suffix = resolveSuffix(info, requestEntity);
		List<String> parts = new ArrayList<>();
		parts.add(PREFIX);
		parts.add(requestKeyResolver.normalizeSegment(requestKeyResolver.interfaceKey(info)));
		parts.add(requestKeyResolver.normalizeSegment(main));
		if (StringUtils.isNotBlank(suffix)) {
			parts.add(requestKeyResolver.normalizeSegment(suffix));
		}
		return StringUtils.join(parts, ":");
	}

	private String resolveMainKey(ApiInfo info, RequestEntity requestEntity) {
		String configured = ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_KEY.getValue());
		return requestKeyResolver.resolve(info, requestEntity, configured);
	}

	private String resolveSuffix(ApiInfo info, RequestEntity requestEntity) {
		String beanName = ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue());
		if (beanName == null && !hasOption(info, Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue())) {
			beanName = Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getDefaultValue();
		}
		RepeatSubmitKeySuffixProvider provider = getSuffixProvider(beanName);
		return provider.getSuffix(requestEntity);
	}

	private RepeatSubmitKeySuffixProvider getSuffixProvider(String beanName) {
		if (StringUtils.isNotBlank(beanName) && applicationContext != null && applicationContext.containsBean(beanName)) {
			Object bean = applicationContext.getBean(beanName);
			if (bean instanceof RepeatSubmitKeySuffixProvider) {
				return (RepeatSubmitKeySuffixProvider) bean;
			}
		}
		return defaultSuffixProvider;
	}

	private boolean hasOption(ApiInfo info, String key) {
		if (info == null || info.getOptions() == null) {
			return false;
		}
		for (Option option : info.getOptions()) {
			if (key.equals(option.getName())) {
				return true;
			}
		}
		return false;
	}
}
