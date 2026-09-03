package dev.langchain4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

/** Marks an API intended for internal library use. */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE})
@Inherited
public @interface Internal {
}
