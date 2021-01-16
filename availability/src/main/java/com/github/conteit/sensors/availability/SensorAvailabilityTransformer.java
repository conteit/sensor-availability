package com.github.conteit.sensors.availability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class SensorAvailabilityTransformer
        implements Transformer<String, Sensor, KeyValue<String, Sensor>>, Punctuator {

    private final AvailabilityConfigProps props;

    private KeyValueStore<String, ValueAndTimestamp<Sensor>> store;

    private Runnable committer;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        this.committer = context::commit;
        this.store = (KeyValueStore<String, ValueAndTimestamp<Sensor>>) context.getStateStore(props.getStoreName());
        context.schedule(props.getCheckPeriod(), PunctuationType.WALL_CLOCK_TIME, this);
    }

    @Override
    public KeyValue<String, Sensor> transform(String sensorId, Sensor sensor) {
        if (justChangedAvailabilityOrNull(sensor)) {
            log.debug("transform: {}", sensor);
            return KeyValue.pair(sensorId, sensor);
        }

        return null;
    }

    private Boolean justChangedAvailabilityOrNull(Sensor sensor) {
        return Optional.ofNullable(sensor)
                       .map(this::justChangedAvailability)
                       .orElse(false);
    }

    private boolean justChangedAvailability(@NonNull Sensor sensor) {
        return sensor.getSince() >= sensor.getLastReadingAt();
    }

    @Override
    public void punctuate(long timestamp) {
        checkForExpiredSensors(timestamp);
        commit();
    }

    private void checkForExpiredSensors(long timestamp) {
        try (var iter = store.all()) {
            final var oldestAllowed = computeOldestUpdateAllowedInstant(timestamp);
            while (iter.hasNext()) {
                markAsNotAvailableIfExpired(iter.next(), oldestAllowed, timestamp);
            }
        }
    }

    private Instant computeOldestUpdateAllowedInstant(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                      .minus(props.getExpirationPeriod());
    }

    private void markAsNotAvailableIfExpired(KeyValue<String, ValueAndTimestamp<Sensor>> entry,
                                             Instant oldestAllowed, long timestamp) {
        final var sensorId = entry.key;
        final var sensor = entry.value.value();
        if (sensor.getAvailable() && isExpired(oldestAllowed, sensor)) {
            store.put(sensorId, ValueAndTimestamp.make(notAvailable(sensor, timestamp), timestamp));
        }
    }

    private Sensor notAvailable(Sensor oldState, long timestamp) {
        return Sensor.newBuilder(oldState)
                     .setAvailable(false)
                     .setSince(timestamp)
                     .build();
    }

    private boolean isExpired(Instant oldestAllowed, Sensor sensor) {
        return toInstant(sensor.getLastReadingAt()).isBefore(oldestAllowed);
    }

    private Instant toInstant(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis);
    }

    private void commit() {
        Optional.ofNullable(committer)
                .ifPresent(Runnable::run);
    }

    @Override
    public void close() {

    }

}
