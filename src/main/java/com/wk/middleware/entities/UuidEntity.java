package com.wk.middleware.entities;


import com.wk.middleware.annotations.TableId;

/**
 * Created by jince on 2018/11/23.
 */
public abstract class UuidEntity extends Base {

    @TableId
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
