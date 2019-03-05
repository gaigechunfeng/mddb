package com.wk.middleware.annotations;

import java.lang.annotation.*;

/**
 * 标志controller中的处理请求方法是否需要注入 权限字段 信息，搭配PageParam使用，用来接收注入的权限字段
 * Created by jince on 2018/11/28.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TablePerm {

    /**
     * 需要注入哪个表的 权限字段信息
     *
     * @return
     */
    String tableName();
}
