package com.wk.middleware.entities;


import com.wk.middleware.annotations.DbField;

import java.util.Date;

/**
 * 基类
 * Created by jince on 2018/11/21.
 */
public abstract class Base {

    @DbField(editable = false)
    private Date crtime;
    @DbField(editable = false)
    private String cruser;

    public Date getCrtime() {
        return crtime;
    }

    public void setCrtime(Date crtime) {
        this.crtime = crtime;
    }

    public String getCruser() {
        return cruser;
    }

    public void setCruser(String cruser) {
        this.cruser = cruser;
    }
}
