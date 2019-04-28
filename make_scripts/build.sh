#!/usr/bin/env bash


source /home/yoni/github.com/JToy/definitions.sh
main() {
    mvn -f ${BASE_DIR} install
}

main