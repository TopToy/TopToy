#!/usr/bin/env bash

toyHome=JToy
rm -r -f /tmp/JToy/logs
rm -r -f /tmp/JToy/res
mkdir -p /tmp/JToy/logs
mkdir -p /tmp/JToy/res

cd ${toyHome}

#mvn install -DskipTests

find ./bin -type f -name 'currentView' -delete
echo ${1} | sudo -S chmod 777 ./bin/run_single.sh

echo "Starting server"
ret=`./bin/run_single.sh ${2} ${3} "src/main/resources"`
rm -r -f ./bin

