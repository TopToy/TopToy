#!/usr/bin/env bash

source $PWD/definitions.sh

main() {
    mvn -f ${BASE_DIR} install

    rm -r ${RESOURCES_DIR}/* && \
    sudo chmod 777 ${BIN_DIR}/run_docker.sh && \
    sudo chmod 777 ${BIN_DIR}/run_single.sh

    rm -r ${CRESOURCES_DIR}/* && \
    sudo chmod 777 ${CBIN_DIR}/run_cdocker.sh && \
    sudo chmod 777 ${CBIN_DIR}/run_client.sh

}

main