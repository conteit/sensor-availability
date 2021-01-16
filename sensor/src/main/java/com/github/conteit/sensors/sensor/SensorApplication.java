package com.github.conteit.sensors.sensor;

import com.github.conteit.sensors.readings.TemperatureReading;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(SensorConfigProps.class)
public class SensorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorApplication.class, args);
    }

    @Bean
    TemperatureReadingsProducer temperatureReadingsProducer(SensorConfigProps props,
                                                            ReactiveKafkaProducerTemplate<String, TemperatureReading> template) {
        return new TemperatureReadingsProducer(props, template);
    }

    @Bean
    ReactiveKafkaProducerTemplate<String, TemperatureReading> reactiveKafkaProducerTemplate(SensorConfigProps props) {
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBrokersList(),
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, props.getSchemaRegistryUrl(),
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, props.getKeySerializer(),
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, props.getValueSerializer()
                )));
    }

    @Bean
    ApplicationRunner applicationRunner(TemperatureReadingsProducer producer) {
        return args -> producer.spawn()
                               .block();
    }

}
