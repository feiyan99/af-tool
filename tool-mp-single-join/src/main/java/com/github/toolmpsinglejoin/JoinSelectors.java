package com.github.toolmpsinglejoin;

import java.lang.annotation.*;

/**
 * 重复注解, 为了使用更方便
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JoinSelectors {

    Join[] value() default {};
}