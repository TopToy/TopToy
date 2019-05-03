#!/usr/bin/env bash

source $PWD/utils/*.sh
source $PWD/definitions.sh

# ${1} tx size
# ${2} max tx/block
# #{3} start worker
# ${4} end worker
# ${5} interval
# ${6} tmo
# ${7} test time
test_correct_tps_servers_over_workers() {
    start_aws_instances
    echo "running test_correct_tps_servers_over_workers"

    configure_correct_servers "$@"

    for i in `seq ${3} ${5} ${4}`; do

        configure_servers_workers

        ./${utils_dir}/watchdog.sh 420
        local wdipd=$!

        run_servers

        kill -9 $wdipd

        collect_res_from_servers

    done
    stop_aws_instances
}

configure_correct_servers() {
    local servers
    readarray -t servers < ${servers_ips}

    local pips
    readarray -t pips < ${servers_pip}

    local config_rb=${conf}/ABConfig/hosts.config
    local config_toml=${conf}/config.toml
    local inst=${conf}/inst/input.inst

    mkdir -p
    for i in `seq 0 $((${servers[@]} - 1))`; do

        configure_tx_size ${1} ${config_toml}
        configure_max_tx_in_block ${2} ${config_toml}
        configure_tmo ${6} ${config_toml}
        configure_inst_with_statistics 30 ${7} 30 ${inst}

        copy_data_to_tmp ${config_rb}
        local public_ip=`echo "${servers[${i}]}" | sed 's/'${user}'\@//g'`
        local private_ip=${pips[${i}]}
        replace_ips ${public_ip} ${private_ip} ${config_rb}

        update_resources_and_load ${servers[${i}]} ${sbin} ${conf}

        restore_data_from_tmp ${config_rb}

    done
}

run_servers() {

    local servers
    readarray -t servers < ${servers_ips}
    local pids=[]
    for i in `seq 0 $((${servers[@]} - 1))`; do
        local pid=$((run_remote_server ${servers[${i}]} ${i} "r"))
        pids[${i}]=${pid}
    done

    sleep 30
    t=$((${7} - 30))
    echo "waits for more ${t} s"
    progress-bar ${t}

    for pid in ${pids[*]}; do
        wait $pid
    done

}

configure_servers_workers() {
    local servers
    readarray -t servers < ${servers_ips}
    local config_toml=${conf}/config.toml
    for s in "${servers[*]}"; do
        configure_workers ${i} ${config_toml}
        update_config_toml ${s} ${sbin} ${config_toml}
    done

}

##########################0#############################
test_correct_tps_servers_over_workers 0 0 1 5 2 100 300
test_correct_tps_servers_over_workers 0 0 7 9 2 100 300
test_correct_tps_servers_over_workers 0 0 10 10 1 100 300

#########################512#############################
########################512x10###########################
test_correct_tps_servers_over_workers 512 10 1 5 2 100 300
test_correct_tps_servers_over_workers 512 10 7 9 2 100 300
test_correct_tps_servers_over_workers 512 10 10 10 1 100 300

#######################512x100###########################
test_correct_tps_servers_over_workers 512 100 1 5 2 100 300
test_correct_tps_servers_over_workers 512 100 7 9 2 100 300
test_correct_tps_servers_over_workers 512 100 10 10 1 100 300

#######################512x1000###########################
test_correct_tps_servers_over_workers 512 1000 1 5 2 100 300
test_correct_tps_servers_over_workers 512 1000 7 9 2 100 300
test_correct_tps_servers_over_workers 512 1000 10 10 1 100 300

#########################1024#############################
########################1024x10###########################
test_correct_tps_servers_over_workers 512 10 1 5 2 100 300
test_correct_tps_servers_over_workers 512 10 7 9 2 100 300
test_correct_tps_servers_over_workers 512 10 10 10 1 100 300

#######################1024x100###########################
test_correct_tps_servers_over_workers 512 100 1 5 2 100 300
test_correct_tps_servers_over_workers 512 100 7 9 2 100 300
test_correct_tps_servers_over_workers 512 100 10 10 1 100 300

#######################1024x1000###########################
test_correct_tps_servers_over_workers 512 1000 1 5 2 100 300
test_correct_tps_servers_over_workers 512 1000 7 9 2 100 300
test_correct_tps_servers_over_workers 512 1000 10 10 1 100 300

#########################4096#############################
########################4096x10###########################
test_correct_tps_servers_over_workers 512 10 1 5 2 100 300
test_correct_tps_servers_over_workers 512 10 7 9 2 100 300
test_correct_tps_servers_over_workers 512 10 10 10 1 100 300

#######################4096x100###########################
test_correct_tps_servers_over_workers 512 100 1 5 2 100 300
test_correct_tps_servers_over_workers 512 100 7 9 2 100 300
test_correct_tps_servers_over_workers 512 100 10 10 1 100 300

#######################4096x1000###########################
test_correct_tps_servers_over_workers 512 1000 1 5 2 100 300
test_correct_tps_servers_over_workers 512 1000 7 9 2 100 300
test_correct_tps_servers_over_workers 512 1000 10 10 1 100 300


