package org.xu.newjob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
public class NewJobApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewJobApplication.class, args);
	}

}
