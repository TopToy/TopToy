#!/usr/bin/env bash
cd $(dirname "$0")
#servers=(yon_b@10.10.43.21 yon_b@10.10.43.35 yon_b@10.10.43.44 yon_b@10.10.43.46)
readarray -t servers < ./servers.txt
readarray -t types < ./types.txt
readarray -t ids < ./ids.txt

#types=(r r r r)
#ids=(0 1 2 3)
#pass=yon_b@2017@
config=${1} #./../../Configurations/4Servers/remote/config.toml
outputDir=./../../out
binDir=./../../bin
txSize=${2}
if [ "$#" -eq 3 ] ; then
    outputDir=${3}
fi
echo "output directory is ${outputDir}"
#rm -r target
#rm -r -f bin
#mvn install -DskipTests > /dev/null
#if [[ "$?" -ne 0 ]] ; then
#  echo 'could compile the project'; exit $1
#fi
#rm -r -f ./bin/src/main/resources/*
#cp -r $configDir/* ${binDir}/src/main/resources/
#for i in `seq 0 $((${#servers[@]} - 1))`; do
#    echo "copy bin to ${servers[$i]}"
#    sshpass -p ${pass} ssh -oStrictHostKeyChecking=no ${servers[$i]} 'rm -r -f ~/JToy'
#    sshpass -p ${pass} ssh -oStrictHostKeyChecking=no  ${servers[$i]} 'mkdir ~/JToy'
#    sshpass -p ${pass} scp -oStrictHostKeyChecking=no -r bin ${servers[i]}:~/JToy
#done
for s in "${servers[@]}"; do
    echo "Updating configuration of ${s}..."
    ssh -oStrictHostKeyChecking=no ${s} 'rm -r -f ~/JToy/bin/src/main/resources/config.toml'
    scp -oStrictHostKeyChecking=no ${config} ${s}:~/JToy/bin/src/main/resources/  > /dev/null
done

for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "running server ${servers[$i]} [${ids[i]}]"
    cat run_remote.sh | sed 's/${2}/'${ids[i]}'/g' | sed 's/${3}/'${types[i]}'/g' | ssh ${servers[$i]} bash &
done
#./run_clients.sh ${txSize}
wait

mkdir -p $outputDir/logs
mkdir -p $outputDir/res
for s in "${servers[@]}"; do
    echo "getting files from server ${s}"
    scp -oStrictHostKeyChecking=no -r ${s}:/tmp/JToy/logs/* $outputDir/logs  > /dev/null
    scp -oStrictHostKeyChecking=no -r ${s}:/tmp/JToy/res/* $outputDir/res  > /dev/null
done
wait

for i in `seq 0 $((${#servers[@]} - 1))`; do
    echo "collecting results summery from server ${servers[$i]}"
    cat $outputDir/res/${ids[i]}/summery.csv >> $outputDir/res/summery.csv
    cat $outputDir/res/${ids[i]}/sig_summery.csv >> $outputDir/res/sig_summery.csv
    rm -f $outputDir/res/${ids[i]}/summery.csv
    rm -f $outputDir/res/${ids[i]}/sig_summery.csv
done