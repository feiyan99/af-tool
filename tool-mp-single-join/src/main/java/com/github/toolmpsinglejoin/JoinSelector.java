package com.github.toolmpsinglejoin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 连接查询器
 * @author zlf
 * @since 2020年09月17日 14:19:00
 */
@Slf4j
public class JoinSelector {

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static void select(Object... objects) {
        // 校验合法性
        if (objects == null || objects.length == 0) {
            return;
        }

        log.debug("关联查询 --------------------------- start ");

        // key: fieldName value: 连接字段 注解
        List<Field_Annotation> joinSelectorSet = new ArrayList<>();

        // key: group value: 同一组的 查询字段 注解
        Map<String, List<Field_Annotation>> selectPropGroup = new HashMap<>();

        // 初始化工作
        init(objects[0], joinSelectorSet, selectPropGroup);

        // 拿到所有的 关联值 即 relationField 字段的值
        // 按照 连接字段 的字段名先分组
        Map<String, List<Field_Annotation>> fieldName_fieldAnnotation = joinSelectorSet.stream().collect(Collectors.groupingBy(annotation -> annotation.fieldName));    //  所有的字段


        /*
         * 字段与 对应的 value list 的映射关系
         */
        Map<String, Set<Object>> fieldValueMap = new HashMap<>();
        /*
         * 关联值和对应对象的映射关系 key: oneq_value value: obj
         */
        Map<String, List<Object>> valueObjMap = new HashMap<>();


        fieldName_fieldAnnotation.forEach((fieldName, field_AnnotationList) -> {
            Set<Object> value = Stream.of(objects).map(object -> {
                Object fieldVal = MPUtils.getValue(object, fieldName);

                field_AnnotationList.forEach(field_annotation -> {
                    if (Objects.nonNull(fieldVal)) {
                        String key = ((Join) field_annotation.annotation).relationProp() + "_" + fieldVal;
                        valueObjMap.computeIfAbsent(key, k -> new ArrayList<>()).add(object);
                    }
                });
                return fieldVal;
            }).filter(Objects::nonNull)
                .collect(Collectors.toSet());

            fieldValueMap.put(fieldName, value);
        });


        // 查出所有的对象
        for (Field_Annotation fieldAnnotation : joinSelectorSet) {
            Join join = (Join) fieldAnnotation.annotation;
            String joinFieldName = fieldAnnotation.fieldName;
            Set<Object> propValues = fieldValueMap.get(joinFieldName);
            // 如果为空,说明属性之间有依赖关系,需要重新初始化一次
            if (propValues.isEmpty()) {
                propValues = reInit(fieldName_fieldAnnotation, fieldValueMap, valueObjMap, joinFieldName, objects);
            }

            BaseMapper<?> baseMapper = MPUtils.getModelMapperCache().get(join.source());
            QueryWrapper queryWrapper = Wrappers.query();
            List<Field_Annotation> select_prop = selectPropGroup.get(join.group());
            /**
             * 要查询的属性集合
             */
            List<String> selectPropList = select_prop.stream().map(prop -> prop.sourceFieldName).map(StringUtils::camelToUnderline).collect(Collectors.toList());
            // 添加关联字段
            String relUnderlineCase = StringUtils.camelToUnderline(join.relationProp());
            selectPropList.add(relUnderlineCase);
            queryWrapper.select(selectPropList.toArray(new String[0]));
            queryWrapper.in(relUnderlineCase, propValues);

            List<?> res = baseMapper.selectList(queryWrapper);

            log.debug("关联查询 [{}] 完成. 准备赋值...", join.source().getSimpleName());

            for (Object model : res) {
                Object joinVal = MPUtils.getValue(model, join.relationProp());

                for (Field_Annotation field_annotation : select_prop) {
                    Prop prop = (Prop) field_annotation.annotation;

                    Object propVal = MPUtils.getValue(model, prop.sourcePropName());

                    if (Objects.isNull(propVal)) {
                        continue;
                    }
                    valueObjMap.get(join.relationProp() + "_" + joinVal).forEach(object -> MPUtils.setValue(object, field_annotation.fieldName, propVal));
                }
            }
            log.debug("赋值完毕...");
        }
        log.debug("关联查询 --------------------------- end ");
    }


    private static Set<Object> reInit(Map<String, List<Field_Annotation>> fieldName_fieldAnnotation, Map<String, Set<Object>> fieldValueMap, Map<String, List<Object>> valueObjMap, String joinFieldName, Object[] objects) {
        Set<Object> propValues;
        propValues = Stream.of(objects).map(o -> MPUtils.getValue(o, joinFieldName)).collect(Collectors.toSet());
        fieldValueMap.put(joinFieldName, propValues);

        for (Object object : objects) {
            Object fieldVal = MPUtils.getValue(object, joinFieldName);

            fieldName_fieldAnnotation.get(joinFieldName).forEach(field_annotation -> {
                if (Objects.nonNull(fieldVal)) {
                    String key = ((Join) field_annotation.annotation).relationProp() + "_" + fieldVal;
                    valueObjMap.computeIfAbsent(key, k -> new ArrayList<>()).add(object);
                }
            });
        }
        return propValues;
    }

    /**
     * 缓存需要查询的字段(有相关注解的字段)
     */
    @SneakyThrows
    private static void init(Object object, Collection<Field_Annotation> joinSelectors, Map<String, List<Field_Annotation>> selectPropGroup) {
        Field[] declaredFields = object.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            Join[] joins = field.getDeclaredAnnotationsByType(Join.class);
            if (joins.length != 0) {
                //joinSelectorMap.put(joinSelector.group(), new Field_Annotation(field, joinSelector));
                for (int i = 0; i < joins.length; i++) {
                    joinSelectors.add(new Field_Annotation(field.getName(), joins[i]));
                }
            }

            Prop prop = field.getDeclaredAnnotation(Prop.class);
            if (prop != null) {
                if (prop.sourcePropName().trim().isEmpty()) {
                    editAnnotationValue(prop, "sourcePropName", field.getName());
                }

                if (selectPropGroup.containsKey(prop.group())) {
                    selectPropGroup.get(prop.group()).add(new Field_Annotation(field.getName(), prop));
                } else {
                    selectPropGroup.put(prop.group(), Stream.of(new Field_Annotation(field.getName(), prop)).collect(Collectors.toList()));
                }
            }
        }
    }

    private static void editAnnotationValue(Annotation annotation, String annotationPropName, String val) throws NoSuchFieldException, IllegalAccessException {
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);
        Field value = invocationHandler.getClass().getDeclaredField("memberValues");
        value.setAccessible(true);
        Map<String, Object> memberValues = (Map<String, Object>) value.get(invocationHandler);
        memberValues.put(annotationPropName, val);
    }

    @ToString
    private static class Field_Annotation {

        String fieldName;
        String sourceFieldName;
        Annotation annotation;

        public Field_Annotation(String fieldName, Annotation annotation) {
            this.fieldName = fieldName;
            this.annotation = annotation;

            if (annotation instanceof Prop) {
                String sourceFieldName = ((Prop) annotation).sourcePropName();
                this.sourceFieldName = StringUtils.isNotBlank(sourceFieldName) ? sourceFieldName : this.fieldName;
            }
        }

    }
}
