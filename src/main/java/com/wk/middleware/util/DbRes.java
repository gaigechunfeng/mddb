package com.wk.middleware.util;

import java.util.List;

/**
 * Created by jince on 2018/11/16.
 */
public class DbRes<T> {

    private List<T> list;
    private PageParam pageParam;

    public DbRes() {
    }

    public DbRes(List<T> list, PageParam pageParam) {
        this.list = list;
        this.pageParam = pageParam;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public PageParam getPageParam() {
        return pageParam;
    }

    public void setPageParam(PageParam pageParam) {
        this.pageParam = pageParam;
    }
}
