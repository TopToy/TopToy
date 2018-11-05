# Dockerfile

FROM openjdk:10
MAINTAINER Yehonatan Buchnik <yon_b@cs.technion.ac.il>

ENV TOY_HOME /JToy

RUN mkdir -p ${TOY_HOME}
RUN mkdir -p ${TOY_HOME}/bin

#RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" >> /etc/apt/sources.list
#RUN apt-get -y update
#
##Install java 10
#RUN apt-get -y install software-properties-common
#RUN add-apt-repository ppa:linuxuprising/java
#RUN apt-get -y update
#RUN echo "oracle-java11-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections
#RUN apt-get -y install oracle-java11-installer
#RUN apt-get -y install oracle-java11-set-default
#RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

VOLUME /tmp/JToy
VOLUME ${TOY_HOME}/bin/src/main/resources
#exspose rmf port
#EXPOSE 30000
##expose panic port
#EXPOSE 11000
##expose sync port
#EXPOSE 13000
##expose bbc port
#EXPOSE 12000
#expose Clients port
EXPOSE 14000

#Copy configuration
COPY ./bin/ $TOY_HOME/bin/

ARG id
ARG type

RUN chmod 777 ${TOY_HOME}/bin/run_docker.sh
#CMD './JToy/bin/run_docker.sh 0 "r"'
CMD "bash"
#RUN ["java", "-jar", "~/JToy/bin/JToy-1.0-jar-with-dependencies.jar", "0", "r"]