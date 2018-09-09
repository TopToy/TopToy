#!/usr/bin/env bash
config=$PWD/Configurations/4Servers/remote/config.toml
outputDir=/home/yoni/toy
mkdir -p ${outputDir}/out

for i in `seq 1 10`; do
    echo "running test with ${i} channels"
    sed -i 's/c =.*/c = '${i}'/g' ${config}
    ./run_remote_cluster.sh > "${outputDir}/out/$(date '+%F-%H-%M-%S').out"
done