#!/usr/bin/env bash
readarray -t servers < ./servers.txt
readarray -t clients < ./clients.txt
user="toy"
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

kill_servers
kill_clients