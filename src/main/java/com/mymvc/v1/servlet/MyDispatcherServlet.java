package com.mymvc.v1.servlet;


import com.mymvc.v1.annotation.MyController;
import com.mymvc.v1.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();

    //享元模式缓存
    private List<String> classNames = new ArrayList<>();

    //初始化IOC容器 key默认是类名首字母小写，value为对应的实例对象
    private Map<String, Object> ioc = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //初始化完毕后
        //委派，跟据URL去找到一个对应的method并通过response返回
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //初始化IOC容器，将扫描到的相关的类的实例化保存到IOC容器中 --IOC部分
        doInstance();

        //在DI之前，进行AOP，因为是新生成的代理对象

        //完成依赖注入  DI部分
        doAutowired();

        //初始化handlerMapping  MCV部分
        doInitHandlerMapping();

        //Spring 初始化完毕
        System.out.printf("spring 初始化完成！");
    }


    /**
     *
     * @param contextConfigLocation web.xml内的param-name标签
     */
    private void doLoadConfig(String contextConfigLocation) {
        //通过类读取相应文件，并转化为流
        InputStream is =
                this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            //将流转化为properties
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.printf("doLoadConfig end ---");

    }

    /**
     *
     * @param scanPackage properties文件的scanPackage属性
     */
    private void doScanner(String scanPackage) {
        //将properties的scanPackage属性的值转为/的路径格式
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            }
            else {
                if (file.getName().endsWith(".class")){
                    String className = (scanPackage + "." + file.getName()).replace(".class", ""); //获取方法名,用于反射
                    //声明一个容器保存className
                    classNames.add(className);
                }
            }
        }
        System.out.printf("doScanner end ---");
    }

    private void doInstance() {

        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className: classNames) {

                Class<?> clazz = Class.forName(className);

                //增加一个判断，保证有响应的注解，才会被IOC
                if (clazz.isAnnotationPresent(MyController.class)) {
                    //自定义IOC容器 key为类名的首字母小写
                    String simpleName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(simpleName, clazz.newInstance());
                }
                else if (clazz.isAnnotationPresent(MyService.class)) {
                    //存在三种情况
                    //2.在多个包下出现相同的类名，只能自己起Service名称
                    String beanName = clazz.getAnnotation(MyService.class).value();
                    if ("".equals(beanName.trim())) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    //1.默认的类名首字母小写
                    ioc.put(beanName, clazz.newInstance());

                    //3.如果是接口被实现了多次，判断多少个实现类，如果是一个，就默认选择，如果有多个，抛异常
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i:interfaces) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The" + i.getName() + " is exists!");
                        }
                        ioc.put(i.getName(), clazz.newInstance());
                    }
                }
                else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void doAutowired() {
    }

    private void doInitHandlerMapping() {
    }

    /**
     * 第一个字母转为小写
     * @param simpleName
     * @return
     */
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}