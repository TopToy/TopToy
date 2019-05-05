#!/usr/bin/env bash

toyHome=~/JToy
rm -r -f /tmp/JToy/logs
rm -r -f /tmp/JToy/res
mkdir -p /tmp/JToy/logs
mkdir -p /tmp/JToy/res

cd ${toyHome}

sudo -S chmod 777 ./cbin/run_client.sh

echo "Starting client"
ret=`./cbin/run_client.sh ${1} ${2} ${3}`

