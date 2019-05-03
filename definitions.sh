#!/usr/bin/env bash

BASE_DIR=.
BIN_DIR=${BASE_DIR}/bin
CBIN_DIR=${BASE_DIR}/cbin
RESOURCES_DIR=${BIN_DIR}/src/main/resources
CRESOURCES_DIR=${CBIN_DIR}/src/main/resources
C=4
F=1
make_script_dir=${BASE_DIR}/make_scripts
docker_image=toy:0.1
cdocker_image=ctoy:0.1

cdest=/home/yoni/Desktop/dtoy/configurations/correct
fbdest=/home/yoni/Desktop/dtoy/configurations/fbenign
asdest=/home/yoni/Desktop/dtoy/configurations/async
byzdest=/home/yoni/Desktop/dtoy/configurations/byz
cldest=/home/yoni/Desktop/dtoy/configurations/client

compose_file_correct=${BASE_DIR}/composed/docker-compose-correct.yml
compose_file_benign_failures=${BASE_DIR}/composed/docker-compose-benign-failures.yml
compose_file_async=${BASE_DIR}/composed/docker-compose-async.yml
compose_file_byz=${BASE_DIR}/composed/docker-compose-byz.yml
compose_file_correct_with_clients=${BASE_DIR}/composed/docker-compose-correct-with-clients.yml

docker_out=/home/yoni/Desktop/dtoy/curr/
cdocker_out=/home/yoni/Desktop/dtoy/ccurr/

output=/home/yoni/Desktop/dtoy/out
test_dir=${make_script_dir}/tests
tests_conf=${test_dir}/tests_conf.txt