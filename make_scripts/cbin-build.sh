#!/usr/bin/env bash


source /home/yoni/github.com/JToy/definitions.sh
main() {
#    mvn -f ${BASE_DIR} install && \
    rm -r ${CRESOURCES_DIR}/* && \
    sudo chmod 777 ${CBIN_DIR}/run_cdocker.sh && \
    sudo chmod 777 ${CBIN_DIR}/run_client.sh
}

main