package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

//所有的SpringBoot的控制器，都必须添加注解
@RestController //只有加了这个注解才能被SpringBoot扫描到
public class TestController {

    @GetMapping("/say")  //将来你也页面的请求地址：http://localhost:8080/say
    //http://127.0.0.1:8080/say
    @Operation(summary = "这是测试方法1",description = "这是测试方法1的描述")
    public String say(){  //这个代码返回的事String
        return "你好，河北理工大学！";
    }

    @GetMapping("/say2")
    @Operation(summary = "这是测试方法2",description = "这是测试方法2的描述")
    public List say2(){
        List list = new ArrayList();
        list.add("张三");
        list.add("李四");
        list.add("王五");
        return list;
    }

    //新版本的Post参数接受，必须要加注解
    //如果你是基础数据类型  @RequestParam
    //如果你是对象类型  @RequestBody  我要把接受的数据全部转成json
    @PostMapping("/save") //这个代码的请求方式是Post 你是不可以在页面直接敲地址
    @Operation(hidden = true) //这个注解是隐藏这个方法
    public String save(@RequestParam("username") String username){ //就是我们将来的增加代码
        return "保存成功"+username;
    }
    //写一个新方法，传入的事对象，而不是字符串
    @PostMapping("/save2")
    @Operation(summary = "增加方法",description = "传递的参数必须是一个JSON")
    //如果你是对象类型  @RequestBody  我要把接受的数据全部转成json
    public User save2(@RequestBody  User user){
        return user;
    }

}
//偷懒，用一种赖皮的方法在此处写一个对象

class User{
    private Integer id;
    private String username;
    private String password;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}