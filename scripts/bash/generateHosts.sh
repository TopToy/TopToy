#!/usr/bin/env bash
rm hosts.txt
inputIpsPath=${1}
i=0
while read line
do
echo "${i} ${line} ${2}" >> hosts.txt
i=$((i+1))
done < ${inputIpsPath}