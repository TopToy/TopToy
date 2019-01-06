#!/usr/bin/env bash
#readarray -t gate < ./gateway.txt
out_path_fn=/home/yoni/toy/res_fail
gate_fn=toy@54.183.203.17
#rsync -au ${gate}:./toy/out/* ${out_path}
while sleep 5; do
#    echo "getting files from ${gate_f}..."
#    rsync -au ${gate_f}:./toy/out/* ${out_path_f}

    echo "getting files from ${gate_fn}..."
    rsync -au ${gate_fn}:./toy/out/* ${out_path_fn}
done


