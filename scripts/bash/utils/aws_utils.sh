#!/usr/bin/env bash

source $PWD/utils/config_utils.sh
source $PWD/definitions.sh

start_aws_instances() {
    aws ec2 start-instances --instance-ids ${servers_aids[@]}
#    aws ec2 wait instance-running --instance-ids ${servers_aids[@]}
    sleep 30

}

start_aws_instances_with_regions() {
    for i in `seq 0 $((${#servers_aids[@]} - 1))`; do
        region=${aws_regions[${i}]}
        sid=${servers_aids[${i}]}
        aws ec2 start-instances --region ${region} --instance-ids ${sid}
     done
#    aws ec2 wait instance-running --instance-ids ${servers_aids[@]}
    sleep 30

}

stop_aws_instances_with_regions() {
    for i in `seq 0 $((${#servers_aids[@]} - 1))`; do
        region=${aws_regions[${i}]}
        sid=${servers_aids[${i}]}
        aws ec2 stop-instances --region ${region} --instance-ids ${sid}
     done
#    aws ec2 wait instance-running --instance-ids ${servers_aids[@]}
    sleep 30

}

stop_aws_instances() {

    aws ec2 stop-instances --instance-ids ${servers_aids[@]}
#    aws ec2 wait instance-stopped --instance-ids ${servers_aids[@]}
    sleep 30
}

# ${1} connection string
# ${2} bin directory
# ${3} configuration directory
update_resources_and_load() {
    update_resources ${2} ${3}
    echo "copy $(basename ${2}) to ${1}..."
    ssh ${1} 'rm -r -f ~/JToy'
    ssh ${1} 'mkdir ~/JToy'
    scp -r ${2} ${1}:~/JToy  > /dev/null
}

# ${1} connection string
# ${2} bin directory
# ${3} updated config.toml
update_config_toml() {
    echo "Updating configuration of ${1}..."
    ssh ${1} "rm -r -f ~/JToy/$(basename ${2})/src/main/resources/config.toml"
    scp ${3} ${1}:~/JToy/$(basename ${2})/src/main/resources/  > /dev/null
}

# ${1} connection string
# ${2} sid
# ${3} type
run_remote_server() {
    cat ${utils_dir}/run_remote.sh | sed 's/${2}/'${2}'/g' | sed 's/${3}/'${3}'/g' | ssh ${1} bash &
}

# ${1} connection string
# ${2} cid
# ${3} sid
# ${4} tx num
run_remote_client() {
    cat ${utils_dir}/run_remote_client.sh | sed 's/${1}/'${2}'/g' | sed 's/${2}/'${3}'/g' | sed 's/${3}/'${4}'/g' | ssh ${1} bash &
}

# ${1} connection string
# ${2} sid
# ${3} logs dir
# ${4} tmp dir
# ${5} servers summery file
collect_res_from_server() {
    local logs=${3}
    local tmp=${4}
    local sum=${5}
    local blocks=${6}
    local sigs=${7}

    echo "getting files from server ${1} [tmp is ${tmp}]"
    scp -o ConnectTimeout=30 -r ${1}:/tmp/JToy/logs/* $logs  > /dev/null
    scp -o ConnectTimeout=30 -r ${1}:/tmp/JToy/res/* $tmp  > /dev/null

    echo "collecting results summery from server ${1} [tmp is ${tmp}]"
    cat $tmp/${2}/summary.csv >> $sum
    cat $tmp/${2}/bsummary.csv >> $blocks
    cat $tmp/${2}/sig_summery.csv >> $sigs
}

collect_res_from_servers() {
    local dt=$(date '+%F-%H:%M:%S')
    local logs=${home}/out/logs/$dt
    local tmp=${home}/out/tmp
    local sum=${home}/out/summeries/$dt.csv
    local blocks=${home}/out/summeries/blocks/$dt.csv
    local sigs=${home}/out/summeries/sigs/$dt.csv
    mkdir -p ${home}/out/summeries/blocks
    mkdir -p ${home}/out/summeries/sigs
    mkdir -p $logs
    mkdir -p $tmp
    print_servers_summery_header ${sum} ${blocks}
    print_sig_test_headers $sigs

    local id=0
    for s in "${servers[@]}"; do
        collect_res_from_server ${s} ${id} ${logs} ${tmp} ${sum} ${blocks} ${sigs}
    id=$((${id} + 1))
    done

    rm -r -f ${tmp}
}

collect_res_from_client() {
    local logs=${3}
    local tmp=${4}
    local csum=${5}
    local txsum=${6}

    echo "getting files from client ${1}"
    scp -o ConnectTimeout=30 -r ${1}:/tmp/JToy/logs/* $logs  > /dev/null
    scp -o ConnectTimeout=30 -r ${1}:/tmp/JToy/res/* $tmp  > /dev/null

    echo "collecting results from client ${1}"
    cat $tmp/${2}/csummery.csv >> $csum
    cat $tmp/${2}/ctsummery.csv >> $txsum

}

collect_res_from_clients() {
    local dt=$(date '+%F-%H:%M:%S')
    local logs=${home}/out/clogs/$dt
    local tmp=${home}/out/ctmp
    local csum=${home}/out/csummeries/$dt.csv
    local txsum=${home}/out/csummeries/transactions/$dt.csv
    mkdir -p ${home}/out/csummeries/transactions
    mkdir -p $logs
    mkdir -p $tmp

    print_clients_summery_header ${csum} ${txsum}

    local id=0
    for c in "${clients[@]}"; do
        collect_res_from_client ${c} ${id} ${logs} ${tmp} ${csum} ${txsum}
    id=$((${id} + 1))
    done

    rm -r -f ${tmp}
}

kill_clients() {
    for c in "${clients[@]}"; do
        echo "kill ${c}..."
        ssh ${c} "pkill -u ${user}"
    done

}

kill_servers() {
    for s in "${servers[@]}"; do
        echo "kill ${s}..."
        ssh ${s} "pkill -u ${user}"
    done

}