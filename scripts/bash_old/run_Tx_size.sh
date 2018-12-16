#!/usr/bin/env bash
cd $(dirname "$0")
conf=./../../Configurations/4Servers/remote
inst=${conf}/inst/input.inst
#
echo "init" > ${inst}
echo "bm -t 40 -s 50000 -p /tmp/JToy/res/" >> ${inst}
echo "quit" >> ${inst}

run_bm.sh ${conf} 40
#
echo "init" > ${inst}
echo "bm -t 507 -s 50000 -p /tmp/JToy/res/" >> ${inst}
echo "quit" >> ${inst}

run_bm.sh ${conf} 507

echo "init" > ${inst}
echo "bm -t 1019 -s 100000 -p /tmp/JToy/res/" >> ${inst}
echo "quit" >> ${inst}

run_bm.sh ${conf} 1019