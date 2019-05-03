#!/usr/bin/env bash

toyHome=~/JToy
rm -r -f /tmp/JToy/logs
rm -r -f /tmp/JToy/res
mkdir -p /tmp/JToy/logs
mkdir -p /tmp/JToy/res

cd ${toyHome}

find ./bin -type f -name 'currentView' -delete
sudo -S chmod 777 ./bin/run_client.sh
rm -r -f ./bin/blocks
echo "Starting client"
ret=`./bin/run_client.sh ${1} ${2} ${3}`

