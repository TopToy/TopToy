#!/usr/bin/env bash

# ${1} - instruction file
# ${2} - before time
# ${3} - benchmark time
# ${4} - after time
conf_basic_inst() {
    echo "init" > ${1}
    echo "serve" >> ${1}
    echo "wait ${2}" >> ${1}
    echo "stStart" >> ${1}
    echo "wait ${3}" >> ${1}
    echo "stStop" >> ${1}
    echo "wait ${4}" >> ${1}
    echo "stop" >> ${1}
    echo "quit" >> ${1}
}

# ${1} - config file
# ${2} - txSize
conf_tx_size() {
    sed -i 's/txSize = .*/txSize = '${2}'/g' ${1}
}

# ${1} - config file
# ${2} - maxTransactionInBlock
conf_maxTransactionInBlock() {
   sed -i 's/maxTransactionInBlock = .*/maxTransactionInBlock = '${2}'/g' ${1}
}

# ${1} - config file
# ${2} - maxTransactionInBlock
conf_workers()  {
    sed -i 's/c =.*/c = '${2}'/g' ${1}
}

# ${1} - config file
# ${2} - tmo
conf_tmo()  {
    sed -i 's/tmo =.*/tmo = '${2}'/g' ${1}
}

# ${1} - hosts file
# ${2} - server prip
# ${3} - server pip
conf_server_files_with_private_ip() {
    sed -i 's/'"${3}"'/'"${1}"'/g' ${1}
}