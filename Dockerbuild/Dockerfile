FROM resin/armv7hf-debian:stretch

MAINTAINER info@r2cloud.ru

COPY test.sh /home/pi
COPY ../target/*.deb /home/pi

WORKDIR /home/pi

ENTRYPOINT /home/pi/test.sh