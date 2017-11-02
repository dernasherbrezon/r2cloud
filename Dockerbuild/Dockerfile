FROM resin/armv7hf-debian:stretch

ENV INITSYSTEM=on

COPY *.deb /usr/share/

RUN gpg --keyserver keyserver.ubuntu.com --recv-keys 129E7CDE \
    && gpg --armor --export 129E7CDE | sudo apt-key add - \
    && bash -c "echo 'deb [arch=armhf] http://s3.amazonaws.com/r2cloud r2cloud main' > /etc/apt/sources.list.d/r2cloud.list" \
    && apt-get update && apt install /usr/share/*.deb openjdk-8-jdk \
    && apt clean \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /home/pi/r2cloud/bin

COPY *.sh /home/pi/r2cloud/bin/
COPY *.jar /home/pi/r2cloud/lib/
COPY .wxtoimglic /home/pi/

RUN chmod +x /home/pi/r2cloud/bin/*.sh

WORKDIR /usr/share

CMD ["bash", "/home/pi/r2cloud/bin/start-services.sh"]