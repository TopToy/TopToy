#!/bin/bash
cd $(dirname "$0")
rm -r "./target"
mvn compile
mvn install