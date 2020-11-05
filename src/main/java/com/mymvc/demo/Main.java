package com.mymvc.demo;

import java.util.regex.Pattern;

/**
 * @Author: Zhangdongdong
 * @Date: 2020/11/4 17:36
 */
public class Main {

    public static void main(String[] args) {
        System.out.println(Test.ABC.name());

        String url = "//demo///demo";
        String regex = ("/" + url).replaceAll("/+", "/");
        Pattern pattern = Pattern.compile(regex);
        System.out.println(pattern);
    }
}
