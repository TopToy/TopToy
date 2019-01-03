#!/usr/bin/env bash
#readarray -t gate < ./gateway.txt
out_path_f=/home/yoni/toy/res_10b
gate_f=toy@52.14.26.110
#rsync -au ${gate}:./toy/out/* ${out_path}
while sleep 5; do
    echo "getting files from ${gate_f}..."
    rsync -au ${gate_f}:./toy/out/* ${out_path_f}

#    echo "getting files from ${gate_s}..."
#    rsync -au ${gate_s}:./toy/out/* ${out_path_s}

#    echo "getting files from ${gate_t}..."
#    rsync -au ${gate_t}:./toy/out/* ${out_path_t}
done


