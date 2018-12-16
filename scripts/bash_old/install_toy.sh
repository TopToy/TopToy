#!/usr/bin/env bash
readarray -t servers < ./servers.txt
readarray -t clients < ./clients.txt
#pass=yon_b@2017@
tDir=./../..
configDir=${1}
binDir=${tDir}/bin
clientBinDir=${tDir}/bin_client
#rm -r ${tDir}/target
#rm -r -f ${binDir}
#rm -r -f ${clientBinDir}
#mvn install -f ${tDir}/pom.xml -DskipTests > /dev/null
#if [[ "$?" -ne 0 ]] ; then
#  echo 'could compile the project'; exit $1
#fi
rm -r -f ${binDir}/src/main/resources/*
cp -r $configDir/* ${binDir}/src/main/resources/

for s in "${servers[@]}"; do
    echo "copy bin to ${s}..."
    ssh -oStrictHostKeyChecking=no ${s} 'rm -r -f ~/JToy'
    ssh -oStrictHostKeyChecking=no ${s} 'mkdir ~/JToy'
    scp -oStrictHostKeyChecking=no -r ${binDir} ${s}:~/JToy  > /dev/null
done
#
#for c in "${clients[@]}"; do
#    echo "copy bin to ${c}..."
#    ssh -oStrictHostKeyChecking=no ${c} 'rm -r -f ~/JToy'
#    ssh -oStrictHostKeyChecking=no ${c} 'mkdir -p ~/JToy'
#    scp -oStrictHostKeyChecking=no -r ${clientBinDir} ${c}:~/JToy > /dev/null
#done
