FROM debian:buster
MAINTAINER engineering@armory.io

ARG PLUGIN_ID

ENV PLUGIN_FILE=${PLUGIN_ID}.zip
ENV PLUGIN_ID=${PLUGIN_ID}

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install coreutils git -y
RUN apt-get remove --purge patch -y
RUN mkdir -p /opt/smp

ADD distributions/${PLUGIN_ID}.zip /opt/smp/
ADD distributions/plugins-docker.json /opt/smp/
ADD distributions/install.sh /usr/bin/install-plugin

RUN chmod +x /usr/bin/install-plugin
CMD install-plugin