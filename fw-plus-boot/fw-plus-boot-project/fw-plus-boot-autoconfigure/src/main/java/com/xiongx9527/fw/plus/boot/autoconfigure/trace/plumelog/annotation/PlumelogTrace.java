package com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface PlumelogTrace {
}
