#!/usr/bin/env bash

cd $(dirname "$0")
java -Xmx4096m -jar client-jar-with-dependencies.jar "$@"