package com.mymvc.demo;

import com.mymvc.v1.annotation.MyController;
import com.mymvc.v1.annotation.MyRequestMapping;
import com.mymvc.v1.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: Zhangdongdong
 * @Date: 2020/11/4 14:53
 */
@MyController
@MyRequestMapping("/demo")
public class DemoController {

    @MyRequestMapping("/demo")
    public String demo(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name")String name) {
        return name;
    }
}
