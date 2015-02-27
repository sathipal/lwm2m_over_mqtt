# LWM2M over MQTT
### Introduction
> OMA Lightweight M2M is a protocol for device and service management. The main purpose of this 
> technology is to address service and management needs for constrained M2M devices, over a number
> of transports and bearers. The current stack for LWM2M relies on CoAP as the protocol.

Our solution involves development of an LWM2M server prototype, as well as, a client prototype, which make use of MQTT as the underlying M2M protocol. Thus LWM2M can be used for both CoAP, as well as, MQTT.

In this project we are using
  - Leshan code 
  - Eclipse Paho client library to interact with MQTT broker
  - An existing MQTT broker

---
### Deliverables
In the first phase, we are providing the following LwM2M operations, in plain-text format
  - Registration interface
    -   register
    -   update
    -   de-register
  - Device Management & Service Enablement Interface
    - read
    - write
    - write-attributes
    - execute
    - create
    - delete
    - deregister
  - Information Reporting Interface 
    - observe
    - notify
    - cancel-observation

---
### Version
0.0.1

---
### Technology

LWM2M over MQTT makes use of a number of open source projects to work properly:

* [Eclipse] 
* [Paho Client] - Open source client implementation for M2M / IoT
* [Leshan] - OMA LwM2M implementation in java
* [Open Source Mosquitto Broker] - This MQTT broker is hosted at eclipse 
* [git repository] - Githubs repository where the artifacts are stored

---
### Installation

You need to install git client

```sh
$ git clone [git repository] lwm2m
$ cd lwm2m
$ ant build.xml
```

---
### Todo's
 - Bootstrap Interface
 - Security
 - Support TLV, Opaque and JSON message format
 - Binding MQTT in Leshan client
 - Write Tests
 - Add Code Comments

---
###License
 - Eclipse Distribution License
 - Eclipse Public License 

---

[eclipse]:http://www.eclipse.org/
[Paho Client]:http://twitter.com/thomasfuchs
[Leshan]:https://github.com/eclipse/leshan
[Open Source Mosquitto Broker]:http://iot.eclipse.org/sandbox.html
[git repository]:http://github.com/sathipal/lwm2m_over_mqtt
