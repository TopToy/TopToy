#!/usr/bin/env bash
readarray -t servers < ./servers.txt
readarray -t servers_p < ./servers_privates.txt
readarray -t clients < ./clients.txt
readarray -t types < ./types.txt
readarray -t ids < ./ids.txt

tDir=./../..
configDir=${tDir}/Configurations
binDir=${tDir}/bin
clientBinDir=${tDir}/bin_client
inst=${configDir}/inst/input.inst
config_toml=${configDir}/config.toml
config_bbc=${configDir}/bbcConfig/hosts.config
#config_panic=${configDir}/panicRBConfig/hosts.config
#config_sync=${configDir}/syncRBConfig/hosts.config
config_rb=${configDir}/RBConfig/hosts.config

tconfig_bbc=${configDir}/bbcConfig/hosts.config.tmp
#tconfig_panic=${configDir}/panicRBConfig/hosts.config.tmp
#tconfig_sync=${configDir}/syncRBConfig/hosts.config.tmp
tconfig_rb=${configDir}/RBConfig/hosts.config.tmp
tconfig=${config_toml}.tmp

user="toy"


load_server() {
    local s=${servers[${1}]}
    echo "copy bin to ${s}..."
    ssh ${s} 'rm -r -f ~/JToy'
    ssh ${s} 'mkdir ~/JToy'
    scp -r ${binDir} ${s}:~/JToy  > /dev/null
}
load_servers() {
    for i in `seq 0 $((${#servers[@]} - 1))`; do
        load_server ${i}
    done
}

load_clients() {
    for c in "${clients[@]}"; do
        echo "copy bin_client to ${c}..."
        ssh ${c} 'rm -r -f ~/JToy'
        ssh ${c} 'mkdir -p ~/JToy'
        scp -r ${clientBinDir} ${c}:~/JToy > /dev/null
    done
}

kill_clients() {
    for c in "${clients[@]}"; do
        echo "kill ${c}..."
        ssh ${c} "pkill -u ${user}"
    done

}

kill_servers() {
    for s in "${servers[@]}"; do
        echo "kill ${s}..."
        ssh ${s} "pkill -u ${user}"
    done

}
install_server() {
    rm -r -f ${binDir}/src/main/resources/*
    cp -r $configDir/* ${binDir}/src/main/resources/
    load_server ${1}

}
install() {
    rm -r -f ${binDir}/src/main/resources/*
    cp -r $configDir/* ${binDir}/src/main/resources/
    load_servers
#    load_clients
}

run_remote_servers() {
    local pids=[]
    local id=0
    for s in "${servers[@]}"; do
        echo "running server ${s} [${id}]"
        cat run_remote.sh | sed 's/${2}/'${id}'/g' | sed 's/${3}/'${types[${id}]}'/g' | ssh ${s} bash &
        pids[${id}]=$!
        id=$((${id} + 1))
    done
    for pid in ${pids[*]}; do
        wait $pid
    done

}

run_remote_clients() {
    local id=0
    local pids=[]
    local cl=`echo "${servers[@]}" | sed 's/'${user}'\@//g'`
    for c in "${clients[@]}"; do
        echo "running client ${c}..."
        cat run_remote_client.sh | sed 's/${1}/'${id}'/g' | sed 's/${2}/'${2}'/g' | sed 's/${3}/'${1}'/g' | sed 's/${5}/'"${cl}"'/g' | ssh ${c} bash &
        pids[${id}]=$!
        id=$((${id} + 1))
    done
    for pid in ${pids[*]}; do
        wait $pid
    done
}

load_server_configuartion() {
    local s=${servers[${1}]}
    echo "Updating configuration of ${s}..."
    ssh ${s} 'rm -r -f ~/JToy/bin/src/main/resources/config.toml'
    scp ${config_toml} ${s}:~/JToy/bin/src/main/resources/  > /dev/null
}

load_servers_configuration() {
    for i in `seq 0 $((${#servers[@]} - 1))`; do
#        cat ${config_toml} > ${tconfig}
#        configure_server_config_toml ${i}
        load_server_configuartion ${i}
#        cat ${tconfig} > ${config_toml}
    done

}


configure_servers() {
    echo "init" > ${inst}
#    echo "wait 30" >> ${inst}
    echo "serve" >> ${inst}
    echo "wait ${1}" >> ${inst}
    echo "stop" >> ${inst}
    echo "quit" >> ${inst}
}

configure_byz_servers() {
    bef=$((${1}/2))
    echo "waits for ${bef}"
    echo "init" > ${inst}
    echo "serve" >> ${inst}
    echo "wait ${bef}" >> ${inst}
#    echo "wait 15" >> ${inst}
    echo "byz 1 -g 0 1 -g 2 3" >> ${inst}
    echo "wait ${bef}" >> ${inst}
    echo "stop" >> ${inst}
    echo "quit" >> ${inst}
}

configure_sigTest() {
    echo "sigTest" > ${inst}
    echo "quit" >> ${inst}
}
# ${1} - max time to sleep when asynchronous (seconds)
# ${2} - period of asynchronous
# ${3} - before
# ${4} - after
configure_async_servers() {
    local before=${3}
    local after=${4}
    echo "init" > ${inst}
    echo "serve" >> ${inst}
    echo "wait ${before}" >> ${inst}
    echo "async ${1} ${2}" >> ${inst}
    echo "wait ${after}" >> ${inst}
    echo "stop" >> ${inst}
    echo "quit" >> ${inst}
}

configure_tx_size() {
    sed -i 's/txSize = .*/txSize = '${1}'/g' ${config_toml}
}

configure_cutter_batch() {
    sed -i 's/cutterBatch = .*/cutterBatch = '${1}'/g' ${config_toml}
}
configure_max_tx_in_block() {
   sed -i 's/maxTransactionInBlock = .*/maxTransactionInBlock = '${1}'/g' ${config_toml}
}

configure_channels()  {
    sed -i 's/c =.*/c = '${1}'/g' ${config_toml}
}

configure_tmo()  {
    sed -i 's/tmo =.*/tmo = '${1}'/g' ${config_toml}
}

configure_server_files() {
    local id=${1}
    local public_ip=`echo "${servers[${id}]}" | sed 's/'${user}'\@//g'`
    local private_ip=${servers_p[${id}]}
    sed -i 's/'"${public_ip}"'/'"${private_ip}"'/g' ${config_bbc}
    sed -i 's/'"${public_ip}"'/'"${private_ip}"'/g' ${config_rb}
#    sed -i 's/'"${public_ip}"'/'"${private_ip}"'/g' ${config_panic}
#    sed -i 's/'"${public_ip}"'/'"${private_ip}"'/g' ${config_sync}

}

configure_server_config_toml() {
    local id=${1}
    local public_ip=`echo "${servers[${id}]}" | sed 's/'${user}'\@//g'`
    local private_ip=${servers_p[${id}]}
    sed -i 's/\"'"${public_ip}"'\"/\"'"${private_ip}"'\"/g' ${config_toml}
}

# ${1} - channels number
run_servers_instance_with_cahnnels() {
    configure_channels ${1}
    load_servers_configuration
    run_remote_servers
}

run_servers_instance_with_tmo() {
    configure_tmo ${1}
    load_servers_configuration
    run_remote_servers
}

print_headers() {
    local currOut=${1}
    echo "valid,ts,id,type,channels,tmo,fm,txSize,txInBlock,txTotal,duration,txPsec,blocksNum,avgTxInBlock,avgDelay,opRate,eRate,dRate,syncEvents" >> $currOut/servers/res/summery.csv
    echo "id,type,channels,txSize,maxTxInBlock,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" >> $currOut/servers/res/blocksStatSummery.csv
    echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" >> ${currOut}/clients/res/summery.csv
}
# ${1} - channel to start with
# ${2} - max channels
# ${3} - interval
# ${3} - output directory
run_servers_channels() {
    local currOut=${4}
    print_headers ${currOut}
    for i in `seq ${1} ${3} ${2}`; do
        chan=${i}
        echo "id,type,channels,txSize,maxTxInBlock,actualTxInBlock,height,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" >> $currOut/servers/res/blocksStat_${i}.csv
        echo "[${i} channels]"
        run_servers_instance_with_cahnnels ${i}
#        sleep 10
        collect_res_from_servers ${currOut} ${chan}
    done
}

# ${1} - tmo to start with
# ${2} - max tmo
# ${3} - interval
# ${4} - output directory
run_servers_tmo() {
    local currOut=${4}
    print_headers ${currOut}
    for i in `seq ${1} ${3} ${2}`; do
#        echo "id,type,channels,txSize,maxTxInBlock,actualTxInBlock,height,signaturePeriod,verificationPeriod,propose2tentative,tentative2permanent,channelPermanent2decide,propose2permanentchannel,propose2decide" >> $currOut/servers/res/blocksStat_${i}.csv
        echo "setting [[current tmo: ${i}]]"
        run_servers_instance_with_tmo ${i}
        collect_res_from_servers ${currOut} ${i}
    done
}

# ${1} - transaction size
# ${2} - channel to start with
# ${3} - max channel
# ${4} - channel interval
# ${5} - test time
# ${6} - output directory
# ${7} - transactions in block
run_clients_instance() {
    local currOut=${6}
    for i in `seq ${2} ${4} ${3}`; do
        echo "channels,txInBlock,ts,id,txSize,txCount,clientLatency,serverLatency,clientOnly" >> ${currOut}/clients/res/blocksStat_${i}.csv
        sleep 60
        run_remote_clients ${1} ${5}
#        wait
        kill_servers
        collect_res_from_clients ${currOut} ${i} ${7}
    done
}

shutdown() {
    for s in "${servers[@]}"; do
        echo "shutting down server ${s}..."
        ssh -o ConnectTimeout=30 ${s} 'sudo shutdown now'
    done

#    for c in "${clients[@]}"; do
#        echo "shutting down client ${s}..."
#        ssh -o ConnectTimeout=30 ${c} 'sudo shutdown now'
#    done
}


#main() {
##    load_servers
#    run_workload "$@"
#    echo "finished main"
#}

collect_res_from_single_server() {
    local currOut=${1}
    local channels=${2}
    local id=${3}
    local s=${servers[${id}]}
    echo "getting files from server ${s}"
    scp -o ConnectTimeout=30 -r ${s}:/tmp/JToy/logs/* $currOut/servers/logs  > /dev/null
    scp -o ConnectTimeout=30 -r ${s}:/tmp/JToy/res/* $currOut/servers/res  > /dev/null

    local i=${id}
    echo "collecting results summery from server ${servers[$i]}"
    cat $currOut/servers/res/${i}/summery.csv >> $currOut/servers/res/summery.csv
    cat $currOut/servers/res/${i}/sig_summery.csv >> $currOut/servers/res/sig_summery.csv
    tail -n -10 $currOut/servers/res/${i}/blocksStat.csv >> $currOut/servers/res/blocksStat_${channels}.csv
    cat $currOut/servers/res/${i}/blocksStatSummery.csv >> $currOut/servers/res/blocksStatSummery.csv
    rm -f $currOut/servers/res/${i}/summery.csv
    rm -f $currOut/servers/res/${i}/sig_summery.csv
    rm -f $currOut/servers/res/${i}/blocksStat.csv
    rm -f $currOut/servers/res/${i}/blocksStatSummery.csv
}

collect_res_from_servers() {
    local currOut=${1}
    local channels=${2}
    for s in "${servers[@]}"; do
        echo "getting files from server ${s}"
        scp -o ConnectTimeout=30 -r ${s}:/tmp/JToy/logs/* $currOut/servers/logs  > /dev/null
        scp -o ConnectTimeout=30 -r ${s}:/tmp/JToy/res/* $currOut/servers/res  > /dev/null
    done

    for i in `seq 0 $((${#servers[@]} - 1))`; do
        echo "collecting results summery from server ${servers[$i]}"
        cat $currOut/servers/res/${i}/summery.csv >> $currOut/servers/res/summery.csv
        cat $currOut/servers/res/${i}/sig_summery.csv >> $currOut/servers/res/sig_summery.csv
        cat $currOut/servers/res/${i}/blocksStat.csv >> $currOut/servers/res/blocksStat_${channels}.csv
        cat $currOut/servers/res/${i}/blocksStatSummery.csv >> $currOut/servers/res/blocksStatSummery.csv
        rm -f $currOut/servers/res/${i}/summery.csv
        rm -f $currOut/servers/res/${i}/sig_summery.csv
        rm -f $currOut/servers/res/${i}/blocksStat.csv
        rm -f $currOut/servers/res/${i}/blocksStatSummery.csv
    done
}

# ${1} - output directory
# ${2} - channels
# ${3} - transaction in block
collect_res_from_clients() {
    local currOut=${1}
    local channels=${2}
    local txInB=${3}
    for c in "${clients[@]}"; do
        echo "getting files from client ${c}"
        scp -r ${c}:/tmp/JToy/logs/* $currOut/clients/logs  > /dev/null
        scp -r ${c}:/tmp/JToy/res/* $currOut/clients/res  > /dev/null
    done

    local id=0
    for c in "${clients[@]}"; do
        echo "collecting results summery from client ${c}"
        cat $currOut/clients/res/${id}/summery.csv |  sed 's/^/'${channels}','${txInB}',/' >> $currOut/clients/res/summery.csv
        cat $currOut/clients/res/${id}/blocksStat.csv |  sed 's/^/'${channels}','${txInB}',/' >> $currOut/clients/res/blocksStat_${channels}.csv
        rm -f $currOut/clients/res/${id}/summery.csv
        rm -f $currOut/clients/res/${id}/blocksStat.csv
        id=$((${id} + 1))
    done
}

# ${1} - transaction size
# ${2} - max transactions in block
# ${3} - tmo
# ${4} - channels
# ${5} - output directory
# ${6} - before
# ${7} - after
# ${8} - async period
# ${9} - async param



run_async_test() {
    for i in `seq 0 $((${#servers[@]} - 1))`; do
        configure_tx_size ${1}
        configure_max_tx_in_block ${2}
        configure_tmo ${3}
        configure_channels ${4}
        configure_async_servers ${9} ${8} ${6} ${7}
#        if (( i < f )); then

#        else
#            configure_servers 60
#        fi
        install_server ${i}
    done
    run_servers_channels ${4} ${4} 1 ${5}
}

# ${1} - transaction size
# ${2} - transactions in block
# ${3} - channel to start with
# ${4} - max channel
# ${5} - channel interval
# ${6} - output directory
# ${7} - tmo
run_latency_test() {
    for i in `seq 0 $((${#servers[@]} - 1))`; do
        cat ${config_bbc} > ${tconfig_bbc}
        cat ${config_rb} > ${tconfig_rb}
#        cat ${config_panic} > ${tconfig_panic}
#        cat ${config_sync} > ${tconfig_sync}
        configure_tx_size ${1}
        configure_max_tx_in_block ${2}
        configure_tmo ${7}
        configure_servers 1200
        configure_server_files ${i}
        install_server ${i}
        cat ${tconfig_bbc} > ${config_bbc}
        cat ${tconfig_rb} > ${config_rb}
#        cat ${tconfig_panic} > ${config_panic}
#        cat ${tconfig_sync} > ${config_sync}
    done
    load_clients
    run_servers_channels ${3} ${4} ${5} ${6} &
    run_clients_instance ${1} ${3} ${4} ${5} 600 ${6} ${2}

}

# ${1} - transaction size
# ${2} - max transactions in block
# ${3} - tmo
# ${4} - channels to start
# ${5} - channels to end
# ${6} - interval
# ${7} - output directory
# ${8} - f
# ${9} - correct time
# ${10} - test time

run_bengin_test() {
    for i in `seq 0 $((${#servers[@]} - 1))`; do
        configure_tx_size ${1}
        configure_max_tx_in_block ${2}
        configure_tmo ${3}
        configure_channels ${4}
        if (( i < ${8} )); then
            configure_servers ${9}
        else
            configure_servers ${10}
        fi
        install_server ${i}
    done
    run_servers_channels ${4} ${5} ${6} ${7}

}


# ${1} - transaction size
# ${2} - max transactions in block
# ${3} - tmo
# ${4} - channels
# ${5} - output directory
# ${6} - f

run_byz_test() {
    for i in `seq 0 $((${#servers[@]} - 1))`; do
        configure_tx_size ${1}
        configure_max_tx_in_block ${2}
        configure_tmo ${3}
        configure_channels ${4}
        if (( i < ${6} )); then
            configure_byz_servers 60
        else
            configure_servers 60
        fi
        install_server ${i}
    done
    run_servers_channels ${4} ${4} 1 ${5}

}

# ${1} - transaction size
# ${2} - transactions in block
# ${3} - channel to start with
# ${4} - max channel
# ${5} - channel interval
# ${6} - output directory
# ${7} - tmo
run_no_failures_test() {
    for i in `seq 0 $((${#servers[@]} - 1))`; do
        cat ${config_bbc} > ${tconfig_bbc}
        cat ${config_rb} > ${tconfig_rb}
#        cat ${config_panic} > ${tconfig_panic}
#        cat ${config_sync} > ${tconfig_sync}
        configure_tx_size ${1}
        configure_max_tx_in_block ${2}
        configure_tmo ${7}
        configure_servers 60
        configure_server_files ${i}
        install_server ${i}
        cat ${tconfig_bbc} > ${config_bbc}
        cat ${tconfig_rb} > ${config_rb}
#        cat ${tconfig_panic} > ${config_panic}
#        cat ${tconfig_sync} > ${config_sync}
    done
    run_servers_channels ${3} ${4} ${5} ${6}
}

# ${1} - transaction size
# ${2} - transactions in block
# ${3} - channels
# ${4} - tmo to start with
# ${5} - max tmo
# ${6} - interval
# ${7} - output directory
run_tmo_test() {
    for i in `seq 0 $((${#servers[@]} - 1))`; do
        configure_tx_size ${1}
        configure_max_tx_in_block ${2}
        configure_channels ${3}
        configure_servers 60
        install_server ${i}
    done
    echo "setting [[txSize:${1}] [tx/block:${2}] [channels:${3}]]"
    run_servers_tmo  ${4} ${5} ${6} ${7}
}
# ${1} - transaction size
# ${2} - transactions in block
# ${3} - workers to start with
# ${4} - max worker
# ${5} - worker interval
# ${6} - outDir
# ${7} - server ids
run_sig_test() {
    local currOut=${6}
    configure_tx_size ${1}
    configure_max_tx_in_block ${2}
    configure_sigTest
    local id=${7}
    local type=r
    local s=${servers[id]}
    echo "ts,workers,txSize,maxTxInBlock,sigPerSec" >> ${currOut}/servers/res/sig_summery.csv
    echo "run [[workers1:${3}] [workers2:${4}] [interval:${5}]]"
    for i in `seq ${3} ${5} ${4}`; do
        configure_channels ${i}
        install_server ${id}
        cat run_remote.sh | sed 's/${2}/'${id}'/g' | sed 's/${3}/'type'/g' | ssh ${s} bash
        echo "run [[txSize:${1}] [txInBlock:${2}] [workers:${i}]]"
        collect_res_from_servers ${currOut} ${i}
    done
}
create_output_dir() {
    local outputDir=${tDir}/out/$(date '+%F-%H:%M:%S')
    local currOut=${outputDir}.${1}.${2}
    mkdir -p $currOut/clients/logs
    mkdir -p $currOut/clients/res
    mkdir -p $currOut/servers/logs
    mkdir -p $currOut/servers/res
    echo ${currOut}

}

create_output_dir_server() {
    local outputDir=${tDir}/out/$(date '+%F-%H:%M:%S')
    local currOut=${outputDir}.${1}.${2}.${3}
    mkdir -p $currOut/clients/logs
    mkdir -p $currOut/clients/res
    mkdir -p $currOut/servers/logs
    mkdir -p $currOut/servers/res
    echo ${currOut}

}



# ${1} - transaction size
# ${2} - transactions in block
# ${3} - channel to start with
# ${4} - max channel
# ${5} - channel interval
# ${6} - tmo
main_latency() {
    local currOut=`create_output_dir ${1} ${2}`
    run_latency_test ${1} ${2} ${3} ${4} ${5} ${currOut} ${6}
}

# ${1} - transaction size
# ${2} - transactions in block
# ${3} - channel to start with
# ${4} - max channel
# ${5} - channel interval
# ${6} - tmo
main_no_failures() {
    local currOut=`create_output_dir ${1} ${2}`
    run_no_failures_test ${1} ${2} ${3} ${4} ${5} ${currOut} ${6}
}

# ${1} - transaction size
# ${2} - transactions in block
# ${3} - channels
# ${4} - tmo to start with
# ${5} - max tmo
# ${6} - interval
main_run_tmo_tunning() {
    local currOut=`create_output_dir ${1} ${2}`
    run_tmo_test ${1} ${2} ${3} ${4} ${5} ${6} ${currOut}
}

# ${1} - transaction size
# ${2} - transactions in block
# ${3} - workers to start with
# ${4} - max worker
# ${5} - worker interval
# ${6} - server id
main_run_sig_test() {
    local currOut=`create_output_dir ${1} ${2}`
    run_sig_test ${1} ${2} ${3} ${4} ${5} ${currOut} 0
}


# ${1} - transaction size
# ${2} - max transactions in block
# ${3} - tmo
# ${4} - channels to start
# ${5} - channels to end
# ${6} - interval
# ${7} - f
# ${8} - correct time
# ${9} - test time

main_bengin() {
   local outputDir=${tDir}/out/$(date '+%F-%H:%M:%S')
   local currOut=${outputDir}.${1}.${2}.bengin
   mkdir -p $currOut/clients/logs
   mkdir -p $currOut/clients/res
   mkdir -p $currOut/servers/logs
   mkdir -p $currOut/servers/res
   echo ${currOut}
   run_bengin_test ${1} ${2} ${3} ${4} ${5} ${6} ${currOut} ${7} ${8} ${9}
}

# ${1} - transaction size
# ${2} - transactions in block
# ${3} - tmo
# ${4} - channels
# ${5} - before
# ${6} - after
# ${7} - async period
# ${8} - async param
# ${9} - mode
main_async() {
   local outputDir=${tDir}/out/$(date '+%F-%H:%M:%S')
   local currOut=${outputDir}.${1}.${2}.async
   mkdir -p $currOut/clients/logs
   mkdir -p $currOut/clients/res
   mkdir -p $currOut/servers/logs
   mkdir -p $currOut/servers/res
   echo ${currOut}
    run_async_test ${1} ${2} ${3} ${4} ${currOut} ${5} ${6} ${7} ${8}
}

# ${1} - transaction size
# ${2} - max transactions in block
# ${3} - tmo
# ${4} - channels
# ${5} - f

main_byz() {
   local outputDir=${tDir}/out/$(date '+%F-%H:%M:%S')
   local currOut=${outputDir}.${1}.${2}.byz
   mkdir -p $currOut/clients/logs
   mkdir -p $currOut/clients/res
   mkdir -p $currOut/servers/logs
   mkdir -p $currOut/servers/res
   echo ${currOut}
   run_byz_test ${1} ${2} ${3} ${4} ${currOut} ${5}
}

main_bengin 500 1000 400 15 20 1 1 1 60

#for i in `seq 0 2`; do
#    main_no_failures 0 10 1 1 1 2000
#    main_no_failures 0 100 1 1 1 2000
#    main_no_failures 0 1000 1 1 1 2000
#done
#main_no_failures 500 1000 1 10 1 1000
#for i in `seq 0 0`; do
#    main_no_failures 0 10 2 20 2 2000
#    main_no_failures 0 100 2 20 2 2000
#    main_no_failures 0 1000 2 20 2 2000
#done

#for i in `seq 0 2`; do
#    main_no_failures 500 10 1 1 1 2000
#    main_no_failures 500 100 1 1 1 2000
#    main_no_failures 500 1000 1 1 1 2000
#done
#
#for i in `seq 0 0`; do
##    main_no_failures 500 10 2 20 2 2000
##    main_no_failures 500 100 2 20 2 2000
#    main_no_failures 500 1000 2 20 2 2000
#done
###
##
#for i in `seq 0 2`; do
#    main_no_failures 1012 10 1 1 1 2000
#    main_no_failures 1012 100 1 1 1 2000
#    main_no_failures 1012 1000 1 1 1 2000
#done
#
#for i in `seq 0 2`; do
#    main_no_failures 1012 10 2 20 2 2000
#    main_no_failures 1012 100 2 20 2 2000
#    main_no_failures 1012 1000 2 20 2 2000
#done
##
#for i in `seq 0 2`; do
#    main_no_failures 4084 10 1 1 1 2000
#    main_no_failures 4084 100 1 1 1 2000
#    main_no_failures 4084 1000 1 1 1 2000
#done
#
#for i in `seq 0 2`; do
#    main_no_failures 4084 10 2 20 2 2000
#    main_no_failures 4084 100 2 20 2 2000
#    main_no_failures 4084 1000 2 20 2 2000
#done
#
#    main_no_failures 1012 1000 16 16 1 2000
#    main_no_failures 4084 1000 12 12 1 2000

#for i in `seq 0 2`; do
#    main_byz 500 100 200 1 1
#    main_byz 500 100 400 5 1
#    main_byz 500 100 600 10 1
#done
#
#for i in `seq 0 2`; do
#    main_byz 500 1000 200 1 1
#    main_byz 500 1000 500 5 1
#    main_byz 500 1000 900 10 1
#done

# ${1} - transaction size
# ${2} - max transactions in block
# ${3} - tmo
# ${4} - channels to start
# ${5} - channels to end
# ${6} - interval
# ${7} - f
# ${8} - correct time
# ${9} - test time


#main_bengin 500 100 400 5 1
#main_bengin 500 100 500 10 1

#main_byz 500 100 200 1 1
#main_byz 500 100 400 5 1
#main_byz 500 100 500 10 1
#
##main_bengin 500 1000 200 1 1
##main_bengin 500 1000 500 5 1
##main_bengin 500 1000 900 10 1
#


#main_bengin 500 100 400 10 1
#main_bengin 500 1000 500 10 1
#
#main_bengin 500 100 600 20 1
#main_bengin 500 1000 900 20 1

#for i in `seq 0 5`; do
#    main_async 500 100 400 10 0 0 60 3
#    main_async 500 1000 500 10 0 0 60 3
#    main_async 500 100 600 20 20 10 30 3
#    main_async 500 1000 900 20 20 10 30 3
#done
#
#for i in `seq 0 3`; do
#main_async 500 100 200 5 20 10 30 1
#main_async 500 100 400 10 20 10 30 1
#main_async 500 100 500 15 20 10 30 2
#main_async 500 100 600 20 20 10 30 3
#main_async 500 1000 300 5 20 10 30 1
#main_async 500 1000 500 10 20 10 30 2
#main_async 500 1000 800 15 20 10 30 3
#main_async 500 1000 900 20 20 10 30 3
#done


#for i in `seq 0 2`; do
#    main_run_sig_test 0 10 1 4 1
#    main_run_sig_test 0 100 1 4 1
#    main_run_sig_test 0 1000 1 4 1
#
#    main_run_sig_test 512 10 1 4 1
#    main_run_sig_test 512 100 1 4 1
#    main_run_sig_test 512 1000 1 4 1
#
#
#    main_run_sig_test 1024 10 1 4 1
#    main_run_sig_test 1024 100 1 4 1
#    main_run_sig_test 1024 1000 1 4 1
#
#
#    main_run_sig_test 4096 10 1 4 1
#    main_run_sig_test 4096 100 1 4 1
#    main_run_sig_test 4096 1000 1 4 1
#done

# ${1} - transaction size
# ${2} - transactions in block
# ${3} - channel to start with
# ${4} - max channel
# ${5} - channel interval
# ${6} - tmo

#for i in `seq 0 2`; do
#    main_latency 500 100 1 1 1 15000
#    main_latency 500 100 5 5 1 15000
#
#    main_latency 500 1000 1 1 1 15000
#    main_latency 500 1000 5 5 1 15000
#done



#shutdown