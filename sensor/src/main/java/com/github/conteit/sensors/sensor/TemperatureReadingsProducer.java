package com.github.conteit.sensors.sensor;

import com.github.conteit.sensors.readings.TemperatureReading;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
class TemperatureReadingsProducer {

    private final SensorConfigProps props;

    private final ReactiveKafkaProducerTemplate<String, TemperatureReading> template;

    Mono<Void> spawn() {
        return Flux.interval(Duration.ofSeconds(1))
                   .map(ignore -> createTemperatureReading())
                   .doOnNext(it -> log.info("Produced: {}", it))
                   .flatMap(it -> template.send(props.getTopic(), it.getSensorId(), it)
                                          .then())
                   .then();

    }

    private TemperatureReading createTemperatureReading() {
        return TemperatureReading.newBuilder()
                                 .setSensorId(props.getSensorId())
                                 .setWhen(now())
                                 .setTemperature(randomValue())
                                 .build();
    }

    private long now() {
        return Instant.now()
                      .toEpochMilli();
    }

    private double randomValue() {
        return props.getMinTemp() + Math.random() * props.tempRange();
    }

}
