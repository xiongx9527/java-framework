package com.xiongx9527.fw.plus.mybatis.datascope.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RowPermission {
	int STATUS_NEITHER = 0;
	int STATUS_TENANT_ONLY = 1;
	int STATUS_BOTH = 3;
	int STATUS_ON_CONDITION = -1;

	int status() default STATUS_BOTH;

	String condition() default "";
}
