#!/usr/bin/env bash
servers=(yon_b@10.10.43.55 yon_b@10.10.43.57 yon_b@10.10.43.56 yon_b@10.10.43.58)
types=(r r r r)
ids=(0 1 2 3)
pass=yon_b@2017@
configDir=$PWD/Configurations/4Servers/remote
outputDir=/home/yoni/toy
rm -r target
rm -r -f bin
mvn install -DskipTests
rm -r -f ./bin/src/main/resources/*
cp -r $configDir/* ./bin/src/main/resources/
for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "copy bin to ${servers[$i]}"
    sshpass -p ${pass} ssh ${servers[$i]} 'rm -r -f ~/JToy'
    sshpass -p ${pass} ssh ${servers[$i]} 'mkdir ~/JToy'
    sshpass -p ${pass} scp -r bin ${servers[i]}:~/JToy
done

for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "running server ${servers[$i]} [${ids[i]}]"
    cat run_remote.sh | sed 's/${1}/'${pass}'/g' | sed 's/${2}/'${ids[i]}'/g' | sed 's/${3}/'${types[i]}'/g' | sshpass -p ${pass} ssh ${servers[$i]} bash &
done
wait

mkdir -p $outputDir/logs
mkdir -p $outputDir/res
for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "getting files from server ${servers[$i]}"
    sshpass -p ${pass} scp -r ${servers[i]}:/tmp/JToy/logs/* $outputDir/logs
    sshpass -p ${pass} scp -r ${servers[i]}:/tmp/JToy/res/* $outputDir/res
done
wait

for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "collecting results summery from server ${servers[$i]}"
    cat $outputDir/res/${ids[i]}/summery.csv >> $outputDir/res/summery.csv
    cat $outputDir/res/${ids[i]}/sig_summery.csv >> $outputDir/res/sig_summery.csv
    rm -f $outputDir/res/${ids[i]}/summery.csv
    rm -f $outputDir/res/${ids[i]}/sig_summery.csv
done




