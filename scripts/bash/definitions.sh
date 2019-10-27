#!/usr/bin/env bash

home=~/toy
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
aws_regions_list=${data_dir}/aws_regions.txt

readarray -t servers < ${servers_ips}
readarray -t clients < ${clients_ips}
readarray -t pips < ${servers_pip}
readarray -t servers_aids < ${servers_aws_ids}
readarray -t aws_regions < ${aws_regions_list}

config_rb=${conf}/ABConfig/hosts.config
config_toml=${conf}/config.toml
inst=${conf}/inst/input.inst

n=1
f=0
