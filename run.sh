#!/bin/bash
cd $(dirname "$0")
if [[ $1 == "-b" ]] || [[ ! -d "./target" ]]; then
    rm -r "./target"
    mvn compile
    mvn install
fi
java -jar -XX:+UseContainerSupport ./target/JToy-1.0-jar-with-dependencies.jar