package com.wk.middleware.annotations;

import java.lang.annotation.*;

/**
 * 表示实体对应数据库中一个表
 * Created by jince on 2018/11/21.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Table {

    /**
     * 数据库表名
     *
     * @return
     */
    String name();

    /**
     * 默认排序字段
     *
     * @return
     */
    String orderBy() default "";

}
