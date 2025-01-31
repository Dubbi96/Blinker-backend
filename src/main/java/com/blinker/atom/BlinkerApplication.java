package com.blinker.atom;

import java.util.Locale;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BlinkerApplication {

    @PostConstruct
    public void started(){
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        Locale.setDefault(Locale.KOREA);
    }
    public static void main(String[] args) {
        SpringApplication.run(BlinkerApplication.class, args);
    }

}
