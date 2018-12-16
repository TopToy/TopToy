#!/usr/bin/env bash
readarray -t gate < ./gateway.txt
out_path=/home/yoni/toy/res
rsync -au ${gate}:./toy/out/* ${out_path}
while sleep 5; do
    echo "getting files..."
    rsync -au ${gate}:./toy/out/* ${out_path}
done
