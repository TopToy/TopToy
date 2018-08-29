#!/bin/bash
cd $(dirname "$0")
rm -r -f "./target"
mvn install -DskipTests