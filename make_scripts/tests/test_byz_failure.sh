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
# ${8} - async param
run_channels() {
    local outputDir=${output}/$(date '+%F-%H:%M:%S').byz
    mkdir -p ${outputDir}/servers
    print_servers_headers ${outputDir}

    configure_async_inst ${8} ${7} ${asdest}
    configure_tx_size ${4} ${asdest}
    configure_tmo ${5} ${asdest}
    configure_max_tx_in_block ${6} ${asdest}
    configure_async_byz_inst ${7} ${8} ${byzdest}
    configure_tx_size ${4} ${byzdest}
    configure_tmo ${5} ${byzdest}
    configure_max_tx_in_block ${6} ${byzdest}
    for i in `seq ${1} ${3} ${2}`; do
        configure_channels ${i} ${asdest}
        configure_channels ${i} ${byzdest}
        run_dockers ${compose_file_byz}
        wait
        collect_res_from_servers ${outputDir}
    done
}

run_channels 1 1 1 512 100 1000 60 0
