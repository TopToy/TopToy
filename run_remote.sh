#!/usr/bin/env bash

toyHome=JToy

mkdir -p /tmp/JToy/logs
mkdir -p /tmp/JToy/res

cd ${toyHome}

rm -r -f ./bin
mvn install -DskipTests

#Basically we should reconfigure the project

find ./bin -type f -name 'currentView' -delete

echo "Starting server"
echo ${1} | sudo -S chmod 777 ./bin/run_single.sh

ret=`${toyHome}/bin/run_single.sh ${2} ${3} "src/main/resources"` &

