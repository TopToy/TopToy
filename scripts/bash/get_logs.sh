#!/usr/bin/env bash

out_path_t=/home/yon/toy/${2}
#readarray -t gate < ./data/gateway.txt

gate=${1}
mkdir -p ${out_path_t}/logs
while sleep 5; do
    echo "getting files from ${gate}..."
    rsync -au ${gate}:./toy/out/logs ${out_path_t}
done


