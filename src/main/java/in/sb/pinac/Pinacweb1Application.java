package in.sb.pinac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Pinacweb1Application {

    public static void main(String[] args) {

        System.out.println("PINAC Web Application Started...");

        SpringApplication.run(Pinacweb1Application.class, args);

        System.out.println("Spring Boot Application Running Successfully!");
    }
}