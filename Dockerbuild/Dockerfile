FROM resin/armv7hf-debian:stretch

MAINTAINER info@r2cloud.ru

COPY test.sh /usr/share
COPY *.deb /usr/share
RUN chmod +x /usr/share/test.sh

WORKDIR /usr/share

ENTRYPOINT /usr/share/test.sh