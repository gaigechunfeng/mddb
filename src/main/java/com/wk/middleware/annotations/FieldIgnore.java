package com.wk.middleware.annotations;

import java.lang.annotation.*;

/**
 * 数据库操作时是否忽视该字段
 * Created by jince on 2018/11/21.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface FieldIgnore {
}
