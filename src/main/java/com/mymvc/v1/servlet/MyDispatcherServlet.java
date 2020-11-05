package com.mymvc.v1.servlet;


import com.mymvc.v1.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public class MyDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();

    //享元模式缓存
    private List<String> classNames = new ArrayList<String>();

    //初始化IOC容器 key默认是类名首字母小写，value为对应的实例对象
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //v1.0
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();
    //v2.0
    private List<Handler> handlerMapping2 = new ArrayList<Handler>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //初始化完毕后
        //委派，跟据URL去找到一个对应的method并通过response返回
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exection, Detail : " + Arrays.toString(e.getStackTrace()));
        }
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
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取所有字段，包括private,protected,default类型的
            //正常来说，普通的oop编程只能获得public类型的字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) { //只对有autowired注解的属性使用
                    return;
                }
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);

                //如果用户没有注入自定义beanName，默认就根据类型注入
                String beanName = autowired.value().trim(); //未做类名首字母小写的情况判断
                if ("".equals(beanName)) {
                    //如果注解值为空,获取接口的类型做为key，然后用这个key到容器中取值
                    beanName = field.getType().getName();
                }

                //如果是public以外的类型，只要加了@Autowired注解就要强制赋值
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 处理器映射器，利用了策略模式
     */
    private void doInitHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }
            //v1.0
            //保存写在类上面的@MyRequestMapping("/demo")
            /*String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = requestMapping.value();
            }
            
            //默认获取所有的public类型的方法
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                //初始化处理器映射器，并将该url放入映射器
                handlerMapping.put(url, method);
                System.out.println("Mapped :" + url + "," + method);
            }*/

            //v2.0
            String url = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                url = requestMapping.value();
            }
            //获取method的url配置
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //没有加requestMapping的直接忽略
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String regex = ("/" + url + requestMapping.value()).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                handlerMapping2.add(new Handler(pattern, entry.getValue(), method));
                System.out.println("mapping " + regex + "," + method);
            }
        }

    }


    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI();

        String contextPath = req.getContextPath();

        url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 找不到");
            return;
        }
        Method method = handlerMapping.get(url);

        //第一个参数为方法所在的实例，第二个参数为调用时所需要的实参
        //采用动态委派模式进行反射调用，url参数的处理是静态的
       /* Map<String, String[]> params = req.getParameterMap(); //TODO 如何确认接收为此类型的map

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());

        //赋值形参列表，硬编码写死只有一个参数
        method.invoke(ioc.get(beanName), new Object[]{req, resp, params.get("name")[0]});*/

        //url参数的动态获取

        //获取方法的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //保存请求的url参数列表
        Map<String, String[]> parameterMap = req.getParameterMap();
        //保存赋值参数的位置
        Object[] paramValues = new Object[parameterTypes.length];
        //根据参数位置动态赋值
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = req;
                continue;
            }
            else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
                continue;
            }
            else if (parameterType == String.class) {
                //提取方法中加了注解的参数
                Annotation[][] pa = method.getParameterAnnotations();

                for (int j = 0; j < pa.length; j++) {
                    for (Annotation a : pa[i]) {
                        if (a instanceof MyRequestParam) {
                            String paramName = ((MyRequestParam) a).value();
                            if (!"".equals(paramName.trim())) {
                                String value = Arrays.toString(parameterMap.get(paramName)).replaceAll("\\[|\\]", "").replaceAll("\\s", "");
                                paramValues[i] = value;
                            }
                        }

                    }
                }

            }
        }
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), paramValues);
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

    /**
     * spring中handlerMapping是一个List而非Map，list中的元素是自定义类型的。
     * 这个类用来记录Controller中的Requestmapping和Method的对应关系
     */
    private class Handler {
        

        /**
         * 保存方法对应的实例
         */
        protected Object controller;

        /**
         * 保存映射的方法
         */
        protected Method method;

        protected Pattern pattern;
        protected  Map<String, Integer> paramIndexMapping; //参数顺序

        public Handler( Pattern pattern, Object controller, Method method) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {
            
            //提取方法中加了注解的参数
            Annotation[][] pa = method.getParameterAnnotations();

            for (int i = 0; i < pa.length; i++) {
                for (Annotation a : pa[i]) {
                    if (a instanceof MyRequestParam) {
                        String paramName = ((MyRequestParam) a).value();
                        if (!"".equals(paramName.trim())) {
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }
            //提取方法中的request和response参数
            Class<?>[] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length; i++) {
                Class<?> type = paramsTypes[i];
                if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }
    }
}