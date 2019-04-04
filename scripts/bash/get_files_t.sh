#!/usr/bin/env bash
#readarray -t gate < ./gateway.txt
out_path_t=/home/yoni/toy/res
gate_t=toy@18.220.84.252
#rsync -au ${gate}:./toy/out/* ${out_path}
while sleep 5; do
#    echo "getting files from ${gate_f}..."
#    rsync -au ${gate_f}:./toy/out/* ${out_path_f}

    echo "getting files from ${gate_t}..."
    rsync -au ${gate_t}:./toy/out/* ${out_path_t}
done


