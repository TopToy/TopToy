#!/usr/bin/env bash

cd $(dirname "$0")
if [ "$#" -eq 0 ] ; then
    java -jar JToy-1.0-jar-with-dependencies.jar
else
    for filename in ${1}/inst/*.inst; do
        java -jar JToy-1.0-jar-with-dependencies.jar < ${filename}
    done
fi