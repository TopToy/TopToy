#!/usr/bin/env bash

source /home/yoni/github.com/JToy/make_scripts/tests/utiles.sh
source /home/yoni/github.com/JToy/definitions.sh
# ${1} - start worker
# ${2} - end worker
# ${3} - interval
# ${4} - tx_size
# ${5} - tmo
# ${6} - block_size
# ${7} - test_time
run_channels() {
    configure_inst ${7} ${cdest}
    configure_tx_size ${4} ${cdest}
    configure_tmo ${5} ${cdest}
    configure_max_tx_in_block ${6} ${cdest}
    configure_inst ${8} ${fbdest}
    configure_tx_size ${4} ${fbdest}
    configure_tmo ${5} ${fbdest}
    configure_max_tx_in_block ${6} ${fbdest}
    for i in `seq ${1} ${3} ${2}`; do
        configure_channels ${i} ${cdest}
        configure_channels ${i} ${fbdest}
        run_dockers ${compose_file_benign_failures}
        wait
    done
}

run_channels 1 1 1 0 1000 1000 60 30