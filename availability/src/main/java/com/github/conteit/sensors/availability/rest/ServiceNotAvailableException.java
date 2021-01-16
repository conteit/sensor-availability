package com.github.conteit.sensors.availability.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
class ServiceNotAvailableException extends RuntimeException {

    public ServiceNotAvailableException(Throwable cause) {
        super(cause);
    }

}