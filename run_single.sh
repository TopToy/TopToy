#!/usr/bin/env bash

cd $(dirname "$0")
find . -type f -name 'currentView' -delete
if [ "$#" -eq 2 ] ; then
    java -Xmx12g -jar JToy-1.0-jar-with-dependencies.jar ${1} ${2}
else
    for filename in ${3}/inst/*.inst; do
        java -Xmx12g -jar JToy-1.0-jar-with-dependencies.jar ${1} ${2} < ${filename}
    done
fi