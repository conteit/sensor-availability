package com.github.conteit.sensors.sensor;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@NoArgsConstructor
@ConfigurationProperties("sensor")
public class SensorConfigProps {

    private String brokersList = "localhost:9092";

    private String schemaRegistryUrl = "http://localhost:8081";

    private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";

    private String valueSerializer = "io.confluent.kafka.serializers.KafkaAvroSerializer";

    private String sensorId = UUID.randomUUID()
                                  .toString();

    private String topic = "temperature-readings";

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration readingPeriod = Duration.ofSeconds(1);

    private double minTemp = -10.0;

    private double maxTemp = 40.0;

    double tempRange() {
        return maxTemp - minTemp;
    }

}
