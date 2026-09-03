package dev.langchain4j.service.guardrail;
import dev.langchain4j.guardrail.InputGuardrail;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Documented @Target({ElementType.TYPE,ElementType.METHOD}) public @interface InputGuardrails { Class<? extends InputGuardrail>[] value(); }
