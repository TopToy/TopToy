#!/usr/bin/env bash

cd $(dirname "$0")
find . -type f -name 'currentView' -delete
#if [ "$#" -eq 2 ] ; then
#    java --Xmx16384m -jar -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap JToy-1.0-jar-with-dependencies.jar ${1} ${2}
#else
#    for filename in ./src/main/resources/inst/*.inst; do
#        java -Xmx16384m -jar -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap JToy-1.0-jar-with-dependencies.jar ${1} ${2} < ${filename}
#    done
#fi
for filename in ./src/main/resources/inst/*.inst; do
        java -Xmx16384m -jar -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap JToy-1.0-jar-with-dependencies.jar ${1} ${2} < ${filename}
done