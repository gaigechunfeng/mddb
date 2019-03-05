package com.wk.middleware.annotations;

import java.lang.annotation.*;

/**
 * 是否允许空值检索，主要用在查询树形结构数据
 * Created by jince on 2018/11/22.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface EmptySearch {
}
