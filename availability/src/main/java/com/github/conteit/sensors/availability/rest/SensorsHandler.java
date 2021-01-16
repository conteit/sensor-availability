package com.github.conteit.sensors.availability.rest;

import com.github.conteit.sensors.availability.AvailabilityConfigProps;
import com.github.conteit.sensors.availability.Sensor;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.web.reactive.function.client.WebClient;
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

@Slf4j
@RequiredArgsConstructor
class SensorsHandler {

    public static final String HEADER_X_STATE_STORE_SCOPE_LOCAL_VALUE = "Local";

    private static final String QUERY_PARAM_AVAILABLE_ONLY = "availableOnly";

    private static final String HEADER_X_STATE_STORE_SCOPE = "X-StateStoreScope";

    private final AvailabilityConfigProps props;

    private final InteractiveQueryService iqs;

    public Mono<ServerResponse> sensors(ServerRequest request) {
        return ok().body(
                fetchFromStore(request, availabilityFilter(availableOnlyParam(request)))
                        .collectList()
                        .map(SensorListResource::fromList), SensorListResource.class);
    }

    private Flux<SensorResource> fetchFromStore(ServerRequest request, Predicate<SensorResource> predicate) {
        return applyFilter(isLocalStoreRequest(request)
                           ? allSensorsInLocalStore()
                           : allSensors(), predicate);
    }

    private Flux<SensorResource> applyFilter(Flux<SensorResource> sensorResourceFlux, Predicate<SensorResource> predicate) {
        return sensorResourceFlux.filter(predicate);
    }

    private Flux<SensorResource> allSensors() {
        return storeReplicas()
                .flatMap(hostInfo -> isLocalReplica(hostInfo)
                                     ? allSensorsInLocalStore()
                                     : allSensorsFromRemote(hostInfo));
    }

    private Flux<SensorResource> allSensorsFromRemote(HostInfo hostInfo) {
        return WebClient.create(String.format("http://%s:%s/", hostInfo.host(), hostInfo.port()))
                        .get()
                        .uri("/")
                        .header("X-StateStoreScope", "Local")
                        .retrieve()
                        .bodyToMono(SensorListResource.class)
                        .map(SensorListResource::getSensors)
                        .flatMapMany(Flux::fromIterable);
    }

    private boolean isLocalReplica(HostInfo hostInfo) {
        return iqs.getCurrentHostInfo()
                  .equals(hostInfo);
    }

    private Flux<HostInfo> storeReplicas() {
        return Flux.fromStream(iqs.getAllHostsInfo(props.getStoreName())
                                  .stream());
    }

    private boolean isLocalStoreRequest(ServerRequest request) {
        return request.headers()
                      .header(HEADER_X_STATE_STORE_SCOPE)
                      .contains(HEADER_X_STATE_STORE_SCOPE_LOCAL_VALUE);
    }

    private Boolean availableOnlyParam(ServerRequest request) {
        return request.queryParam(QUERY_PARAM_AVAILABLE_ONLY)
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

    private Predicate<SensorResource> availabilityFilter(boolean availableOnly) {
        if (availableOnly) {
            return SensorResource::isAvailable;
        }

        return ignore -> true;
    }

    private Instant toInstant(long millis) {
        return Instant.ofEpochMilli(millis);
    }

    private ReadOnlyKeyValueStore<String, Sensor> getStore() {
        return Try.of(() -> iqs.getQueryableStore(
                props.getStoreName(),
                QueryableStoreTypes.<String, Sensor>keyValueStore()))
                .getOrElseThrow(ServiceNotAvailableException::new);
    }

    private <T> Stream<T> toStream(Iterator<T> iter) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED), false);
    }

    private Flux<SensorResource> allSensorsInLocalStore() {
        return Flux.fromStream(toStream(getStore().all()))
                   .map(it -> it.value)
                   .map(this::toResource);
    }

}
