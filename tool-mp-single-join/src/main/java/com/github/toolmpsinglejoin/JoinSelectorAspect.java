package com.github.toolmpsinglejoin;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

import java.util.Collection;
import java.util.Map;

/**
 * 关联查询切面
 * @author 张龙飞
 * @since 2020年09月16日 11:42:00
 */
@Slf4j
@Aspect
public class JoinSelectorAspect {

    @AfterReturning(value = "@annotation(com.github.toolmpsinglejoin.JoinSelectTag)", returning = "ret")
    public Object joinSelect(Object ret) {

        if (ret instanceof Collection) {
            JoinSelector.select(((Collection) ret).toArray());
        } else if (ret instanceof Map) {
            JoinSelector.select(((Map) ret).values().toArray());
        } else if (ret instanceof CommonResp) {
            joinSelect(((CommonResp) ret).getData());
        } else {
            JoinSelector.select(ret);
        }

        return ret;
    }

    /**
     * 项目当中通用的响应格式，实现该接口，并注入 {@link JoinSelectorAspect}，即可通过 {@link JoinSelectTag} 注解方式关联查询属性
     * @param <T>
     */
    interface CommonResp<T> {

        T getData();
    }


}
