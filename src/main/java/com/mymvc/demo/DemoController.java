package com.mymvc.demo;

import com.mymvc.v1.annotation.MyController;
import com.mymvc.v1.annotation.MyRequestMapping;
import com.mymvc.v1.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * @Author: Zhangdongdong
 * @Date: 2020/11/4 14:53
 */
@MyController
@MyRequestMapping("/demo")
public class DemoController {

    @MyRequestMapping("/demo")
    public void demo(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name")String name) {
        try {
            response.getWriter().write(name);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                response.getWriter().write(Arrays.toString(e.getStackTrace()));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
