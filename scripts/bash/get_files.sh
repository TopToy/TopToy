#!/usr/bin/env bash

out_path_t=/home/yoni/toy/${2}
#readarray -t gate < ./data/gateway.txt

gate=${1}
mkdir -p ${out_path_t}
while sleep 5; do
    echo "getting files from ${gate}..."
    rsync -au ${gate}:./toy/out/* ${out_path_t}
done


