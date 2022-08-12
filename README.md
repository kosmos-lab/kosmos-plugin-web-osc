# KosmoS Web OSC Plugin


## Description
This is a simple web ui for opensoundcontrol enabled devices.


## Tested with
* Behringer X32(Rack)
* Behringer XR-18

## Configuration
Configuration needs to be placed in config/osc.
There are currently 2 Examples for Behringer X32 and Behringer XR devices. (only differ in the port used)


## compile yourself

If you want to compile it yourself you can do so with

```shell
mvn clean package
```

This should create a zip in the target folder.

Just drop this zip file to the "plugins" folder of the KosmoS platform.










