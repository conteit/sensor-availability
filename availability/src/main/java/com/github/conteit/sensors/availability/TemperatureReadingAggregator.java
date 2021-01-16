package com.github.conteit.sensors.availability;

import com.github.conteit.sensors.readings.TemperatureReading;
import org.apache.kafka.streams.kstream.Aggregator;

class TemperatureReadingAggregator implements Aggregator<String, TemperatureReading, Sensor> {

    static Sensor defaultSensor() {
        return Sensor.newBuilder()
                     .setSensorId("")
                     .setAvailable(false)
                     .setSince(0L)
                     .setLastReadingAt(0L)
                     .build();
    }

    @Override
    public Sensor apply(String sensorId, TemperatureReading temperatureReading, Sensor oldState) {
        return Sensor.newBuilder(oldState)
                     .setSensorId(sensorId)
                     .setAvailable(true)
                     .setLastReadingAt(temperatureReading.getWhen())
                     .setSince(updateSinceIfWasNotAvailable(temperatureReading, oldState))
                     .build();
    }

    private long updateSinceIfWasNotAvailable(TemperatureReading temperatureReading, Sensor oldState) {
        if (!oldState.getAvailable()) {
            return temperatureReading.getWhen();
        }

        return oldState.getSince();
    }

}
