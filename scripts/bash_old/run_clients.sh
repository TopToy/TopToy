#!/usr/bin/env bash
readarray -t clients < ./clients.txt
readarray -t servers < ./servers.txt
pass=yon_b@2017@
binDir=$PWD/../../bin_client
time=90
txSize=${1}
cl=`echo "${servers[@]}" | sed 's/yon_b\@//g'`
#for c in "${clients[@]}"; do
#    echo "running client ${c}..."
#    cat run_remote_client.sh | sed 's/${1}/'${time}'/g' | sed 's/${2}/'${txSize}'/g' | sed 's/${3}/'"${cl}"'/g' | ssh ${c} bash &
#done
cat run_remote_client.sh | sed 's/${1}/'${time}'/g' | sed 's/${2}/'${txSize}'/g' | sed 's/${3}/'"${cl}"'/g' | bash &