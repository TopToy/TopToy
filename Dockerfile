# Dockerfile

FROM openjdk:11
MAINTAINER Yehonatan Buchnik <yon_b@cs.technion.ac.il>

ENV TOY_HOME /JToy

RUN mkdir -p ${TOY_HOME}/bin
RUN export PATH="$PATH:${TOY_HOME}/bin"

VOLUME /tmp/JToy
VOLUME ${TOY_HOME}/bin/src/main/resources

##exspose wrb port
#EXPOSE 30000
###expose panic port
#EXPOSE 11000
###expose sync port
#EXPOSE 13000
###expose bbc port
#EXPOSE 12000

#expose Clients port
#EXPOSE 14000
ENV ID=0
ENV Type=r
#Copy configuration
COPY bin $TOY_HOME/bin

#ENTRYPOINT ["chmod 777 ${TOY_HOME}/bin/run_docker.sh", "./${TOY_HOME}/bin/run_docker.sh"]
#CMD echo ${ID}
ENTRYPOINT ${TOY_HOME}/bin/run_docker.sh ${ID} ${Type}
#CMD cat ${TOY_HOME}/bin/src/main/resources/inst/input.inst
STOPSIGNAL SIGTERM
