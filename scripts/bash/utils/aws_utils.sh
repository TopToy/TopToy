#!/usr/bin/env bash

source $PWD/utils/config_utils.sh
source $PWD/definitions.sh

start_aws_instances() {
    local servers
    readarray -t servers < ${servers_aws_ids}
    aws ec2 start-instances --instance-ids ${servers[*]}
    sleep 30

}

stop_aws_instances() {
    local servers
    readarray -t servers < ${servers_aws_ids}
    aws ec2 stop-instances --instance-ids ${servers[*]}
    sleep 30
}

# ${1} connection string
# ${2} bin directory
# ${3} configuration directory
update_resources_and_load() {
    update_resources ${2} ${3}
    echo "copy ${2} to ${1}..."
    ssh ${1} 'rm -r -f ~/JToy'
    ssh ${1} 'mkdir ~/JToy'
    scp -r ${2} ${1}:~/JToy  > /dev/null
}

# ${1} connection string
# ${2} bin directory
# ${3} update config.toml
update_config_toml() {
    echo "Updating configuration of ${1}..."
    ssh ${1} 'rm -r -f ~/JToy/${2}/src/main/resources/config.toml'
    scp ${3} ${1}:~/JToy/${2}/src/main/resources/  > /dev/null
}

# ${1} connection string
# ${2} sid
# ${3} type
run_remote_server() {
    cat ${utils_dir}/run_remote.sh | sed 's/${2}/'${2}'/g' | sed 's/${3}/'${3}'/g' | ssh ${1} bash &
    echo $!
}

# ${1} connection string
# ${2} cid
# ${3} sid
# ${4} tx num
run_remote_client() {
    cat ${utils_dir}/run_remote_client.sh | sed 's/${1}/'${2}'/g' | sed 's/${2}/'${3}'/g' | sed 's/${3}/'${4}'/g' | ssh ${1} bash &
    echo $!
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

    echo "getting files from server ${1}"
    scp -o ConnectTimeout=30 -r ${1}:/tmp/JToy/logs/* $logs  > /dev/null
    scp -o ConnectTimeout=30 -r ${1}:/tmp/JToy/res/* $tmp  > /dev/null

    echo "collecting results summery from server ${1}"
    cat $tmp/${2}/summery.csv >> $sum
}

collect_res_from_servers() {
    local dt=$(date '+%F-%H:%M:%S')
    local logs=${home}/out/logs/$dt
    local tmp=${home}/out/tmp
    local sum=${home}/out/summeries/$dt.csv
    mkdir -p ${home}/out/summeries
    mkdir -p $logs
    mkdir -p $tmp

    print_servers_summery_header ${sum}
    local servers
    readarray -t servers < ${servers_ips}

    local id=0
    for s in $"{[servers[*]}"; do
        collect_res_from_server ${s} ${id} ${logs} ${tmp} ${sum}
    id=$((${id} + 1))
    done

    rm -r -f ${tmp}
}