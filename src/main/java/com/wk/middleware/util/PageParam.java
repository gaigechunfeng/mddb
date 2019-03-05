package com.wk.middleware.util;


/**
 * Created by jince on 2018/11/16.
 */
public class PageParam {

    public static final PageParam ALL = new PageParam(1, 1000, 1);
    private int pageCurrent = 1;

    private int pageSize = 10;

    private int pageCount;

    private long recordCount;

    private SortField sort;

    public PageParam() {
    }

    public PageParam(Integer pageCurrent, Integer pageSize, Integer pageCount) {
        this.pageCurrent = pageCurrent <= 0 ? 1 : pageCurrent;
        this.pageSize = pageSize <= 0 ? 10 : pageSize;
        this.pageCount = pageCount;
    }

    public int getPageCurrent() {
        return pageCurrent;
    }

    public void setPageCurrent(int pageCurrent) {
        this.pageCurrent = pageCurrent;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(long recordCount) {
        this.recordCount = recordCount;
    }



    public SortField getSort() {
        return sort;
    }

    public void setSort(SortField sort) {
        this.sort = sort;
    }

    public static class SortField {
        String fieldName;
        Integer sortType; // 0:asc 1:desc

        public SortField() {
        }

        public SortField(String fieldName, Integer sortType) {
            this.fieldName = fieldName;
            this.sortType = sortType;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public Integer getSortType() {
            return sortType;
        }

        public void setSortType(Integer sortType) {
            this.sortType = sortType;
        }
    }
}
