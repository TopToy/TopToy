#!/usr/bin/env bash
#
toyHome=~/JToy
rm -r -f /tmp/JToy/logs
rm -r -f /tmp/JToy/res
mkdir -p /tmp/JToy/logs
mkdir -p /tmp/JToy/res

cd ${toyHome}

#mvn install -DskipTests
#
#find ./bin -type f -name 'currentView' -delete

sudo -S chmod 777 ./bin_client/run_client.sh

echo "Starting client"
./bin_client/run_client.sh ${1} ${2} ${3} "/tmp/JToy/res/" ${5}
#rm -r -f ./bin


#toyHome=../../
##rm -r -f /tmp/JToy/logs
##rm -r -f /tmp/JToy/res
##mkdir -p /tmp/JToy/logs
##mkdir -p /tmp/JToy/res
#
#cd ${toyHome}
#
##mvn install -DskipTests
##
##find ./bin -type f -name 'currentView' -delete
#
##sudo -S chmod 777 ./bin_client/run_client.sh
#
#echo "Starting client"
#./bin_client/run_client.sh ${1} ${2} ${3}
##rm -r -f ./bin
