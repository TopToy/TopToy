#!/usr/bin/env bash
curr=/home/yoni/Desktop/dtoy/curr
output=/home/yoni/Desktop/dtoy/out
test_dir=$(dirname "$0")/tests

run(){
    for t in ${test_dir}/active/*.sh; do
        sudo chmod 777 $t
        local outputDir=${output}/$(date '+%F-%H:%M:%S')
        mkdir -p ${outputDir}/servers
        print_headers ${outputDir}
        ${t}
        collect_res_from_servers ${outputDir}
        sudo rm -r -f ${curr}/*
    done
}

collect_res_from_servers() {
    local currOut=${1}
    mkdir -p ${currOut}/servers/logs
    shopt -s globstar

    cp -r ${curr}/logs/* ${currOut}/servers/logs/
    for summery in ${curr}/**/summery.csv ; do
        cat ${summery} >> ${currOut}/servers/summery.csv
    done

    for sigsummery in ${curr}/**/sig_summery.csv ; do
        cat ${sigsummery} >> ${currOut}/servers/sig_summery.csv
    done
    shopt -u globstar
}

print_headers() {
    local currOut=${1}
    echo "ts,id,type,channels,tmo,fm,txSize,txInBlock,txTotal,duration,txPsec,blocksNum,avgTxInBlock,avgDelay,opRate,eRate,dRate" >> $currOut/servers/summery.csv
    echo "id,type,channels,txSize,maxTxInBlock,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" >> $currOut/servers/blocksStatSummery.csv
#    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" >> ${currOut}/clients/summery.csv
}

run