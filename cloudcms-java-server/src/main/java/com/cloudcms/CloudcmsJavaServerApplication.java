/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class CloudcmsJavaServerApplication {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(CloudcmsJavaServerApplication.class, args);

        final Logger log = LoggerFactory.getLogger(CloudcmsJavaServerApplication.class.getClass());
        if (log.isDebugEnabled()) {
            for (String name : applicationContext.getBeanDefinitionNames()) {
                log.debug(name);
            }
        }
    }
}
