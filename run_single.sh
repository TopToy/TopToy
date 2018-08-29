#!/usr/bin/env bash

cd $(dirname "$0")
if [ "$#" -eq 2 ] ; then
    java -jar JToy-1.0-jar-with-dependencies.jar ${1} ${2}
else
    for filename in ${3}/inst/*.inst; do
        java -jar JToy-1.0-jar-with-dependencies.jar ${1} ${2} < ${filename}
    done
fi