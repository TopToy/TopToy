#!/usr/bin/env bash

source /home/yoni/github.com/JToy/definitions.sh

compose_header() {
    echo \
"version: \"2.1\"
services:" \
        > ${1}
}

compose_correct_server() {
local id=${1}
local image=${2}
local conf=${3}
local out=${4}
echo \
"   TS${id}:
        image: ${image}
        container_name: TS_${id}
        environment:
        - ID=${id}
        - Type=r
        volumes:
        - ${out}:/tmp/JToy
        - ${conf}:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.$((${id} + 3))" \
            >> ${5}
}

compose_benign_server() {
local id=${1}
local image=${2}
local conf=${3}
local out=${4}
echo \
"   TS${id}:
        image: ${image}
        container_name: TS_${id}
        environment:
        - ID=${id}
        - Type=r
        volumes:
        - ${out}:/tmp/JToy
        - ${conf}:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.$((${id} + 3))" \
            >> ${5}
}

compose_async_server() {
local id=${1}
local image=${2}
local conf=${3}
local out=${4}
echo \
"   TS${id}:
        image: ${image}
        container_name: TS_${id}
        environment:
        - ID=${id}
        - Type=a
        volumes:
        - ${out}:/tmp/JToy
        - ${conf}:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.$((${id} + 3))" \
            >> ${5}
}

compose_footer(){
    echo \
"networks:
    toy_net:
        driver: bridge
        ipam:
            driver: default
            config:
            - subnet: 172.18.0.0/16
              gateway: 172.18.0.1" \
                  >> ${1}
}

compose_correct_dockers() {
    containers_n=$((${C} - 1))
    for i in `seq 0 ${containers_n}`; do
        compose_correct_server $i ${docker_image} ${cdest} ${docker_out} ${compose_file_correct}
    done
}

compose_benign_network() {
    containers_n=$((${C} - 1))
    for i in `seq 0 ${containers_n}`; do
        if ((i < ${F})); then
            compose_benign_server $i ${docker_image} ${fbdest} ${docker_out} ${compose_file_benign_failures}
        else
            compose_correct_server $i ${docker_image} ${cdest} ${docker_out} ${compose_file_benign_failures}
        fi
    done

}

compose_async_dockers() {
    containers_n=$((${C} - 1))
    for i in `seq 0 ${containers_n}`; do
        compose_async_server $i ${docker_image} ${asdest} ${docker_out} ${compose_file_async}
    done
}

main_correct(){
    compose_header ${compose_file_correct}
#    compose_benign_network ${C} ${F}
    compose_correct_dockers
    compose_footer ${compose_file_correct}
}

main_benign_failures(){
    compose_header ${compose_file_benign_failures}
    compose_benign_network
    compose_footer ${compose_file_benign_failures}
}

main_async(){
    compose_header ${compose_file_async}
    compose_async_dockers
    compose_footer ${compose_file_async}
}

main_correct
main_benign_failures
main_async