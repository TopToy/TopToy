#!/usr/bin/env bash

source $PWD/definitions.sh

run2() {
    while read -r line; do
        sudo rm -r -f ${docker_out}
        sudo rm -r -f ${cdocker_out}
        mkdir -p ${docker_out}
        mkdir -p ${cdocker_out}
        local name=`echo $line | cut -f 1 -d " "`
        local t=`echo $line | cut -f 2 -d " "`

        if [[ $line == '#'* ]] ; then
            echo $"skipping ${name}..."
        else
            echo "running ${name}..."
            sudo chmod 777 ${test_dir}/$t.sh
            local outputDir=${output}/$(date '+%F-%H:%M:%S').${name}
            mkdir -p ${outputDir}/servers
            mkdir -p ${outputDir}/clients
            print_headers ${outputDir}
            ${test_dir}/${t}.sh
            collect_res_from_servers ${outputDir}
            collect_res_from_clients ${outputDir}
            sudo rm -r -f ${docker_out}/*
            sudo rm -r -f ${cdocker_out}/*
            echo "done..."
        fi
    done < "${tests_conf}"

}

collect_res_from_servers() {
    local currOut=${1}
    mkdir -p ${currOut}/servers/logs
    shopt -s globstar

    cp -r ${docker_out}/logs/* ${currOut}/servers/logs/
    for summery in ${docker_out}/**/summery.csv ; do
        cat ${summery} >> ${currOut}/servers/summery.csv
    done

    shopt -u globstar
}

collect_res_from_clients() {
    local currOut=${1}
    mkdir -p ${currOut}/clients/logs
    shopt -s globstar

    cp -r ${cdocker_out}/logs/* ${currOut}/clients/logs/
    for csummery in ${cdocker_out}/**/csummery.csv ; do
        cat ${csummery} >> ${currOut}/clients/summery.csv
    done

    for txsummery in ${cdocker_out}/**/ctsummery.csv ; do
        cat ${txsummery} >> ${currOut}/clients/transactions.csv
    done

    shopt -u globstar
}


print_headers() {
    local currOut=${1}
    echo "valid,id,type,workers,tmo,actTmo,maxTmo,txSize,txInBlock,txTotal,duration,tps,nob,noeb,bps,avgTxInBlock,opt,opRate,pos,posRate,neg,negRate,avgNegTime,syncs,BP2T,BP2D,BP2DL,HP2T,HP2D,HP2DL,HT2D,HD2DL,suspected" >> $currOut/servers/summery.csv

    echo "w,beta,cid,txSize,duration,txNum,avgLatency,maxLatency" > $currOut/clients/summery.csv

    echo "n,w,beta,cid,txID,txSize,txLatency"  > $currOut/clients/transactions.csv
}

run2