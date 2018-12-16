#!/usr/bin/env bash

cd $(dirname "$0")
args=("$@")
wd=${1}
servers=${3}
resources=${2}
types=("${args[@]:3:$servers}")
#echo $((servers - 1))
# Create local cluster

#echo "Compiling sources" ${i}

rm -rf ${wd}
mvn install -DskipTests
if [ "$resources" != '-d' ]; then
    rm -r -f ./bin/src/main/resources/*
    cp -r $PWD/$resources/* ./bin/src/main/resources/
fi

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

for i in `seq 0 $((servers - 1))`; do
    echo "collecting results summery from server ${i}"
    cat /home/yoni/Desktop/toy/res/${i}/summery.csv >> /home/yoni/Desktop/toy/res/summery.csv
    rm -f  /home/yoni/Desktop/toy/res/${i}/summery.csv
done

rm -r -f ./bin