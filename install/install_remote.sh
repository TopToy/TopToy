#!/usr/bin/env bash
cd $(dirname "$0")
readarray -t servers < ./servers.txt
for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "******************Install server ${servers[$i]} **************************"
    cat install_software.sh | ssh ${servers[$i]} bash

#    chmod 777 ${wd}/${i}/run_single.sh
#    ret=`${wd}/${i}/run_single.sh ${i} ${types[$i]} "src/main/resources"` &
done
#cat install_software.sh | sed 's/${1}/'${1}'/g' | sshpass -p ${1} ssh ${2} bash
