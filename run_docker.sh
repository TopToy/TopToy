#!/usr/bin/env bash

cd $(dirname "$0")
find . -type f -name 'currentView' -delete
java -Xmx2048m -jar -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions JToy-1.0-jar-with-dependencies.jar ${1} ${2} < ./src/main/resources/inst/input.inst