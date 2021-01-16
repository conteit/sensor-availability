# Sensor Availability

This is an experimental test project that I have created in order to find a way for using temperature sensor readings in order to determine if sensor is still operational (available) or not, by exploiting Apache Kafka capabilities.

## Usage

* Clone this repo and run `mvn package`
* Configure the **availability** application in order to find a Kafka broker and the Schema Registry, then launch it
* Configure the **sensor** application in order to find a Kafka broker and the Schema Registry, then launch multiple instances of it
  * If no `sensor.sensor-id` property is set, a new sensorId is auto-generated each time the application is started
  * In order to simulate a sensor that goes away and comes back disable the auto-generation by giving a vaule to `sensor.sensor-id`


## Acknowledgements

The idea for the solution came from https://medium.com/@cbenaveen/high-available-task-scheduling-design-using-kafka-and-kafka-streams-dcd81121d43b.
