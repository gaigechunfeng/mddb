package com.wk.middleware.annotations;

import java.lang.annotation.*;

/**
 * Created by jince on 2019/1/3.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface OneToMany {

    String unionField() ;
}
