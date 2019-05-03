#!/usr/bin/env bash

kill_clients() {
    local clients
    readarray -t clients < ${clients_ips}
    for c in "${clients[@]}"; do
        echo "kill ${c}..."
        ssh ${c} "pkill -u ${user}"
    done

}

kill_servers() {
    local servers
    readarray -t servers < ${servers_ips}
    for s in "${servers[@]}"; do
        echo "kill ${s}..."
        ssh ${s} "pkill -u ${user}"
    done

}

kill_servers
kill_clients