package dev.langchain4j.agent.tool;

import dev.langchain4j.Experimental;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Marks a Java method as a tool that can be called by a language model. */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Tool {

    String name() default "";

    String[] value() default "";

    @Experimental
    ReturnBehavior returnBehavior() default ReturnBehavior.TO_LLM;

    @Experimental
    SearchBehavior searchBehavior() default SearchBehavior.SEARCHABLE;

    @Experimental
    String metadata() default "{}";
}
