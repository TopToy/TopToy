#!/usr/bin/env bash
servers=(yon_b@10.10.43.55 yon_b@10.10.43.57 yon_b@10.10.43.56 yon_b@10.10.43.58)
types=(r r r r)
ids=(0 1 2 3)
pass=yon_b@2017@
rm -r -f bin
mvn install -DskipTests

for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "copy bin to ${servers[$i]}"
    sshpass -p ${pass} ssh ${servers[$i]} rm -r -f JToy
    sshpass -p ${pass} ssh ${servers[$i]} mkdir JToy
    sshpass -p ${pass} scp -r bin ${servers[i]}:~/JToy
done

for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "running server ${servers[$i]} [${ids[i]}]"
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
wait

for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "collecting results summery from server ${servers[$i]}"
    cat /home/yoni/toy/res/${ids[i]}/summery.csv >> /home/yoni/toy/res/summery.csv
done




