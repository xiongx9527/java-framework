package dev.langchain4j.service.guardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import java.lang.annotation.*;
@Documented @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE,ElementType.METHOD}) public @interface OutputGuardrails { Class<? extends OutputGuardrail>[] value(); int maxRetries() default dev.langchain4j.guardrail.config.OutputGuardrailsConfig.MAX_RETRIES_DEFAULT; }
