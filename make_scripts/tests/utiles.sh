#!/usr/bin/env bash

conf=/home/yoni/Desktop/dtoy/configurations/benign
dc=/home/yoni/github.com/JToy/docker-compose.yml
configure_inst(){
echo \
"init
serve
wait ${1}
stop
quit" > ${conf}/inst/input.inst
}

configure_channels(){
    sed -i 's/c =.*/c = '${1}'/g' ${conf}/config.toml

}

configure_tx_size() {
    sed -i 's/txSize = .*/txSize = '${1}'/g' ${conf}/config.toml
}


configure_max_tx_in_block() {
   sed -i 's/maxTransactionInBlock = .*/maxTransactionInBlock = '${1}'/g' ${conf}/config.toml
}


configure_tmo()  {
    sed -i 's/tmo =.*/tmo = '${1}'/g' ${conf}/config.toml
}

run_dockers(){
    docker-compose -f ${dc} up
}