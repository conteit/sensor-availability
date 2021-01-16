package com.github.conteit.sensors.availability.rest;

import com.github.conteit.sensors.availability.AvailabilityConfigProps;
import com.github.conteit.sensors.availability.Sensor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@RequiredArgsConstructor
class SensorsHandler {

    private final AvailabilityConfigProps props;

    private final InteractiveQueryService iqs;

    public Mono<ServerResponse> sensors(ServerRequest request) {
        return ok().body(
                allSensors()
                        .map(it -> it.value)
                        .filter(availabilityFilter(availableOnlyParam(request)))
                        .map(this::toResource), SensorResource.class);
    }

    private Boolean availableOnlyParam(ServerRequest request) {
        return request.queryParam("availableOnly")
                      .map(BooleanUtils::toBoolean)
                      .orElse(false);
    }

    private SensorResource toResource(Sensor it) {
        return SensorResource.builder()
                             .sensorId(it.getSensorId())
                             .available(it.getAvailable())
                             .since(toInstant(it.getSince()))
                             .lastReadingAt(toInstant(it.getLastReadingAt()))
                             .build();
    }

    private Predicate<? super Sensor> availabilityFilter(boolean availableOnly) {
        if (availableOnly) {
            return Sensor::getAvailable;
        }

        return ignore -> true;
    }

    private Instant toInstant(long millis) {
        return Instant.ofEpochMilli(millis);
    }

    private ReadOnlyKeyValueStore<String, Sensor> getStore() {
        return iqs.getQueryableStore(
                props.getStoreName(),
                QueryableStoreTypes.<String, Sensor>keyValueStore());
    }

    private <T> Stream<T> toStream(Iterator<T> iter) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED), false);
    }

    private Flux<KeyValue<String, Sensor>> allSensors() {
        return Flux.fromStream(toStream(getStore().all()));
    }

}
