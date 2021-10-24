package com.example.orchestrator

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestClientConfig {
    @Bean
    fun getRestTemplate(): RestTemplate = RestTemplate()
}