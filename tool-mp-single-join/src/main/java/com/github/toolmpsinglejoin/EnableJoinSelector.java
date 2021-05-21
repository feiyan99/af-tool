package com.github.toolmpsinglejoin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 关联查询器开关
 * @author zlf
 * @since 2021年05月10日 13:47:00
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnClass(BaseMapper.class)
@Import({MPUtils.class, JoinSelectorAspect.class})
public @interface EnableJoinSelector {
}
