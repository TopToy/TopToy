#!/usr/bin/env bash

source $PWD/make_scripts/tests/utiles.sh
source $PWD/definitions.sh
# ${1} - start worker
# ${2} - end worker
# ${3} - interval
# ${4} - tx_size
# ${5} - tmo
# ${6} - block_size
# ${7} - test_time
run_channels() {
    local outputDir=${output}/$(date '+%F-%H:%M:%S').correct
    mkdir -p ${outputDir}/servers
    print_servers_headers ${outputDir}

    configure_inst ${7} ${cdest}
    configure_tx_size ${4} ${cdest}
    configure_tmo ${5} ${cdest}
    configure_max_tx_in_block ${6} ${cdest}
    for i in `seq ${1} ${3} ${2}`; do
        configure_channels ${i} ${cdest}
        run_dockers ${compose_file_correct}
        wait
        collect_res_from_servers ${outputDir}
    done
}

run_channels 1 1 1 512 10 1000 60