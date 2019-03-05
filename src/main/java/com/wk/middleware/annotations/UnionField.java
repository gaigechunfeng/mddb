package com.wk.middleware.annotations;

import java.lang.annotation.*;

/**
 * <pre>
 *     是否 是表关联字段，设置在字段上，标识该字段是关联字段（该字段类型通常是Set<T>，或者List<T>），
 *     采用DbUtil.findByExample方法查询时会将关联字段信息也一并查询
 * </pre>
 * Created by jince on 2018/11/26.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface UnionField {

    /**
     * 关联的表名
     *
     * @return
     */
    String tableName();

    /**
     * 当前表的关联字段
     *
     * @return
     */
    String unionField();

    /**
     * 关联表的关联字段
     *
     * @return
     */
    String otherField();

    String order() default "";
}
