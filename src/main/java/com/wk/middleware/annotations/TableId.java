package com.wk.middleware.annotations;

import java.lang.annotation.*;

/**
 * 标志字段是主键字段
 * Created by jince on 2018/11/21.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface TableId {
}
