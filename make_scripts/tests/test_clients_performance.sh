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
    local outputDir=${output}/$(date '+%F-%H:%M:%S').clients
    mkdir -p ${outputDir}/servers
    mkdir -p ${outputDir}/clients
    print_servers_headers ${outputDir}
    print_clients_headers ${outputDir}

    configure_inst ${7} ${cdest}
    configure_tx_size ${4} ${cdest}
    configure_tmo ${5} ${cdest}
    configure_max_tx_in_block ${6} ${cdest}
    configure_testing 0 ${cdest}

    configure_tx_size ${4} ${cldest}
    configure_tmo ${5} ${cldest}
    configure_max_tx_in_block ${6} ${cldest}
    configure_testing 0 ${cldest}

    for i in `seq ${1} ${3} ${2}`; do
        configure_channels ${i} ${cdest}
        configure_channels ${i} ${cldest}
        docker-compose -f ${compose_file_correct} up &
        pid=$!
        sleep 30
        docker-compose -f ${compose_file_standalone_clients_p} up
        kill -9 ${pid}
        docker stop $(docker ps -q --filter ancestor=${docker_image})
        collect_res_from_servers ${outputDir}
        collect_res_from_clients ${outputDir}
    done
}

run_channels 1 1 1 512 10 10 120