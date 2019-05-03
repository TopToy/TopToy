#!/usr/bin/env bash

home=/home/yoni/github.com/JToy
user=toy
conf=${home}/configurations
sbin=${home}/bin
cbin=${home}/cbin
scripts_dir=${home}/scripts/bash
data_dir=${scripts_dir}/data
utils_dir=${scripts_dir}/utils
tests_dir=${scripts_dir}/tests
tests_conf=${scripts_dir}/tests_conf.txt

servers_ips=${data_dir}/servers.txt
clients_ips=${data_dir}/clients.txt
servers_pip=${data_dir}/servers_privates.txt
servers_aws_ids=${data_dir}/awsInstanceIds.txt

n=4
f=1

. ${utils_dir}/*.sh
