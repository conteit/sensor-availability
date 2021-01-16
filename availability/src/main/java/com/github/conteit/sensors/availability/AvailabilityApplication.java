package com.github.conteit.sensors.availability;

import com.github.conteit.sensors.readings.TemperatureReading;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.function.Function;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(AvailabilityConfigProps.class)
public class AvailabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvailabilityApplication.class, args);
    }

    @Bean
    public Function<KStream<String, TemperatureReading>, KStream<String, Sensor>> availability(
            AvailabilityConfigProps props, @Qualifier("sensorSerde") Serde<Sensor> sensorSerde) {
        return readings -> readings
                .groupByKey()
                .aggregate(TemperatureReadingAggregator::defaultSensor, new TemperatureReadingAggregator(),
                        Materialized.<String, Sensor, KeyValueStore<Bytes, byte[]>>as(props.getStoreName())
                                .withKeySerde(Serdes.String())
                                .withValueSerde(sensorSerde))
                .toStream()
                .peek((sensorId, value) -> log.debug("aggregate: <{},since:{};lastUpdate:{}>", sensorId, value.getSince(), value.getLastReadingAt()))
                .transform(new SensorAvailabilityTransformerSupplier(props), props.getStoreName())
                .peek((sensorId, value) -> log.info("Sensor {} availability={}", sensorId, value.getAvailable()));
    }

    @Bean
    @SuppressWarnings("unchecked")
    @Qualifier("temperatureReadingSerde")
    public Serde<TemperatureReading> temperatureReadingSerde(@Qualifier("streamConfigGlobalProperties") Object streamConfigGlobalProperties) {
        final SpecificAvroSerde<TemperatureReading> serde = new SpecificAvroSerde<>();
        serde.configure((Map<String, Object>) streamConfigGlobalProperties, false);
        return serde;
    }

    @Bean
    @SuppressWarnings("unchecked")
    @Qualifier("sensorSerde")
    public Serde<Sensor> sensorSerde(@Qualifier("streamConfigGlobalProperties") Object streamConfigGlobalProperties) {
        final SpecificAvroSerde<Sensor> serde = new SpecificAvroSerde<>();
        serde.configure((Map<String, Object>) streamConfigGlobalProperties, false);
        return serde;
    }

}
