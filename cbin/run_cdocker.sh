#!/usr/bin/env bash

cd $(dirname "$0")
java -Xmx2048m -jar client-jar-with-dependencies.jar "$@"