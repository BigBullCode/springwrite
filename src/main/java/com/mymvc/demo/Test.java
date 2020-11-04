package com.mymvc.demo;

/**
 * @Author: Zhangdongdong
 * @Date: 2020/11/4 17:36
 */
public enum Test {

    ABC("abc");

    private String name;

    Test(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
