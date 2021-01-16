package com.github.conteit.sensors.availability;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@NoArgsConstructor
@ConfigurationProperties("availability")
public class AvailabilityConfigProps {

    private String storeName = "sensors";

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration checkPeriod = Duration.ofSeconds(1);

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration expirationPeriod = Duration.ofSeconds(5);

}
