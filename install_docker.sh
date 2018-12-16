#!/usr/bin/env bash

toyDir=${PWD}
conf=${toyDir}/Configurations/docker/single
mvn install -DskipTests > /dev/null

#rm -r -f ./bin/src/main/resources/*
#cp -r ${conf}/* ./bin/src/main/resources/

docker build -t toy:0.1 .
