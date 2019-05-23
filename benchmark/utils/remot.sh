#!/usr/bin/env bash

. utils/defs.sh

# ${1} - server user@ip
load_bin_to_server() {
    echo "copy bin to ${1}..."
    ssh ${1} 'rm -r -f ~/JToy'
    ssh ${1} 'mkdir ~/JToy'
    scp -r ${bin_dir} ${1}:~/JToy  > /dev/null
}

load_bin_to_servers() {
    local conn=$(get_conn_array)
    for c in ${conn[*]}; do
        echo ${c}
#        load_bin_to_server ${c}
    done
}
# ${1} - configuration folder
update_bin_resources() {
#    rm -r -f ${bin_dir}/src/main/resources/*
#    cp -r ${1}/* ${bin_dir}/src/main/resources/
    ls ${bin_dir}/src/main/resources/*
    ls ${1}/* ${bin_dir}/src/main/resources/
}

# ${1} - user@ip
# ${2} - configuration
load_server_conf() {
    echo "Updating configuration of ${1}..."
    ssh ${1} 'rm -r -f ~/JToy/bin/src/main/resources/config.toml'
    scp ${2} ${1}:~/JToy/bin/src/main/resources/  > /dev/null
}

# ${1} conf file
load_servers_conf() {
    readarray -t conn < `get_conn_array`
    for c in ${conn[*]}; do
        load_server_conf ${c} ${1}
    done
}
# ${1} - user@ip
# ${2} - id
# ${3} - type
run_server() {
    cat run_remote.sh | sed 's/${2}/'${2}'/g' | sed 's/${3}/'${3}'/g' | ssh ${1} bash &
    echo $!
}

# ${1} - list of user@ip
# ${2} - list of types
run_cluster() {
    declare -a servers=("${!1}")
    declare -a types=("${!2}")
    ./watchdog.sh &
    local pwatch=$!
    local pids=[]
    local id=0
    for s in "${servers[@]}"; do
        echo "running server ${s} [${id}] ${types[${id}]}"
        pids[${id}]=$(run_server ${s} ${id} ${types[${id}]})
        id=$((${id} + 1))
    done
    for pid in ${pids[*]}; do
        wait $pid
    done
    kill -9 $pwatch
}

# ${1} - list of user@ip
collect_res_from_servers() {
    local dt=$(date '+%F-%H:%M:%S')
    local logs=${tDir}/out/logs/$dt
    local tmp=$tDir/tmp
    local sum=${tDir}/out/summeries/$dt.csv
    mkdir -p ${tDir}/out/summeries
    mkdir -p $logs
    mkdir -p $tmp
    declare -a servers=("${!1}")
    for s in "${servers[@]}"; do
        echo "getting files from server ${s}"
        scp -o ConnectTimeout=30 -r ${s}:/tmp/JToy/logs/* $logs  > /dev/null
        scp -o ConnectTimeout=30 -r ${s}:/tmp/JToy/res/* $tmp  > /dev/null
    done
    echo "id,type,workers,tmo,actTmo,maxTmo,txSize,txInBlock,txTotal,duration,tps,nob,bps,avgTxInBlock,opt,opRate,pos,posRate,neg,negRate,avgNegTime,ATDT,APDT,T,P,syncEvents,suspected,tm,fm" >> $sum

    for i in `seq 0 $((${#servers[@]} - 1))`; do
        echo "collecting results summery from server ${servers[$i]}"
        cat $tmp/${i}/summery.csv >> $sum
    done
        rm -r -f $tmp
}

# ${1} - conn array
get_conn_array() {
    readarray -t ips < ${data_dir}/ips.txt
#    conn=${1}
    for ip in ${ips[*]}; do
        echo "${user}@${ip}"
    done
}