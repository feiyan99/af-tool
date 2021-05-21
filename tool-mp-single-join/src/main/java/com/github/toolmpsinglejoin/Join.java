package com.github.toolmpsinglejoin;

import java.lang.annotation.*;

/**
 * 关联查询器 封装关联查询的通用部分逻辑
 * @author zlf
 * @since 2020年09月14日 16:03:00
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(JoinSelectors.class)
public @interface Join {

    /**
     * 值的来源对象
     */
    Class<?> source();

    /**
     * 查询时的关联字段
     * @return
     */
    String relationProp() default "id";

    String group() default "default";

    SourceType sourceType() default SourceType.DB;

    /**
     * 来源类型
     */
    enum SourceType {
        DB, OBJECT
    }


}