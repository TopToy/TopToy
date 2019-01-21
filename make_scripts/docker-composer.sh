#!/usr/bin/env bash

base_dir=..
bengin_conf=/home/yoni/Desktop/dtoy/configurations/benign
#dockers_in_correct=/home/yoni/Desktop/dtoy/intsructions/correct/
docker_out=/home/yoni/Desktop/dtoy/curr/
docker_image=toy:0.1

compose_benign_dockers() {
    containers_n=$((${1} - 1))
#    rm $base_dir/docker-compose.yml
    echo \
"version: \"2.1\"
services:" \
        > $base_dir/docker-compose.yml
    for i in `seq 0 ${containers_n}`; do
        echo \
"   TS${i}:
        image: ${docker_image}
        container_name: TS_${i}
        environment:
        - ID=${i}
        - Type=r
        volumes:
        - ${docker_out}:/tmp/JToy
        - ${bengin_conf}:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.$((${i} + 3))" \
            >> $base_dir/docker-compose.yml
    done

    echo \
"networks:
    toy_net:
        driver: bridge
        ipam:
            driver: default
            config:
            - subnet: 172.18.0.0/16
              gateway: 172.18.0.1" \
                  >> $base_dir/docker-compose.yml


}

main(){
    base_dir=${1}
    C=${2}
    compose_benign_dockers ${C}
}

main ${1} ${2}