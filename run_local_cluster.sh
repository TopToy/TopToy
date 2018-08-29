#!/usr/bin/env bash

cd $(dirname "$0")
args=("$@")
wd=${1}
servers=${2}
types=("${args[@]:2:$servers}")

#echo $((servers - 1))
# Create local cluster

#echo "Compiling sources" ${i}

rm -rf ${wd}
mvn install -DskipTests
find ./bin -type f -name 'currentView' -delete
for i in `seq 0 $((servers - 1))`; do
    echo "Creating server" ${i}
    mkdir -p ${wd}/${i}
    cp -r bin/* ${wd}/${i}
done


for i in `seq 0 $((servers - 1))`; do
    echo "Starting server" ${i}

    chmod 777 ${wd}/${i}/run_single.sh
    ret=`${wd}/${i}/run_single.sh ${i} ${types[$i]} "src/main/resources"` &
done
wait
rm -r -f ./bin