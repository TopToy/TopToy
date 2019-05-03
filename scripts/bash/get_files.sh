#!/usr/bin/env bash

out_path_t=/home/yoni/toy/correct/7
readarray -t gate < ./gateway.txt

mkdir -p ${out_path_t}
while sleep 5; do
    echo "getting files from ${gate}..."
    rsync -au ${gate}:./toy/out/* ${out_path_t}
done


