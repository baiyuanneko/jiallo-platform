package moe.byn.bynspring21.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class Fastjson2Config {
    static {
        com.alibaba.fastjson2.JSONFactory.setUseJacksonAnnotation(false);
    }
}
