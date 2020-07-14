FROM r2cloud/it:latest

COPY *.deb /usr/share/

RUN apt-get update && apt-get install /usr/share/*.deb \
    && apt clean \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /home/pi/r2cloud-tests/bin \
    && mkdir -p /home/pi/r2cloud-tests/lib

COPY *.sh /home/pi/r2cloud-tests/bin/
COPY *.jar /home/pi/r2cloud-tests/lib/
# tests are running under root, thus need to put license file for the root user
COPY .wxtoimglic /root/
COPY r2cloud.txt /boot/

RUN chmod +x /home/pi/r2cloud-tests/bin/*.sh

WORKDIR /usr/share

CMD ["bash", "/home/pi/r2cloud-tests/bin/start-services.sh"]