#!/usr/bin/env bash
cd $(dirname "$0")
servers=(yon_b@10.10.43.55 yon_b@10.10.43.57 yon_b@10.10.43.56 yon_b@10.10.43.58)
pass=yon_b@2017@
for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "******************Install server ${servers[$i]} **************************"
    cat install_software.sh | sed 's/${1}/'${pass}'/g' | sshpass -p ${pass} ssh ${servers[$i]} bash

#    chmod 777 ${wd}/${i}/run_single.sh
#    ret=`${wd}/${i}/run_single.sh ${i} ${types[$i]} "src/main/resources"` &
done
#cat install_software.sh | sed 's/${1}/'${1}'/g' | sshpass -p ${1} ssh ${2} bash
