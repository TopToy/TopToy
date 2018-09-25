#!/usr/bin/env bash
config=$PWD/Configurations/4Servers/remote/config.toml
outputDir=/home/yoni/toy/$(date '+%F-%H:%M:%S')

mkdir -p ${outputDir}

for i in `seq 1 20`; do
    echo "******* running test with ${i} channels *********"
    sed -i 's/c =.*/c = '${i}'/g' ${config}
    ./run_remote_cluster.sh ${outputDir}
done