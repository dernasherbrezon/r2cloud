## About [![Build Status](https://app.travis-ci.com/dernasherbrezon/r2cloud.svg?branch=master)](https://app.travis-ci.com/github/dernasherbrezon/r2cloud) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ru.r2cloud%3Ar2cloud&metric=alert_status)](https://sonarcloud.io/dashboard?id=ru.r2cloud%3Ar2cloud) [![Discussions](https://img.shields.io/badge/discussions-chat-green)](https://github.com/dernasherbrezon/r2cloud/discussions)

r2cloud can track and decode various radio signals from satellites such as:

  - APT (weather satellite)
  - LRPT (weather satellite)
  - Cubesats (FSK, BPSK, QPSK, AFSK, AX.25, AX100 &etc)
 
## Screenshots ([r2cloud-ui](https://github.com/dernasherbrezon/r2cloud-ui)) 

<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen1.png" width="20%">&nbsp;<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen2.png" width="20%">&nbsp;<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen3.png" width="20%">&nbsp;<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen4.png" width="20%">&nbsp;<img src="https://github.com/dernasherbrezon/r2cloud/raw/master/docs/screen5.png" width="18%">
 
## Principal diagram

![diagram](docs/diagram.png)

## Assemble base station

Base station might include different hardware components. Please check recommended [bill of materials](https://github.com/dernasherbrezon/r2cloud/wiki/Bill-of-materials). This is very basic setup, but it is guaranteed to be working.

## Installation 

1. Install r2cloud
  - From the image. This is the easiest way to install r2cloud on Raspberry PI. It will require brand new SD card:
    - Download the [latest](http://apt.r2server.ru/dist/image_2020-08-13-r2cloud-lite.zip) official image
    - Insert SD card into the card reader and flash it. You could use [Etcher](https://etcher.io) to do this
    - Insert SD card into the card reader and create file ```r2cloud.txt``` in the root directory. This file should contain any random string. This string is a login token. This token will be used during initial setup.
  
  - Or from [repository binaries](https://r2server.ru/apt.html):
    - Login via SSH and create ```r2cloud.txt``` file in /boot directory. This file should contain any random string. This string is a login token. This token will be used during initial setup.
    - Execute the following commands:
```
sudo apt-get install dirmngr lsb-release
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys A5A70917
sudo bash -c "echo 'deb http://apt.r2server.ru $(lsb_release --codename --short) main' > /etc/apt/sources.list.d/r2cloud.list"
sudo apt-get update
sudo apt-get install r2cloud
```
2. Open [https://raspberrypi.local](https://raspberrypi.local) address.
3. Accept self-signed certificate. This is unique certificate that was generated during installation. Once setup is complete, you could enable proper SSL using [Letsencrypt](https://letsencrypt.org). 

## Main features

  - Autonomous:
    - Ability to operate without internet connection
    - synchronize state once connection restored
    - new decoders could be added after auto-update
  - Integration with external systems:
    - [https://r2server.ru](https://r2server.ru)
    - [SatNOGS](https://satnogs.org)
    - [Amsat TLM](https://www.amsat.org/tlm/leaderboard.php?id=0&db=FOXDB)
    - [Funcube warehouse](http://data.amsat-uk.org/registration)
    - and more
  - Decode satellite signal right after reception
    - full list of supported satellite is available at [jradio](https://github.com/dernasherbrezon/jradio)
    - display images if they supported by the satellite
  - Multiple devices
    - [RTL-SDR](https://www.rtl-sdr.com/buy-rtl-sdr-dvb-t-dongles/)
    - [PlutoSDR](https://github.com/dernasherbrezon/r2cloud/wiki/PlutoSDR)
    - [sdr-server](https://github.com/dernasherbrezon/r2cloud/wiki/sdr-server)
    - [LoRa via r2lora](https://github.com/dernasherbrezon/r2cloud/wiki/LoRa)
  - Security
    - safe to expose administration UI to the internet
   
Detailed features could be found in the [documentation](https://github.com/dernasherbrezon/r2cloud/wiki/Features).

## Contribution

Please read our [guidelines](https://github.com/dernasherbrezon/r2cloud/wiki/Contribution).

## Contact

Please follow [@r2cloud1](https://twitter.com/r2cloud1) at twitter to get the latest updates or join conversations at [github discussions](https://github.com/dernasherbrezon/r2cloud/discussions).

## Troubleshooting guide

Frequent errors are combined in the [troubleshooting guide](https://github.com/dernasherbrezon/r2cloud/wiki/Troubleshooting-guide).

