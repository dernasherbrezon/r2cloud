FROM r2cloud/it:latest

COPY *.deb /usr/share/

RUN apt-get update && apt-get install /usr/share/*.deb \
    && apt clean \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /home/pi/r2cloud/bin

COPY *.sh /home/pi/r2cloud/bin/
COPY *.jar /home/pi/r2cloud/lib/
# tests are running under root, thus need to put license file for the root user
COPY .wxtoimglic /root/
COPY r2cloud.txt /boot/

RUN chmod +x /home/pi/r2cloud/bin/*.sh \
    && echo "127.0.0.1 raspberrypi.local" >> /etc/hosts \
    && ps aux | grep avahi && ping -c 3 raspberrypi.local

WORKDIR /usr/share

CMD ["bash", "/home/pi/r2cloud/bin/start-services.sh"]