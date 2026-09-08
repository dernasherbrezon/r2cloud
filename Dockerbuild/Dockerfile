FROM balenalib/armv7hf-debian:bookworm

COPY *.deb /usr/share/

RUN apt-get update \
    && apt install systemd sox r2cloud-ui \
    && apt-get install /usr/share/*.deb \
    && apt clean \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /home/pi/r2cloud-tests/bin \
    && mkdir -p /home/pi/r2cloud-tests/lib \
    && chmod +x /usr/bin/modprobe
    
COPY *.sh /home/pi/r2cloud-tests/bin/
COPY *.jar /home/pi/r2cloud-tests/lib/
# tests are running under root, thus need to put license file for the root user
COPY .wxtoimglic /root/
COPY r2cloud.txt /boot/

RUN chmod +x /home/pi/r2cloud-tests/bin/*.sh

RUN (cd /lib/systemd/system/sysinit.target.wants/; for i in *; do [ $i == systemd-tmpfiles-setup.service ] || rm -f $i; done); \
    rm -f /lib/systemd/system/multi-user.target.wants/*;\
    rm -f /etc/systemd/system/*.wants/*;\
    rm -f /lib/systemd/system/local-fs.target.wants/*; \
    rm -f /lib/systemd/system/sockets.target.wants/*udev*; \
    rm -f /lib/systemd/system/sockets.target.wants/*initctl*; \
    rm -f /lib/systemd/system/basic.target.wants/*;\
    rm -f /lib/systemd/system/anaconda.target.wants/*;    

WORKDIR /usr/share

ENTRYPOINT ["/lib/systemd/systemd"]