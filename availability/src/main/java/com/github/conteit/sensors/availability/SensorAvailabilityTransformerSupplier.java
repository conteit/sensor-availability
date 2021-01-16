package com.github.conteit.sensors.availability;

import com.github.conteit.sensors.readings.TemperatureReading;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;

@RequiredArgsConstructor
public class SensorAvailabilityTransformerSupplier implements TransformerSupplier<String, Sensor, KeyValue<String, Sensor>> {

    private final AvailabilityConfigProps props;

    @Override
    public Transformer<String, Sensor, KeyValue<String, Sensor>> get() {
        return new SensorAvailabilityTransformer(props);
    }

}
