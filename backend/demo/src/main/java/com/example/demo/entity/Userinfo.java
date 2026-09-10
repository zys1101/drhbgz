package com.example.demo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.catalina.User;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@MyTable
@Data
public class Userinfo {
    private Integer uid ;
    private String username = "张三";
    private String password = "123456";

    public static void main(String[] args) throws Exception{
        Class<Userinfo> clazz = Userinfo.class;
        Annotation[] annotations = clazz.getAnnotations();
        for (Annotation ann : annotations) {
            System.out.println("注解: " + ann);
        }

        Userinfo user = new Userinfo();
        user.setUid(1);
        user.setUsername("李四");
        user.setPassword("654321");

        String tableName = clazz.getSimpleName().toLowerCase();
        Field[] fields = clazz.getDeclaredFields();

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            Object value = field.get(user);

            if (value != null) {
                if (columns.length() > 0) {
                    columns.append(", ");
                    values.append(", ");
                }
                columns.append(field.getName());
                if (value instanceof String) {
                    values.append("'").append(value).append("'");
                } else {
                    values.append(value);
                }
            }
        }
        System.out.println("INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")");

        //        可以得到你类的所有的字段和值
//        Object obj = Userinfo.class.newInstance();
//        Field[] fields = Userinfo.class.getDeclaredFields();
//        for (Field field : fields) {
//            System.out.println(field.getName() );
//            field.setAccessible(true);
//            System.out.println(field.get(obj));
//        }

        //反射是一种动态加载调用类的行为 几乎所有的框架最底层都是使用反射
        //得到类的所有字段
//        Field[] fields = Userinfo.class.getDeclaredFields();//得到当前类的所有字段 反射+设计模式就是核武器
//        for (Field field : fields) {
//            System.out.println(field.getName());
//        }
//        //得到类的所有方法
//        Method[] methods = Userinfo.class.getDeclaredMethods() ;
//        for (Method method : methods) {
//            System.out.println(method.getName());
//        }
//        //得到指定的方法
//        Method eat = Userinfo.class.getDeclaredMethod("eat");
//        Object obj = Userinfo.class.newInstance(); //通过反射调用对方的构造函数创建shili
//        eat.invoke(obj);



    }
}
