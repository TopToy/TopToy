#!/usr/bin/env bash
cd $(dirname "$0")
config=${1}/config.toml #./../../Configurations/7Servers/remote/config.toml
outputDir=./../../out/$(date '+%F-%H:%M:%S')
txInBlock=(100 500 1000)
txSize=${2}
install_toy.sh ${1}
for j in "${txInBlock[@]}"; do
    currOut=${outputDir}.${j}
    mkdir -p ${currOut}
    mkdir -p $currOut/res
    sed -i 's/maxTransactionInBlock = .*/maxTransactionInBlock = '${j}'/g' ${config}
    echo "ts,id,type,channels,fm,txSize,txInBlock,txTotal,duration,txPsec,latency,rounds,opRounds,opRate,blocksNum" >> ${currOut}/res/summery.csv
    for i in `seq 1 9`; do
        echo "******* running test with ${i} channels ${j} tx in block*********"
        sed -i 's/c =.*/c = '${i}'/g' ${config}
        run_remote_cluster.sh ${config} ${txSize} ${currOut}
    done
    for i in `seq 10 2 30`; do
        echo "******* running test with ${i} channels ${j} tx in block*********"
        sed -i 's/c =.*/c = '${i}'/g' ${config}
        run_remote_cluster.sh ${config} ${txSize} ${currOut}
    done
done

#python ./scripts/python/getDiagrams.py --summery ${outputDir}