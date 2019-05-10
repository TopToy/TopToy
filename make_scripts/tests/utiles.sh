#!/usr/bin/env bash


configure_inst(){
echo \
"init
serve
wait 30
stStart
wait ${1}
stStop
wait 30
stop
quit" > ${2}/inst/input.inst
}

configure_async_inst(){
echo \
"init
serve
stStart
async ${1} ${2}
stStop
stop
quit" > ${3}/inst/input.inst
}

configure_byz_inst(){
local tme=$((${1}/2))
echo \
"init
serve
stStart
wait ${tme}
byz
wait ${tme}
stStop
stop
quit" > ${2}/inst/input.inst
}

configure_async_byz_inst(){
local tme=$((${1}/2))
echo \
"init
serve
stStart
wait ${tme}
byz
async ${2} ${tme}
stStop
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

configure_testing()  {
    sed -i 's/testing =.*/testing = '${1}'/g' ${2}/config.toml
}

run_dockers(){
    docker-compose -f ${1} up
}
