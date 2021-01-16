package com.github.conteit.sensors.availability.rest;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SensorResource {

    String sensorId;

    boolean available;

    Instant since;

    Instant lastReadingAt;

}
