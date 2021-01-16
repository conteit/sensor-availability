package com.github.conteit.sensors.availability.rest;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor(staticName = "fromList")
public class SensorListResource {

    List<SensorResource> sensors;

}
