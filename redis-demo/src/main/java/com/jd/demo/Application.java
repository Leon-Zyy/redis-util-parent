package com.jd.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Application
 *
 * @author Y.Y.Zhao
 */
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        log.info("start");
        new ClassPathXmlApplicationContext("classpath:spring/spring-main.xml");
        log.info("success");
    }
}
