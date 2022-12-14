package ru.taustudio.duckview.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
@EnableEurekaClient
@EnableFeignClients(basePackages="ru.taustudio.duckview.agent.screenshots")
public class SpringSeleniumAgentApplication {

	public static void main(String[] args) {

		SpringApplication.run(SpringSeleniumAgentApplication.class, args);
	}

}
