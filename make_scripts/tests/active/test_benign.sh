#!/usr/bin/env bash

source /home/yoni/Desktop/Research/github.com/JToy/make_scripts/tests/utiles.sh

# ${1} - start worker
# ${2} - end worker
# ${3} - interval
# ${4} - tx_size
# ${5} - tmo
# ${6} - block_size
# ${7} - test_time
run_channels() {
    configure_inst ${7}
    configure_tx_size ${4}
    configure_tmo ${5}
    configure_max_tx_in_block ${6}
    for i in `seq ${1} ${3} ${2}`; do
        configure_channels ${i}
        run_dockers
        wait
    done
}

run_channels 1 2 1 0 1000 1000 10