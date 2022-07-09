package com.ryou.tomcat.http.codec;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/28 9:49
 */
public enum BodyForm {

    X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),

    APPLICATION_JSON("application/json"),

    FORM_DATA("multipart/form-data");

    private final String form;

    BodyForm(String form) {
        this.form = form;
    }
    // get set 方法
    public String getForm() {
        return form;
    }

}
