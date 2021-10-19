package com.example.orchestrator

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@EnableKafka
@Configuration
class ReviewsConsumerConfig(private val kafkaProperties: KafkaProperties) {

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, String> = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()
        factory.isBatchListener = true
        factory.setConcurrency(kafkaProperties.listener.concurrency)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        return factory
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        return DefaultKafkaConsumerFactory<String, String>(consumerConfigs())
    }

    @Bean
    fun consumerConfigs(): Map<String, Any?> {
        val props = kafkaProperties.buildConsumerProperties()
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        return props
    }
}