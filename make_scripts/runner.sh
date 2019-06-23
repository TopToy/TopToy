#!/usr/bin/env bash

source $PWD/definitions.sh

run2() {
    docker network create toy_net --gateway 172.18.0.1 --subnet 172.18.0.0/16
    while read -r line; do
        sudo rm -r -f ${docker_out}
        sudo rm -r -f ${cdocker_out}
        mkdir -p ${docker_out}
        mkdir -p ${cdocker_out}
        local name=`echo $line | cut -f 1 -d " "`
        local t=`echo $line | cut -f 2 -d " "`

        if [[ $line == '#'* ]] ; then
            echo $"skipping ${name}..."
        else
            echo "running ${name}..."
            sudo chmod 777 ${test_dir}/$t.sh
            ${test_dir}/${t}.sh
            echo "done..."
        fi
    done < "${tests_conf}"

}






run2