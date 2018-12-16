#!/usr/bin/env bash
out_path=/home/yoni/toy/res_gd_latency_7Servers
latency_path=/home/yoni/toy/gd_latency/7Servers


remove() {
    rm -r -f ${latency_path}
}
copy() {

    mkdir -p ${latency_path}/clients/500.100
    mkdir -p ${latency_path}/clients/500.1000
    mkdir -p ${latency_path}/servers/500.100
    mkdir -p ${latency_path}/servers/500.1000
    cd ${out_path}

    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" > ${latency_path}/clients/500.100/blocksStat_1.csv
    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" > ${latency_path}/clients/500.100/blocksStat_5.csv
    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" > ${latency_path}/clients/500.100/blocksStat_10.csv
    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" > ${latency_path}/clients/500.1000/blocksStat_1.csv
    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" > ${latency_path}/clients/500.1000/blocksStat_5.csv
    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" > ${latency_path}/clients/500.1000/blocksStat_10.csv

    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" > ${latency_path}/clients/500.100/summery.csv
    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" > ${latency_path}/clients/500.1000/summery.csv

    echo "id,type,channels,txSize,maxTxInBlock,actualTxInBlock,height,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" > ${latency_path}/servers/500.100/blocksStat_1.csv
    echo "id,type,channels,txSize,maxTxInBlock,actualTxInBlock,height,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" > ${latency_path}/servers/500.100/blocksStat_5.csv
    echo "id,type,channels,txSize,maxTxInBlock,actualTxInBlock,height,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" > ${latency_path}/servers/500.100/blocksStat_10.csv
    echo "id,type,channels,txSize,maxTxInBlock,actualTxInBlock,height,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" > ${latency_path}/servers/500.1000/blocksStat_1.csv
    echo "id,type,channels,txSize,maxTxInBlock,actualTxInBlock,height,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" > ${latency_path}/servers/500.1000/blocksStat_5.csv
    echo "id,type,channels,txSize,maxTxInBlock,actualTxInBlock,height,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" > ${latency_path}/servers/500.1000/blocksStat_10.csv



    echo "ts,id,type,channels,tmo,fm,txSize,txInBlock,txTotal,duration,txPsec,blocksNum,avgTxInBlock,avgDelay,opRate,eRate,dRate" > ${latency_path}/servers/500.100/summery.csv
    echo "ts,id,type,channels,tmo,fm,txSize,txInBlock,txTotal,duration,txPsec,blocksNum,avgTxInBlock,avgDelay,opRate,eRate,dRate" > ${latency_path}/servers/500.1000/summery.csv

    echo "id,type,channels,txSize,maxTxInBlock,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" > ${latency_path}/servers/500.100/blocksStatSummery.csv
    echo "id,type,channels,txSize,maxTxInBlock,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" > ${latency_path}/servers/500.1000/blocksStatSummery.csv

    # copy client [100, 1]
    files=`find . -type f -wholename "*.100/clients/res/blocksStat_1.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/clients/500.100/blocksStat_1.csv
    done

    # copy client [100, 5]
    files=`find . -type f -wholename "*.100/clients/res/blocksStat_5.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/clients/500.100/blocksStat_5.csv
    done

    # copy client [100, 10]
    files=`find . -type f -wholename "*.100/clients/res/blocksStat_10.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/clients/500.100/blocksStat_10.csv
    done

    # copy client summery [100]
    files=`find . -type f -wholename "*.100/clients/res/summery.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/clients/500.100/summery.csv
    done


    # copy client summery [1000]
    files=`find . -type f -wholename "*.1000/clients/res/summery.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/clients/500.1000/summery.csv
    done


    # copy client [1000 , 1]
    files=`find . -type f -wholename "*.1000/clients/res/blocksStat_1.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/clients/500.1000/blocksStat_1.csv
    done

    # copy client [1000 , 5]
    files=`find . -type f -wholename "*.1000/clients/res/blocksStat_5.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/clients/500.1000/blocksStat_5.csv
    done

    # copy client [1000, 10]
    files=`find . -type f -wholename "*.1000/clients/res/blocksStat_10.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/clients/500.1000/blocksStat_10.csv
    done

    # copy server [100, 1]
    files=`find . -type f -wholename "*.100/servers/res/blocksStat_1.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.100/blocksStat_1.csv
    done

    # copy server [100, 5]
    files=`find . -type f -wholename "*.100/servers/res/blocksStat_5.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.100/blocksStat_5.csv
    done

    # copy server [100, 10]
    files=`find . -type f -wholename "*.100/servers/res/blocksStat_10.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.100/blocksStat_10.csv
    done

    # copy server [1000, 1]
    files=`find . -type f -wholename "*.1000/servers/res/blocksStat_1.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.1000/blocksStat_1.csv
    done

    # copy server [1000, 5]
    files=`find . -type f -wholename "*.1000/servers/res/blocksStat_5.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.1000/blocksStat_5.csv
    done

    # copy server [1000, 10]
    files=`find . -type f -wholename "*.1000/servers/res/blocksStat_10.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.1000/blocksStat_10.csv
    done

    # copy server summery [100]
    files=`find . -type f -wholename "*.100/servers/res/summery.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.100/summery.csv
    done

    # copy server blocksStatSummery [100]
    files=`find . -type f -wholename "*.100/servers/res/blocksStatSummery.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.100/blocksStatSummery.csv
    done


    # copy server summery [1000]
    files=`find . -type f -wholename "*.1000/servers/res/summery.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.1000/summery.csv
    done

    # copy server blocksStatSummery [1000]
    files=`find . -type f -wholename "*.1000/servers/res/blocksStatSummery.csv"`
    for f in ${files[*]}; do
        tail -n +2 ${f} >> ${latency_path}/servers/500.1000/blocksStatSummery.csv
    done
}

files=`find . -type f -wholename "*/summery.csv"`
for f in ${files[*]}; do
    if grep -q "Nan" "$f"; then
      echo "Nan was found!! [${f}]"
      exit
    fi
done

echo "Do you wish to remove existing files?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) remove ; break;;
        No ) exit;;
    esac
done

echo "Do you wish to copy files?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) copy ; break;;
        No ) exit;;
    esac
done