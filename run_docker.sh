#!/usr/bin/env bash

cd $(dirname "$0")
find . -type f -name 'currentView' -delete
#ls ./src/main/resources/inst
#cat ./src/main/resources/inst/input.inst
#for filename in ./src/main/resources/inst/*.inst; do
#        java -Xmx16384m -jar -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions JToy-1.0-jar-with-dependencies.jar ${1} ${2} < ./src/main/resources/inst/input.inst
#done
java -Xmx4096m -jar -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions JToy-1.0-jar-with-dependencies.jar ${1} ${2} < ./src/main/resources/inst/input.inst