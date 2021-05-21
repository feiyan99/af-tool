package com.github.toolmpsinglejoin;

import java.lang.annotation.*;

/**
 * 标注需要关联查询的属性
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Prop {
    String group() default "default";

    /**
     * 默认取打注解的字段名
     * @return
     */
    String sourcePropName() default "";

}
