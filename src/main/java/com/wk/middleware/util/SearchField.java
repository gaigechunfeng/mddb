package com.wk.middleware.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jince on 2018/11/19.
 */
public class SearchField {

	private String fieldName;
	private String fieldOpr;
	private List<String> fieldVal = new ArrayList<>();
	private String fieldUnion;
	private String fieldType;

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFieldOpr() {
		return fieldOpr;
	}

	public void setFieldOpr(String fieldOpr) {
		this.fieldOpr = fieldOpr;
	}

	public List<String> getFieldVal() {
		return fieldVal;
	}

	public void setFieldVal(List<String> fieldVal) {
		this.fieldVal = fieldVal;
	}

	public String getFieldUnion() {
		return fieldUnion;
	}

	public void setFieldUnion(String fieldUnion) {
		this.fieldUnion = fieldUnion;
	}

	public String getFieldType() {
		return fieldType;
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

}
