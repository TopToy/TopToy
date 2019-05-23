#!/usr/bin/env bash

. utils/remot.sh
. utils/config.sh

# ${1} - tx size
# ${2} - max tx/block
# ${3} - start tmo
# ${4} - before time
# ${5} - benchmark time
# ${6} - after time
# ${7} - worker start
# ${8} - worker end
# ${9} - worker interval
sc_no_failure() {
    conf_file=${config_dir}/${n}/config.toml
    conf_tx_size ${conf_file} ${1}
    conf_maxTransactionInBlock ${conf_file} ${2}
    conf_tmo ${conf_file} ${3}
    conf_basic_inst ${config_dir}/${n}/inst/input.inst ${4} ${5} ${6}
    update_bin_resources ${config_dir}/${n}
    load_bin_to_servers
#
#    local types=()
#    for i in `seq 0 $((${n} - 1))`; do
#        types+=(r)
#    done
#
#    local conn=()
#    get_conn_array conn
#
#    for i in `seq ${7} ${9} ${8}`; do
#        conf_workers ${conf_file} ${i}
#        load_servers_conf ${conf_file}
#        echo "Running ${i} workers"
#        run_cluster ${conn[*]} ${types[*]}
#        collect_res_from_servers ${conn[*]}
#    done

}

