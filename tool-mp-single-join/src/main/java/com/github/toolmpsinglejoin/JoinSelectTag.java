package com.github.toolmpsinglejoin;

import java.lang.annotation.*;

/**
 * 标志方法返回值需要连接查询
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JoinSelectTag {
}
