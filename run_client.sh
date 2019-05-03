#!/usr/bin/env bash

cd $(dirname "$0")
java -jar client-jar-with-dependencies.jar "$@"