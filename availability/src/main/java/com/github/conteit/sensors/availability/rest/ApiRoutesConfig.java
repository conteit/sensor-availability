package com.github.conteit.sensors.availability.rest;

import com.github.conteit.sensors.availability.AvailabilityConfigProps;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class ApiRoutesConfig {

    @Bean
    RouterFunction<?> routes(SensorsHandler handler) {
        return route(GET("/"), handler::sensors);
    }

    @Bean
    SensorsHandler sensorsHandler(AvailabilityConfigProps props, InteractiveQueryService iqs) {
        return new SensorsHandler(props, iqs);
    }

}
