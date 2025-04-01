## About [![Main](https://github.com/dernasherbrezon/r2cloud/actions/workflows/build.yml/badge.svg)](https://github.com/dernasherbrezon/r2cloud/actions/workflows/build.yml) [![Quality Status](https://sonarcloud.io/api/project_badges/measure?project=ru.r2cloud%3Ar2cloud&metric=alert_status)](https://sonarcloud.io/dashboard?id=ru.r2cloud%3Ar2cloud) [![Discussions](https://img.shields.io/badge/discussions-chat-green)](https://github.com/dernasherbrezon/r2cloud/discussions)

r2cloud can track and decode various radio signals from satellites such as:

  - APT (weather satellite)
  - LRPT (weather satellite)
  - Cubesats (FSK, BPSK, QPSK, AFSK, AX.25, AX100 &etc)
  - LoRa
 
## Screenshots ([r2cloud-ui](https://github.com/dernasherbrezon/r2cloud-ui)) 

<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen1.png" width="20%">&nbsp;<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen2.png" width="20%">&nbsp;<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen3.png" width="20%">&nbsp;<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen4.png" width="20%">&nbsp;<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen5.png" width="18%">
 
## Diagram

![diagram](docs/diagram.png)

## Main features

 - r2cloud knows about hundreds of satellites, their orbits, radio channels and communication protocols
 - it can automatically schedule observations for the selected satellites
 - once satellite is within the antenna range, r2cloud will receive the signal, save it and decode it
 - decoded data will be displayed on UI or can be forwarded to the central data warehouses for the analysis

Detailed features could be found in the [documentation](https://github.com/dernasherbrezon/r2cloud/wiki/Features).

## Hardware

The following hardware is required:

 - Antenna
 - SDR receiver
 - *nix-based computer

Please check recommended [bill of materials](https://github.com/dernasherbrezon/r2cloud/wiki/Bill-of-materials). This is very basic setup, but it is guaranteed to be working.

## Software 

1. Install r2cloud
  - From the image. This is the easiest way to install r2cloud on Raspberry PI. It will require brand new SD card:
    - Download the [latest](http://apt.leosatdata.com/dist/image_2024-05-04-r2cloud-lite.zip) official image
    - Insert SD card into the card reader and flash it. You could use [Raspberry PI Imager](https://www.raspberrypi.com/software/) or [Etcher](https://etcher.io) to do this
    - Insert SD card into the card reader and create file ```r2cloud.txt``` in the root directory. This file should contain any random string. This string is a login token. This token will be used during initial setup.
  
  - Or from [repository binaries](https://leosatdata.com/apt.html). Suitable for Ubuntu or Debian:
    - Login via SSH and create ```r2cloud.txt``` file in /boot directory. This file should contain any random string. This string is a login token. This token will be used during initial setup.
    - Execute the following commands:
```
sudo apt-get install curl lsb-release
curl -fsSL https://leosatdata.com/r2cloud.gpg.key | sudo gpg --dearmor -o /usr/share/keyrings/r2cloud.gpg
sudo bash -c "echo 'deb [signed-by=/usr/share/keyrings/r2cloud.gpg] http://apt.leosatdata.com $(lsb_release --codename --short) main' > /etc/apt/sources.list.d/r2cloud.list"
sudo apt-get update
sudo apt-get install r2cloud
```
2. Open [https://raspberrypi.local](https://raspberrypi.local) address.
3. Accept self-signed certificate. This is unique certificate that was generated during installation.

## What to do next?

 1. Configure the station
 2. Select satellites for observation based on their frequency / your personal preferences
 3. Wait for several observations to happen
 4. Analyze the results using "Spectogram" feature and number of frames. The more frames - the better!
 5. Tune your software configuration and hardware setup for better performance
 6. Install rotator to maximize signal strength
 7. Setup additional LNAs or band pass filters 
 8. Help tracking [just launched satellites](https://github.com/dernasherbrezon/r2cloud/wiki/Tracking-newly-launched-satellites)
 9. [Share the data](https://github.com/dernasherbrezon/r2cloud/wiki/LEOSatData) with community using [leosatdata.com](https://leosatdata.com) integrations

## Contribution

Please read our [guidelines](https://github.com/dernasherbrezon/r2cloud/wiki/Contribution).

## Contact

Please follow [@r2cloud1](https://twitter.com/r2cloud1) at twitter to get the latest updates or join conversations at [github discussions](https://github.com/dernasherbrezon/r2cloud/discussions).

## Troubleshooting guide

Frequent errors are combined in the [troubleshooting guide](https://github.com/dernasherbrezon/r2cloud/wiki/Troubleshooting-guide).

