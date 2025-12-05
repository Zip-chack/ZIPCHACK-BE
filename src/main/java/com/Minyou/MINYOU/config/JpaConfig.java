package com.Minyou.MINYOU.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.Minyou.MINYOU.repository")
@EntityScan(basePackages = "com.Minyou.MINYOU.entity")
public class JpaConfig {
}

