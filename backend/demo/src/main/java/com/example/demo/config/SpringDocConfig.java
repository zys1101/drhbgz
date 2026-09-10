package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//这个类就是swagger和核心配置类
@Configuration
public class SpringDocConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("东软环境监测系统 API")
                        .description("东软环境检测系统后端的 API")
                        .version("1.0.0"));
    }
}
