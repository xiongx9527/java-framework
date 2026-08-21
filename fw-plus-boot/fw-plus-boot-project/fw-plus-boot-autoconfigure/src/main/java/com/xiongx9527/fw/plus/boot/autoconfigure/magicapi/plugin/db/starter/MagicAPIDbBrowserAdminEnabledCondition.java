package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.starter;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class MagicAPIDbBrowserAdminEnabledCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		return environment.getProperty("magic-api.plugin.db.admin.enabled", Boolean.class, false);
	}
}
