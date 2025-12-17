package com.buyapp.apigateway.config;

import com.netflix.discovery.EurekaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration to make Eureka optional.
 * Gateway will work standalone even if Eureka is unavailable.
 */
@Configuration
@ConditionalOnClass(EurekaClient.class)
public class EurekaConfig {

    private static final Logger logger = LoggerFactory.getLogger(EurekaConfig.class);

    @PostConstruct
    public void init() {
        logger.info("Eureka client is enabled but Gateway can run independently");
        logger.info("If Eureka is unavailable, Gateway will use direct service URLs");
    }
}
