package io.yggdrasil.labs.midgard.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Starter
 *
 * @author YoungerYang-Y
 */
@SpringBootApplication(scanBasePackages = {"io.yggdrasil.labs.midgard", "com.alibaba.cola"})
@MapperScan("io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.mapper")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
