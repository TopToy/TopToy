#!/usr/bin/env bash

source $PWD/utils/aws_utils.sh
source $PWD/utils/config_utils.sh
source $PWD/utils/utils.sh
source $PWD/definitions.sh

outFile=~/toy/out/net.csv
test_network_performace() {
    start_aws_instances
    prepareTest
#    for s in "${servers[@]}"; do
#        install_servers ${s}
#    done

    for s in "${servers[@]}"; do
        start_server ${s}
    done

    servers_num=$((${#servers[@]} - 1))
    for i in `seq 0 ${servers_num}`; do
        for j in `seq $((${i} + 1)) ${servers_num}`; do
            local public_ip=`echo "${servers[${j}]}" | sed 's/'${user}'\@//g'`
            echo "testing latency ${servers[${i}]} over ${public_ip}"
            test_lat ${servers[${i}]} ${public_ip} ${i}
        done
    done

    for ((i=${servers_num}; i>=0; i--)); do
        for ((j=${i} - 1; j>=0; j--)); do
            local public_ip=`echo "${servers[${j}]}" | sed 's/'${user}'\@//g'`
            echo "testing bandwidth ${servers[${i}]} over ${public_ip}"
            test_band ${servers[${i}]} ${public_ip} ${i}
        done
    done

    collect_res
    stop_aws_instances

}



install_servers() {
    echo "install ${1}"
    ssh ${1} "sudo apt-get install -y iperf3"
    ssh ${1} "sudo apt-get install -y qperf"
    echo "------------------done install ${1} ---------------------------------"
}

start_server() {
    echo "starts ipref3 on ${1}"
    ssh ${1} "iperf3 -s -f M" &
    echo "starts qperf on ${1}"
    ssh ${1} "qperf" &
}

test_band() {
    mkdir -p ~/toy/out/${3}/band/
    ssh ${1} "iperf3 -c ${2} -f M" > ~/toy/out/${3}/band/${2}.csv
}

test_lat() {
    mkdir -p ~/toy/out/${3}/lat/
    ssh ${1} "qperf -vvs ${2} tcp_lat" > ~/toy/out/${3}/lat/${2}.csv
}

collect_res() {
    servers_num=$((${#servers[@]} - 1))
    for i in `seq 0 ${servers_num}`; do
        if [ -d ~/toy/out/${i}/band ]; then
            files=`ls ~/toy/out/${i}/band`
            files_num=$((${#files[@]} - 1))
            for j in `seq 0 ${files_num}`; do
                f=${files[${j}]}
                tail -4 ~/toy/out/${i}/band/${f} | head -1 >  ~/toy/out/${i}/band/${f}.tmp
                readarray -t dataArr < ~/toy/out/${i}/band/${f}.tmp
                local data2Write=`echo -n ${dataArr[0]} | cut -d " " -f7`
                echo -n "${data2Write}," >> ${outFile}
                rm ~/toy/out/${i}/band/${f}.tmp
                rm ~/toy/out/${i}/band/${f}
            done

        fi
        echo -n "0">> ${outFile}

         if [ -d ~/toy/out/${i}/lat ]; then
            files=`ls ~/toy/out/${i}/lat`
            files_num=$((${#files[@]} - 1))
            for j in `seq 0 ${files_num}`; do
                f=${files[${j}]}
                cat ~/toy/out/${i}/lat/${f} | head -2 | tail -1 | cut -d '=' -f2 >  ~/toy/out/${i}/lat/${f}.tmp
                readarray -t dataArr < ~/toy/out/${i}/lat/${f}.tmp
                local data2Write=`echo -n ${dataArr[0]}` # | cut -d " " -f1`
                echo -n ",${data2Write}" >> ${outFile}
#                rm ~/toy/out/${i}/lat/${f}.tmp
#                rm ~/toy/out/${i}/lat/${f}
            done
        fi
        echo >> ${outFile}
    done





}
prepareTest() {
    mkdir -p ~/toy/out/
    touch ${outFile}
#    servers_num=$((${#servers[@]} - 1))
#    for i in `seq 0 ${servers_num}`; do
#        if [ ${i} -eq 0 ]; then
#            echo -n ${servers[${i}]} >> ${outFile}
#        else
#            echo -n ",${servers[${i}]}" >> ${outFile}
#        fi
#    done
#    echo >> ${outFile}
}

test_network_performace