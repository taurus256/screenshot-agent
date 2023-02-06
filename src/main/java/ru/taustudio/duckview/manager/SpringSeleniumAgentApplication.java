package ru.taustudio.duckview.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
@EnableEurekaClient
@EnableFeignClients(basePackages= "ru.taustudio.duckview.manager.screenshots")
@EnableScheduling
@EnableAspectJAutoProxy
public class SpringSeleniumAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringSeleniumAgentApplication.class, args);
	}

}
