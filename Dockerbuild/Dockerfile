FROM r2cloud/it:latest

COPY *.deb /usr/share/

RUN apt-get update && apt install r2cloud-ui  /usr/share/*.deb \
    && apt clean \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /home/pi/r2cloud/bin

COPY *.sh /home/pi/r2cloud/bin/
COPY *.jar /home/pi/r2cloud/lib/
# tests are running under root, thus need to put license file for the root user
COPY .wxtoimglic /root/

RUN chmod +x /home/pi/r2cloud/bin/*.sh

WORKDIR /usr/share

CMD ["bash", "/home/pi/r2cloud/bin/start-services.sh"]