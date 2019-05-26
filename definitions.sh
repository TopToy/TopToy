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

curr_out=/tmp/dtoy
configurations=${curr_out}/configurations

output=${curr_out}/out
composed=${curr_out}/composed


cdest=${configurations}/correct
fbdest=${configurations}/fbenign
asdest=${configurations}/async
byzdest=${configurations}/byz
cldest=${configurations}/client

compose_file_correct=${composed}/docker-compose-correct.yml
compose_file_benign_failures=${composed}/docker-compose-benign-failures.yml
compose_file_async=${composed}/docker-compose-async.yml
compose_file_byz=${composed}/docker-compose-byz.yml
compose_file_correct_with_clients=${composed}/docker-compose-correct-with-clients.yml

docker_out=${curr_out}/curr
cdocker_out=${curr_out}/ccurr

test_dir=${make_script_dir}/tests
tests_conf=${test_dir}/tests_conf.txt