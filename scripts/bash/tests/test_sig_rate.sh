#!/usr/bin/env bash

source $PWD/utils/aws_utils.sh
source $PWD/utils/config_utils.sh
source $PWD/utils/utils.sh
source $PWD/definitions.sh

# ${1} tx size
# ${2} max tx/block
# #{3} start worker
# ${4} end worker
# ${5} interval
# ${6} test time
test_sig_rate_over_workers() {

    echo "running test_sig_rate_over_workers"

    configure_correct_servers "$@"

    for i in `seq ${3} ${5} ${4}`; do
        sleep 10
        echo "run [[txSize:${1}] [txInBlock:${2}] [workers:${i}]]"
        local w=${i}
        configure_servers_workers ${w}

        ${utils_dir}/watchdog.sh 420 &
        local wdipd=$!

        run_servers $6

        kill -9 $wdipd

        collect_res_from_servers

    done

}

configure_correct_servers() {

    configure_tx_size ${1} ${config_toml}
    configure_max_tx_in_block ${2} ${config_toml}
    configure_tmo ${6} ${config_toml}
    configure_inst_for_sig_test ${6} ${inst}

    for i in `seq 0 $((${#servers[@]} - 1))`; do

        copy_data_to_tmp ${config_rb}
        local public_ip=`echo "${servers[${i}]}" | sed 's/'${user}'\@//g'`
        local private_ip=${pips[${i}]}
        replace_ips ${public_ip} ${private_ip} ${config_rb}

        update_resources_and_load ${servers[${i}]} ${sbin} ${conf}

        restore_data_from_tmp ${config_rb}

    done
}

run_servers() {

    local pids=[]
    for i in `seq 0 $((${#servers[@]} - 1))`; do
        local id=${i}
        run_remote_server ${servers[${id}]} ${id} "r"
        pids[${id}]=$!
    done
    sleep 30
    t=$((${1} - 30))
    echo "waits for more ${t} s"
    progress-bar ${t}

    for pid in "${pids[@]}"; do
        wait ${pid}
    done

}

configure_servers_workers() {

    for s in "${servers[@]}"; do
        configure_workers ${1} ${config_toml}
        sr=${s}
        update_config_toml ${sr} ${sbin} ${config_toml}
    done

}
start_aws_instances

# ${1} tx size
# ${2} max tx/block
# #{3} start worker
# ${4} end worker
# ${5} interval
# ${6} test time
test_sig_rate_over_workers 0 0 1 9 2 60
test_sig_rate_over_workers 0 0 10 10 2 60
#################beta=10##############################
test_sig_rate_over_workers 10 512 1 9 2 60
test_sig_rate_over_workers 10 512 10 10 2 60

test_sig_rate_over_workers 10 1024 1 9 2 60
test_sig_rate_over_workers 10 1024 10 10 2 60

test_sig_rate_over_workers 10 4096 1 9 2 60
test_sig_rate_over_workers 10 4096 10 10 2 60

#################beta=100##############################
test_sig_rate_over_workers 100 512 1 9 2 60
test_sig_rate_over_workers 100 512 10 10 2 60

test_sig_rate_over_workers 100 1024 1 9 2 60
test_sig_rate_over_workers 100 1024 10 10 2 60

test_sig_rate_over_workers 100 4096 1 9 2 60
test_sig_rate_over_workers 100 4096 10 10 2 60


#################beta=1000##############################
test_sig_rate_over_workers 1000 512 1 9 2 60
test_sig_rate_over_workers 1000 512 10 10 2 60

test_sig_rate_over_workers 1000 1024 1 9 2 60
test_sig_rate_over_workers 1000 1024 10 10 2 60

test_sig_rate_over_workers 1000 4096 1 9 2 60
test_sig_rate_over_workers 1000 4096 10 10 2 60




stop_aws_instances

