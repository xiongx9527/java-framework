package dev.langchain4j.agent.tool;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(PARAMETER)
public @interface P {
    String name() default "";
    String description() default "";
    String value() default "";
    boolean required() default true;
    String defaultValue() default NO_DEFAULT;
    String NO_DEFAULT = "\0__LANGCHAIN4J_NO_DEFAULT__\0";
}
