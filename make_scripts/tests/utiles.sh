#!/usr/bin/env bash


configure_inst(){
echo \
"init
serve
stStart
wait ${1}
stStop
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

print_servers_headers() {
    local currOut=${1}
    echo "valid,id,type,workers,tmo,actTmo,maxTmo,txSize,txInBlock,txTotal,duration,tps,nob,noeb,bps,avgTxInBlock,opt,opRate,pos,posRate,neg,negRate,avgNegTime,syncs,BP2T,BP2D,BP2DL,HP2T,HP2D,HP2DL,HT2D,HD2DL,suspected" >> $currOut/servers/summary.csv

}

print_clients_headers() {
    local currOut=${1}
    echo "w,beta,cid,txSize,duration,txNum,avgLatency,maxLatency" > $currOut/clients/summary.csv

    echo "n,w,beta,cid,txID,txSize,txLatency"  > $currOut/clients/transactions.csv
}

collect_res_from_servers() {
    local currOut=${1}
    mkdir -p ${currOut}/servers/logs
    shopt -s globstar

    if [ -d "${docker_out}/logs/" ]; then
        cp -r ${docker_out}/logs/* ${currOut}/servers/logs/
    fi

    for summary in ${docker_out}/**/summary.csv ; do
        cat ${summary} >> ${currOut}/servers/summary.csv
    done
    sudo rm -r -f ${docker_out}
    shopt -u globstar
}

collect_res_from_clients() {
    local currOut=${1}
    mkdir -p ${currOut}/clients/logs
    shopt -s globstar

    if [ -d "${cdocker_out}/logs" ]; then
        cp -r ${cdocker_out}/logs/* ${currOut}/clients/logs/
    fi

    for csummary in ${cdocker_out}/**/csummary.csv ; do
        cat ${csummary} >> ${currOut}/clients/summary.csv
    done

    for txsummary in ${cdocker_out}/**/ctsummary.csv ; do
        cat ${txsummary} >> ${currOut}/clients/transactions.csv
    done
    sudo rm -r -f ${cdocker_out}
    shopt -u globstar
}
