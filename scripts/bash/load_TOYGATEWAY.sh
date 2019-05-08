#!/usr/bin/env bash
tDir=$PWD/../../
#config=${tDir}/configurations/aws/4Servers2
#config=${tDir}/configurations/aws/1Servers
#config=${tDir}/configurations/aws/gd/4Servers
#config=${tDir}/configurations/aws/7ServersGD
#config=${tDir}/configurations/aws/10ServersGD
#config=${tDir}/configurations/aws/4Servers
#config=${tDir}/configurations/aws/7Servers
#config=${tDir}/configurations/aws/10Servers
#config=${tDir}/configurations/aws/49Servers
#config=${tDir}/configurations/aws/100Servers
#config=${tDir}/configurations/4Servers/remote
gate=${1}
config=${2}
#readarray -t gate < ./data/gateway.txt
mvn install -f ${tDir}/pom.xml -DskipTests
if [[ "$?" -ne 0 ]] ; then
  echo 'could compile the project'; exit $1
fi
ssh ${gate} "rm -r -f ./toy"
ssh ${gate} "mkdir -p ./toy/scripts"
ssh ${gate} "mkdir -p ./toy/configurations"

echo "copy bin to ${gate}..."
scp  -r ${tDir}/bin ${gate}:./toy > /dev/null
#echo "copy cbin to ${gate}..."
#scp  -r ${tDir}/cbin ${gate}:./toy > /dev/null
echo "copy configurations to ${gate}..."
scp  -r ${config}/* ${gate}:./toy/configurations > /dev/null
echo "copy scripts to ${gate}..."
scp  -r ${tDir}/scripts/bash ${gate}:./toy/scripts > /dev/null