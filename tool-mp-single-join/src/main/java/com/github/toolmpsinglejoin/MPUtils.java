package com.github.toolmpsinglejoin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * mybatis plus util
 * @author zlf
 * @since 2021年05月10日 13:57:00
 */
@Slf4j
public class MPUtils implements ApplicationListener<ApplicationStartedEvent>, ApplicationContextAware {
    static Map<Type, BaseMapper> MODEL_MAPPER_CACHE;
    private static ApplicationContext APPLICATION_CONTEXT;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        MPUtils.APPLICATION_CONTEXT = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        log.info("init MODEL_MAPPER_CACHE...");
        MODEL_MAPPER_CACHE = APPLICATION_CONTEXT.getBean(SqlSessionFactory.class).getConfiguration().getMapperRegistry()
            .getMappers().stream()
            .collect(Collectors.toMap(m -> ResolvableType.forType(m).getInterfaces()[0].getGeneric(0).resolve() ,m -> ((BaseMapper) APPLICATION_CONTEXT.getBean(m))));
        log.info("Model 和 Mapper 的映射缓存已初始化完成！");
    }

    public static Map<Type, BaseMapper> getModelMapperCache() {
        return Collections.unmodifiableMap(MODEL_MAPPER_CACHE);
    }
}
