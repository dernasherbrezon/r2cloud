# About [![Build Status](https://travis-ci.org/dernasherbrezon/r2cloud.svg?branch=master)](https://travis-ci.org/dernasherbrezon/r2cloud) [![codecov](https://codecov.io/gh/dernasherbrezon/r2cloud/branch/master/graph/badge.svg)](https://codecov.io/gh/dernasherbrezon/r2cloud) [![Known Vulnerabilities](https://snyk.io/test/github/dernasherbrezon/r2cloud/badge.svg)](https://snyk.io/test/github/dernasherbrezon/r2cloud)

r2cloud converts Raspberry PI into the base station which supports various radio signals, such as:

 - ADS-B (airplane)
 - APT (weather satellite)
 - 2FSK telemetry demodulation for various cubesats
 
# Principal diagram

![diagram](docs/diagram.png)

# Installation

There are 2 ways to install r2cloud:

1. From the image. This is simpliest way and require brand new SD card

  - Download the [latest](https://s3.amazonaws.com/r2cloud/dist/image_2017-09-09-r2cloud-lite.zip) official image
  - Insert SD card into the card reader and flash it. You could use [Etcher](https://etcher.io) to do this
  
2. From repository binaries. Use this way if you have something already installed on SD card.
  - Login via SSH and execute the following commands:
```
sudo gpg --keyserver keyserver.ubuntu.com --recv-keys 129E7CDE
sudo gpg --armor --export 129E7CDE | sudo apt-key add -
sudo bash -c "echo 'deb [arch=armhf] http://s3.amazonaws.com/r2cloud r2cloud main' > /etc/apt/sources.list.d/r2cloud.list"
sudo apt-get update
sudo apt-get install r2cloud
```

Insert SD card into the card reader and create file ```r2cloud.txt``` in the root directory. This file should contain any random string. This string is a login token. This token will be used during initial setup.

Insert SD card into the RPi and open [http://raspberrypi](http://raspberrypi) address.

# Design guidelines

 - Autonomous:
   * Ability to operate without internet connection
   * synchronize state once connection restored
   * automatically calibrate average absolute error
   * configure base station location based on coordinates from GPS receiver
   * new decoders could be added after auto-update
 - Integration with external systems:
   * share as much as possible data with external systems
   * re-use libraries
 - Single stack
   * focus on single hardware and software stack
   * optimize it
 - Stability
   * housekeeping logs and data based on available hard disk
   * auto-update
   * backward compatibility
 - Security
   * safe to expose administration UI to the internet
   
Details could be found in official [documentation](https://github.com/dernasherbrezon/r2cloud/wiki/Features).

# Contribution

Please read our [guidelines](https://github.com/dernasherbrezon/r2cloud/wiki/Contribution).

# Contact

Please use [gitter](https://gitter.im/r2cloud/Lobby)
