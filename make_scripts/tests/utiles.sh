#!/usr/bin/env bash


configure_inst(){
echo \
"init
serve
wait ${1}
stop
quit" > ${2}/inst/input.inst
}

configure_async_inst(){
echo \
"init
serve
async ${1} ${2}
stop
quit" > ${3}/inst/input.inst
}

configure_byz_inst(){
local tme=$((${1}/2))
echo \
"init
serve
wait ${tme}
byz 1 -g 0 2 -g 1 3
wait ${tme}
stop
quit" > ${2}/inst/input.inst
}

configure_async_byz_inst(){
local tme=$((${1}/2))
echo \
"init
serve
wait ${tme}
byz 1 -g 0 2 -g 1 3
async ${2} ${tme}
stop
quit" > ${3}/inst/input.inst
}

configure_channels(){
    sed -i 's/c =.*/c = '${1}'/g' ${2}/config.toml

}

configure_tx_size() {
    sed -i 's/txSize = .*/txSize = '${1}'/g' ${2}/config.toml
}


configure_max_tx_in_block() {
   sed -i 's/maxTransactionInBlock = .*/maxTransactionInBlock = '${1}'/g' ${2}/config.toml
}


configure_tmo()  {
    sed -i 's/tmo =.*/tmo = '${1}'/g' ${2}/config.toml
}

run_dockers(){
    docker-compose -f ${1} up
}
