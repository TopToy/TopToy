#!/usr/bin/env bash
tDir=$PWD/../../
#config=${tDir}/Configurations/aws/4Servers2
#config=${tDir}/Configurations/aws/1Servers
#config=${tDir}/Configurations/aws/4ServersGD
#config=${tDir}/Configurations/aws/7ServersGD
#config=${tDir}/Configurations/aws/10ServersGD
#config=${tDir}/Configurations/aws/4Servers
#config=${tDir}/Configurations/aws/7Servers
#config=${tDir}/Configurations/aws/10Servers
config=${tDir}/Configurations/aws/49Servers
#config=${tDir}/Configurations/aws/100Servers
#config=${tDir}/Configurations/4Servers/remote
readarray -t gate < ./gateway.txt
mvn install -f ${tDir}/pom.xml -DskipTests > /dev/null
if [[ "$?" -ne 0 ]] ; then
  echo 'could compile the project'; exit $1
fi
ssh ${gate} "rm -r -f ./toy"
ssh ${gate} "mkdir -p ./toy/scripts"
ssh ${gate} "mkdir -p ./toy/Configurations"

echo "copy bin to ${gate}..."
scp  -r ${tDir}/bin ${gate}:./toy > /dev/null
echo "copy bin_client to ${gate}..."
scp  -r ${tDir}/bin_client ${gate}:./toy > /dev/null
echo "copy Configuration to ${gate}..."
scp  -r ${config}/* ${gate}:./toy/Configurations > /dev/null
echo "copy scripts to ${gate}..."
scp  -r ${tDir}/scripts/bash ${gate}:./toy/scripts > /dev/null