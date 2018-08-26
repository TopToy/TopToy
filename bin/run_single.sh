#!/usr/bin/env bash

cd $(dirname "$0")
if [ "$#" -eq 1 ] ; then
    java -jar JToy-1.0-jar-with-dependencies.jar ${1}
else
    for filename in ${2}/inst/*.inst; do
        java -jar JToy-1.0-jar-with-dependencies.jar ${1} < ${filename}
    done
fi