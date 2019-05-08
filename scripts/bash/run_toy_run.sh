#!/usr/bin/env bash

gate=${1}
out_path=${3}
conf_path=${2}

echo "loading ${2} to ${1}"
./load_TOYGATEWAY.sh ${1} ${2}

echo "run benchmark"
cat ./run_toy.sh | ssh ${1} bash

echo "collect results"
mkdir -p ${3}
rsync -au ${gate}:./toy/out/* ${3}
