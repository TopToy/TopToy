#!/bin/bash
cd $(dirname "$0")
#if [[ $1 == "-b" ]] || [[ ! -d "./target" ]]; then
#    rm -r "./target"
#    mvn install
#fi
#cp ${1}/conf/config.toml src/main/resources
#rm -f src/main/resources/sslConfig/*
#cp ${1}/conf/ca.pem src/main/resources/sslConfig/
#cp ${1}/conf/cert.pem src/main/resources/sslConfig/
#cp ${1}/conf/key.pem src/main/resources/sslConfig/

for filename in ${1}/inst/*.inst; do
    java -jar -XX:+UseContainerSupport JToy-1.0-jar-with-dependencies.jar < ${filename}
done