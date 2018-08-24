# Dockerfile

#FROM phusion/baseimage:0.10.1
FROM ubuntu
MAINTAINER Yehonatan Buchnik <yon_b@cs.technion.ac.il>

#ENV TOY_CONF ${TOY_DATA}/conf
#ENV TOY_LOGS $TOY_DATA/logs
#ENV TOY_INST $TOY_DATA/inst
#ENV TOY_CONF $TOY_DATA/conf


RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" >> /etc/apt/sources.list
RUN apt-get -y update

# Install Maven
#RUN apt-cache search maven
#RUN apt-get -y install maven
#
##Install git
#RUN apt-get install -y git

#Install java 10
RUN apt-get -y install software-properties-common
RUN add-apt-repository ppa:linuxuprising/java
RUN apt-get -y update
RUN echo "oracle-java10-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections
RUN apt-get -y install oracle-java10-installer
RUN apt-get -y install oracle-java10-set-default
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

#Clone the repo (currently with my cradintials!)
#RUN git clone https://yontyon:y8o9ni89@github.com/yontyon/JToy.git

##Compile
#RUN cd $TOY_HOME && mvn compile && mvn install

##Create directories
#RUN mkdir -p ${TOY_LOGS}
##RUN mkdir -p ${TOY_CONF}
#RUN mkdir -p ${TOY_INST}


#exspose rmf port
EXPOSE 20000
#expose bbc port
EXPOSE 15000
#expose panic port
EXPOSE 16000
#expose sync port
EXPOSE 17000

#Create directories
#RUN mkdir -p $TOY_LOGS
#RUN mkdir -p ${TOY_CONF}
#RUN mkdir -p $TOY_INST

#Copy configuration
#ADD $HOST_TOY_DATA/config.toml $TOY_HOME/src/main/resources
#ADD $HOST_TOY_DATA/input.inst $TOY_INST


ARG TOY_BIN
ARG TOY_CONF
ENV TOY_HOME /JToy
ENV TOY_DATA /toy

RUN mkdir $TOY_HOME
RUN mkdir $TOY_DATA

COPY $TOY_BIN $TOY_HOME
COPY $TOY_CONF/ca.pem $TOY_HOME/src/main/resources/sslConfig
COPY $TOY_CONF/cert.pem $TOY_HOME/src/main/resources/sslConfig
COPY $TOY_CONF/key.pem $TOY_HOME/src/main/resources/sslConfig
COPY $TOY_CONF/config.toml $TOY_HOME/src/main/resources

VOLUME ${TOY_DATA}

#RUN mkdir -p $TOY_HOME/src/main/resources
#RUN mkdir -p $TOY_HOME/target
#COPY $HOST_TOY_BIN
#COPY $HOST_TOY_DATA/target $TOY_HOME/target
#COPY $HOST_TOY_DATA/run.sh $TOY_HOME
#COPY $HOST_TOY_DATA/src/main/resources $TOY_HOME/src/main/resources

#ADD $HOST_TOY_DATA/ca.pem $TOY_HOME/src/main/resources/sslConfig
#ADD $HOST_TOY_DATA/cert.pem $TOY_HOME/src/main/resources/sslConfig
#ADD $HOST_TOY_DATA/server.pem $TOY_HOME/src/main/resources/sslConfig

#RUN cd $TOY_HOME && mvn install
#Copy configuration
#COPY ${HOST_TOY_DATA}/config.toml ${TOY_HOME}/src/main/resources
#COPY ${HOST_TOY_DATA}/input.inst ${TOY_INST}
#CMD ["cp -f ${TOY_CONF}/config.toml ${TOY_HOME}/src/main/resources"]
##Compile
#CMD ["./JToy/build.sh"]
#Clean
#CMD ["clear"]
#Run
#CMD "bash"
#CMD "cat ${TOY_INST}/input.inst"
#ENTRYPOINT echo "Hi" >  /JToy/src/main/resources/config.toml
#CMD ["cp", "-f", "/toy/conf/config.toml", "/JToy/src/main/resources/config.toml"]
#CMD ["cat", "/toy/conf/config.toml"]
#CMD cat /JToy/src/main/resources/config.toml
#CMD ["da", "/JToy/src/main/resources/sslConfig/*"]
#CMD ["cp", "/toy/conf/ca.pem /JToy/src/main/resources/sslConfig"]
#CMD ["cp", "/toy/conf/cert.pem /JToy/src/main/resources/sslConfig"]
#CMD ["cp", "/toy/conf/key.pem /JToy/src/main/resources/sslConfig"]
CMD chmod 777 /JToy/run.sh
CMD /JToy/run.sh /toy
# < ${TOY_INST}/input.inst"