#!/usr/bin/env bash

BASE_DIR=.
BIN_DIR=${BASE_DIR}/bin
RESOURCES_DIR=${BIN_DIR}/src/main/resources
C=7
F=2
make_script_dir=${BASE_DIR}/make_scripts
docker_image=toy:0.1

cdest=/home/yoni/Desktop/dtoy/configurations/correct
fbdest=/home/yoni/Desktop/dtoy/configurations/fbenign
asdest=/home/yoni/Desktop/dtoy/configurations/async
byzdest=/home/yoni/Desktop/dtoy/configurations/byz

compose_file_correct=${BASE_DIR}/composed/docker-compose-correct.yml
compose_file_benign_failures=${BASE_DIR}/composed/docker-compose-benign-failures.yml
compose_file_async=${BASE_DIR}/composed/docker-compose-async.yml
compose_file_byz=${BASE_DIR}/composed/docker-compose-byz.yml

docker_out=/home/yoni/Desktop/dtoy/curr/
output=/home/yoni/Desktop/dtoy/out
test_dir=${make_script_dir}/tests
tests_conf=${test_dir}/tests_conf.txt