#!/usr/bin/env bash
servers=(yon_b@10.10.43.55) # yon_b@10.10.43.57 yon_b@10.10.43.56 yon_b@10.10.43.58)
types=(r r r r)
ids=(0 0 0 0)
pass=yon_b@2017@
for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "compiling server ${servers[$i]}"
    echo "cd JToy && rm -r -f ./target && mvn install -DskipTests" | sshpass -p ${pass} ssh ${servers[$i]} bash
done

for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "running server ${servers[$i]}"
    cat run_remote.sh | sed 's/${1}/'${pass}'/g' | sed 's/${2}/'${ids[i]}'/g' | sed 's/${3}/'${types[i]}'/g' | sshpass -p ${pass} ssh ${servers[$i]} bash &
done
wait

mkdir -p /home/yoni/toy/logs
mkdir -p /home/yoni/toy/res
for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "getting files from server ${servers[$i]}"
    sshpass -p ${pass} scp -r ${servers[i]}:/tmp/JToy/logs/* /home/yoni/toy/logs
    sshpass -p ${pass} scp -r ${servers[i]}:/tmp/JToy/res/* /home/yoni/toy/res
done

for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "collecting results summery from server ${servers[$i]}"
    cat /home/yoni/toy/res/${ids[i]}/summery.csv > /home/yoni/toy/res/summery.csv
done




