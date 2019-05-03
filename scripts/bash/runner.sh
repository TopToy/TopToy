#!/usr/bin/env bash

. /home/yoni/github.com/JToy/scripts/bash/definitions.sh

run() {
    while read -r line; do
        local name=`echo $line | cut -f 1 -d " "`
        local t=`echo $line | cut -f 2 -d " "`

        if [[ $line == '#'* ]] ; then
            echo $"skipping ${name}..."
        else
            echo "running ${name}..."
            sudo chmod 777 ${tests_dir}/${t}.sh
            ${tests_dir}/${t}.sh
            echo "done..."
        fi
    done < "${tests_conf}"
}

run