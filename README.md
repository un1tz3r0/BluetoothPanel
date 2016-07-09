# BluetoothPanel

This is an Android app for controlling hardware (Arduino, Teensy, Raspberry Pi, etc.) projects using your phone or tablet. It uses Bluetooth Serial Port Profile (SPP) which 
is easy to add to any microcontroller project with an unused UART interface. The hardware projects use a simple command-line protocol to export a list of parameters that are 
presented as widgets to the user once the Bluetooth connection is established.

## Features

- The app's user interface reflects the name and type of each parameter as well as its minimum and maximum (if applicable) and the current values.
- The user may interactively adjust the parameters, which causes the changes to be reported over the SPP socket as the interaction is tracked, allowing the hardware to reflect them in real-time.
- The app allows the set of current parameter values to be saved as a named =Preset=. The existing presets can be selected from a list to restore the saved parameter values later.
- Rudimentary rate-limiting is implemented on the app side since writing to the SPP socket too fast for the UART to keep up can overflow the buffer and cause drop characters, resulting in mangled commands and data.
- The app allows choosing from a list of Bluetooth SPP devices to connect to and remembers the chosen device on restart.

## To Do

1. Clean up and refactor code to improve encapsulation and reusability.
2. Document reusable classes and modules.
3. Add context menu for Preset list rows which allows rename and delete (possibly edit?).
4. Add checkable list of Parameter names and values to be included when adding a new Preset.
5. Add more descriptive indeterminate progress indication reflecting different disconnected states
   (device-not-seen, establishing-connection, applying-preset)
  
## Future

There are some deficiencies that should be addressed sooner rather than later.

 1. The bluetooth connection should be handled by a background service so that it can maintain the SPP connection across multiple activities.
 2. Support for notifying the client of asynchronous parameter value changes outside of the `list` command's `begin`...`end` response block. (as in **(fully asynchronous) Two-Way Binding**)
 3. Support for read only and write only (meter and trigger) preferences, enabling and disabling preferences... 
 4. ??? 
 4. Profit.

## Hardware Side

The hardware controlled by this app is intended to be very resource-constrained. 8-bit AVR devices such as that used in the Arduino Uno and Mega, 
as well as 32-bit ARM microcontrollers like the Freescale MK20DX256 that the PJRC Teensy 3.2 is built around are the intended target. For an example
of firmware which implements the remote side of the protocol used by this app, see http://github.com/un1tz3r0/panel.

A major goal of this project was to develop a reusable, self-contained hardware-side library implementing the serial protocol and a clean, compact and 
non-invasive way to specify the list of exported parameters and their types and other metedata, and how to bind them to values in existing code.

The header preferences.hpp in the http://github.com/un1tz3r0/panel works and meets most of these requirements. Using the modern features of C++11/14/17
and compile-time metaprogramming, preferences.hpp allows us to add support with very few lines of code...
