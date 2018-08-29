#!/usr/bin/env bash
args=("$@")
for i in `seq 1 $(($# - 1))`; do
    echo "******************Install server ${args[$i]} **************************"
    cat install_software.sh | sed 's/${1}/'${1}'/g' | sshpass -p ${1} ssh ${args[$i]} bash

#    chmod 777 ${wd}/${i}/run_single.sh
#    ret=`${wd}/${i}/run_single.sh ${i} ${types[$i]} "src/main/resources"` &
done
#cat install_software.sh | sed 's/${1}/'${1}'/g' | sshpass -p ${1} ssh ${2} bash
