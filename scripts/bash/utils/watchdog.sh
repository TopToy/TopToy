#!/usr/bin/env bash
source $PWD/definitions.sh

sleep ${1}
echo "watchdog activated"
${utils_dir}/clean_machines.sh