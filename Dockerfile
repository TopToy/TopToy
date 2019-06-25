# Dockerfile

FROM openjdk:11
MAINTAINER Yehonatan Buchnik <yon_b@cs.technion.ac.il>

ENV TOY_HOME /JToy

RUN mkdir -p ${TOY_HOME}/bin
RUN export PATH="$PATH:${TOY_HOME}/bin"

VOLUME /tmp/JToy
VOLUME ${TOY_HOME}/bin/src/main/resources

ENV ID=0
ENV Type=r
#Copy configuration
COPY bin $TOY_HOME/bin

EXPOSE 9876

ENTRYPOINT ${TOY_HOME}/bin/run_docker.sh ${ID} ${Type}

STOPSIGNAL SIGTERM
