#!/usr/bin/env bash

folder=${1}
workers=${2}

readarray -t logs <<< `find ${folder} -name "*.log" -not -name "*.err.log"`

for log in "${logs[@]}"; do
    d_name=$(dirname "$log")
    f_name=$(basename "${log}")
    echo "$d_name"
    for w in `seq 0 "$(($workers - 1))"`; do
        grep "C\[${w}\]" $log > "$d_name/${w}.${f_name}"
    done
done