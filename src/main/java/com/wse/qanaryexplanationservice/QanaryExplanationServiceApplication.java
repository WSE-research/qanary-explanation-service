package com.wse.qanaryexplanationservice;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan
public class QanaryExplanationServiceApplication {

    private final Environment env;

    public QanaryExplanationServiceApplication(@Autowired Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(QanaryExplanationServiceApplication.class, args);
    }


    @Bean
    public OpenAPI customOpenAPI(
            @Value("${springdoc.version}") String appVersion, //
            @Value("${spring.application.name}") String appName //
    ) {
        return new OpenAPI().info(new Info()
                .title(appName) //
                .version(appVersion) //
                .description(
                        "OpenAPI 3 with Spring Boot provided this API documentation.")
                .termsOfService("http://swagger.io/terms/") //
                .license(new License().name("Apache 2.0").url("http://springdoc.org")) //
        );
    }
}
