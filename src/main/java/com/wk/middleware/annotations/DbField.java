package com.wk.middleware.annotations;

import java.lang.annotation.*;

/**
 * 标志数据库字段信息
 * Created by jince on 2018/11/23.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DbField {

    /**
     * 字段名称，如果为空则采用实体field名称
     * @return
     */
    String name() default "";

    /**
     * 是否唯一，在新增或者编辑的时候会进行查询判断
     * @return
     */
    boolean unique() default false;

    /**
     * 是否可编辑，在更新实体的时候会过滤
     * @return
     */
    boolean editable() default true;

    /**
     * 是否是树字段，在删除的时候会级联删除子节点
     * @return
     */
    boolean tree() default false;
}
