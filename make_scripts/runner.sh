#!/usr/bin/env bash

source /home/yoni/github.com/JToy/definitions.sh

run() {
    for t in ${test_dir}/active/*.sh; do
        echo "running ${t}..."
        sudo chmod 777 $t
        local outputDir=${output}/$(date '+%F-%H:%M:%S')
        mkdir -p ${outputDir}/servers
        print_headers ${outputDir}
        ${t}
        collect_res_from_servers ${outputDir}
        sudo rm -r -f ${docker_out}/*
        echo "done..."
    done
}

run2() {
    while read -r line; do
        rm -r ${docker_out}
        mkdir -p ${docker_out}
        local name=`echo $line | cut -f 1 -d " "`
        local t=`echo $line | cut -f 2 -d " "`

        if [[ $line == '#'* ]] ; then
            echo $"skipping ${name}..."
        else
            echo "running ${name}..."
            sudo chmod 777 ${test_dir}/$t.sh
            local outputDir=${output}/$(date '+%F-%H:%M:%S').${name}
            mkdir -p ${outputDir}/servers
            print_headers ${outputDir}
            ${test_dir}/${t}.sh
            collect_res_from_servers ${outputDir}
            sudo rm -r -f ${docker_out}/*
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

#    for sigsummery in ${docker_out}/**/sig_summery.csv ; do
#        cat ${sigsummery} >> ${currOut}/servers/sig_summery.csv
#    done
    shopt -u globstar
}

print_headers() {
    local currOut=${1}
    echo "id,type,workers,tmo,actTmo,maxTmo,txSize,txInBlock,txTotal,duration,tps,nob,bps,avgTxInBlock,opt,opRate,pos,posRate,neg,negRate,avgNegTime,ATDT,APDT,T,P,syncEvents" >> $currOut/servers/summery.csv
#    echo "id,type,channels,txSize,maxTxInBlock,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" >> $currOut/servers/blocksStatSummery.csv
#    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" >> ${currOut}/clients/summery.csv
}

run2