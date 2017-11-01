FROM resin/armv7hf-debian:stretch

ENV INITSYSTEM=on
#ENV RESIN_DEVICE_UUID=r2cloud
#ENV RESIN=on
#ENV RESIN_DEVICE_TYPE=generic
COPY test.sh /usr/share
COPY *.deb /usr/share
RUN chmod +x /usr/share/test.sh \
    && gpg --keyserver keyserver.ubuntu.com --recv-keys 129E7CDE \
    && gpg --armor --export 129E7CDE | sudo apt-key add - \
    && bash -c "echo 'deb [arch=armhf] http://s3.amazonaws.com/r2cloud r2cloud main' > /etc/apt/sources.list.d/r2cloud.list" \
    && apt-get update && apt install /usr/share/*.deb \
    && apt clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /usr/share

CMD ["bash", "/usr/share/test.sh"]