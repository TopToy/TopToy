#!/usr/bin/env bash

toyHome=JToy

mkdir ${toyHome}/logs
mkdir ${toyHome}/res

cd ${toyHome}


mvn install -DskipTests

#Basically we should reconfigure the project

find ./bin -type f -name 'currentView' -delete

echo "Starting server"
echo ${1} | sudo -S chmod 777 ./bin/run_single.sh

ret=`${toyHome}/bin/run_single.sh ${2} ${3} "src/main/resources"` &

