#!/usr/bin/env bash

BASE_DIR=.
BIN_DIR=${BASE_DIR}/bin
RESOURCES_DIR=${BIN_DIR}/src/main/resources
C=4
F=1
make_script_dir=${BASE_DIR}/make_scripts
docker_image=toy:0.1
cdest=/home/yoni/Desktop/dtoy/configurations/correct
fbdest=/home/yoni/Desktop/dtoy/configurations/fbenign
compose_file_correct=${BASE_DIR}/composed/docker-compose-correct.yml
compose_file_benign_failures=${BASE_DIR}/composed/docker-compose-benign-failures.yml
docker_out=/home/yoni/Desktop/dtoy/curr/
output=/home/yoni/Desktop/dtoy/out
test_dir=${make_script_dir}/tests