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
# ${6} tmo
# ${7} tx num
test_clients_latency_over_workers() {
    start_aws_instances
    echo "running test_clients_latency_over_workers"

    configure_correct_servers "$@"
    configure_clients "$@"

    for i in `seq ${3} ${5} ${4}`; do
        echo "Testing [w=${i} ; sigma=${1} ; beta=${2}]"
        configure_servers_workers
        configure_clients_workers

        ${utils_dir}/watchdog.sh 900 &
        local wdipd=$!

        run_servers
        sleep 30

        run_clients ${7}
        kill -9 $wdipd


        collect_res_from_clients
        collect_res_from_servers

        kill_servers
    done


    stop_aws_instances
}

configure_correct_servers() {

    for i in `seq 0 $((${#servers[@]} - 1))`; do

        configure_tx_size ${1} ${config_toml}
        configure_max_tx_in_block ${2} ${config_toml}
        configure_tmo ${6} ${config_toml}
        configure_testing "true" ${config_toml}
        configure_inst 1200 ${inst}

        copy_data_to_tmp ${config_rb}
        local public_ip=`echo "${servers[${i}]}" | sed 's/'${user}'\@//g'`
        local private_ip=${pips[${i}]}
        replace_ips ${public_ip} ${private_ip} ${config_rb}

        update_resources_and_load ${servers[${i}]} ${sbin} ${conf}

        restore_data_from_tmp ${config_rb}

    done
}

configure_clients() {

    for i in `seq 0 $((${#clients[@]} - 1))`; do

        configure_tx_size ${1} ${config_toml}
        configure_max_tx_in_block ${2} ${config_toml}
        configure_testing "true" ${config_toml}
        update_resources_and_load ${clients[${i}]} ${cbin} ${conf}

    done
}
run_servers() {

    for i in `seq 0 $((${#servers[@]} - 1))`; do
        run_remote_server ${servers[${i}]} ${i} "r"
    done

}

run_clients() {

    local pids=[]
    for i in `seq 0 $((${#clients[@]} - 1))`; do
        run_remote_client ${clients[${i}]} ${i} ${i} ${1}
        pids[${i}]=$!
    done

    for pid in ${pids[@]}; do
        wait $pid
    done

}

configure_servers_workers() {
    for s in "${servers[@]}"; do
        configure_workers ${i} ${config_toml}
        update_config_toml ${s} ${sbin} ${config_toml}
    done

}

configure_clients_workers() {
    for c in "${clients[@]}"; do
        configure_workers ${i} ${config_toml}
        update_config_toml ${c} ${cbin} ${config_toml}
    done

}
# ${1} tx size
# ${2} max tx/block
# #{3} start worker
# ${4} end worker
# ${5} interval
# ${6} tmo
# ${7} tx num

########################### 10 tx/block ###########################
#test_clients_latency_over_workers 512 10 1 1 1 100 1000
#test_clients_latency_over_workers 512 10 5 5 1 100 1000
#test_clients_latency_over_workers 512 10 10 10 1 100 1000

########################### 100 tx/block ###########################
#test_clients_latency_over_workers 512 100 1 1 1 100 1000
#test_clients_latency_over_workers 512 100 5 5 1 100 1000
#test_clients_latency_over_workers 512 100 10 10 1 100 1000

########################### 1000 tx/block ###########################
#test_clients_latency_over_workers 512 1000 1 1 1 100 1000
#test_clients_latency_over_workers 512 1000 5 5 1 100 1000
test_clients_latency_over_workers 512 1000 10 10 1 100 1000



