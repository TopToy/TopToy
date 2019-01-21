#!/usr/bin/env bash
base=..
dest=/home/yoni/Desktop/dtoy/configurations/benign

generate_config_toml() {
    echo \
"title = \"configuration\"
[system]
    n = ${1}
    f = ${2}
    c = 20
    testing = true
    txSize = 0
    cutterBatch = 1

[setting]
    tmo = 1000
    tmoInterval = 100
    rmfBbcConfigPath = \"src/main/resources/bbcConfig\"
    panicRBroadcastConfigPath = \"src/main/resources/panicRBConfig\"
    syncRBroadcastConfigPath = \"src/main/resources/syncRBConfig\"
    maxTransactionInBlock = 1000
    caRootPath = \"\"
    fastMode = true

[server]
    privateKey = \"\"\"MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQg/ngTdAL+eZOyh4lilm6djqsl
                    RDHT5C60eLxRcEoNjAGgBwYFK4EEAAqhRANCAASeFQqtyOwJcJtYceofW2TeNg7rJBlW
                    L28GZn+tk32fz95JqVS3+iF6JdoM1clkRFLliyXSxEnS1iO4wzRKGQwm\"\"\"

    publicKey = \"\"\"MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnhUKrcjsCXCbWHHqH1tk3jYO6yQZVi9vBmZ/rZ
                    N9n8/eSalUt/oheiXaDNXJZERS5Ysl0sRJ0tYjuMM0ShkMJg==\"\"\"

    TlsPrivKeyPath = \"src/main/resources/sslConfig/server.pem\"
    TlsCertPath = \"src/main/resources/sslConfig/server.crt\"

[[cluster]]" \
> ${dest}/config.toml
n=$((${1} - 1))
for i in `seq 0 ${n}`; do
    echo "      [cluster.s${i}]
            id = ${i}
            ip = \"172.18.0.$((${i} + 3))\"
            port = 30000
            publicKey =\"\"\"MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnhUKrcjsCXCbWHHqH1tk3jYO6yQZVi9vBmZ/rZ
                           N9n8/eSalUt/oheiXaDNXJZERS5Ysl0sRJ0tYjuMM0ShkMJg==\"\"\"
    " >> ${dest}/config.toml
done
}

generate_hosts(){
    conf=${1}
    echo \
"# Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
#
# Licensed under the Apache License, Version 2.0 (the \"License\");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an \"AS IS\" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This file defines the replicas ids, IPs and ports.
# It is used by the replicas and clients to find connection info
# to the initial replicas.
# The ports defined here are the ports used by clients to communicate
# with the replicas. Additional connections are opened by replicas to
# communicate with each other. This additional connection is opened in the
# next port defined here. For an example, consider the line \"0 127.0.0.1 11000\".
# That means that clients will open a communication channel to replica 0 in
# IP 127.0.0.1 and port 11000. On startup, replicas with id different than 0
# will open a communication channel to replica 0 in port 11001.
# The same holds for replicas 1, 2, 3 ... N.

#server id, address and port (the ids from 0 to n-1 are the service replicas)" \
    > ${dest}/${conf}/hosts.config
    n=$((${2} - 1))
    for i in `seq 0 ${n}`; do
        echo "${i} 172.18.0.$((${i} + 3)) ${3}" >> ${dest}/${conf}/hosts.config
    done
}

generate_system(){
    conf=${1}
    echo -n \
"# Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
#
# Licensed under the Apache License, Version 2.0 (the \"License\");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an \"AS IS\" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

############################################
####### Communication Configurations #######
############################################

#HMAC algorithm used to authenticate messages between processes (HmacMD5 is the default value)
#This parameter is not currently being used being used
#system.authentication.hmacAlgorithm = HmacSHA1

#Specify if the communication system should use a thread to send data (true or false)
system.communication.useSenderThread = true

#Force all processes to use the same public/private keys pair and secret key. This is useful when deploying experiments
#and benchmarks, but must not be used in production systems.
system.communication.defaultkeys = false

############################################
### Replication Algorithm Configurations ###
############################################

#Number of servers in the group
system.servers.num = ${2}

#Maximum number of faulty replicas
system.servers.f = ${3}

#Timeout to asking for a client request
system.totalordermulticast.timeout = 2000


#Maximum batch size (in number of messages)
system.totalordermulticast.maxbatchsize = 400

#Number of nonces (for non-determinism actions) generated
system.totalordermulticast.nonces = 10

#if verification of leader-generated timestamps are increasing
#it can only be used on systems in which the network clocks
#are synchronized
system.totalordermulticast.verifyTimestamps = false

#Quantity of messages that can be stored in the receive queue of the communication system
system.communication.inQueueSize = 500000

# Quantity of messages that can be stored in the send queue of each replica
system.communication.outQueueSize = 500000

#Set to 1 if SMaRt should use signatures, set to 0 if otherwise
system.communication.useSignatures = 1

#Set to 1 if SMaRt should use MAC's, set to 0 if otherwise
system.communication.useMACs = 1

#Set to 1 if SMaRt should use the standard output to display debug messages, set to 0 if otherwise
system.debug = 0

#Print information about the replica when it is shutdown
system.shutdownhook = true

############################################
###### State Transfer Configurations #######
############################################

#Activate the state transfer protocol ('true' to activate, 'false' to de-activate)
system.totalordermulticast.state_transfer = true

#Maximum ahead-of-time message not discarded
system.totalordermulticast.highMark = 10000

#Maximum ahead-of-time message not discarded when the replica is still on EID 0 (after which the state transfer is triggered)
system.totalordermulticast.revival_highMark = 10

#Number of ahead-of-time messages necessary to trigger the state transfer after a request timeout occurs
system.totalordermulticast.timeout_highMark = 200

############################################
###### Log and Checkpoint Configurations ###
############################################

system.totalordermulticast.log = true
system.totalordermulticast.log_parallel = false
system.totalordermulticast.log_to_disk = false
system.totalordermulticast.sync_log = false

#Period at which BFT-SMaRt requests the state to the application (for the state transfer state protocol)
system.totalordermulticast.checkpoint_period = 1000
system.totalordermulticast.global_checkpoint_period = 120000

system.totalordermulticast.checkpoint_to_disk = false
system.totalordermulticast.sync_ckp = false


############################################
###### Reconfiguration Configurations ######
############################################

#Replicas ID for the initial view, separated by a comma.
# The number of replicas in this parameter should be equal to that specified in 'system.servers.num'
system.initial.view = 0" > ${dest}/${conf}/system.config
for i in `seq 1 $((${2} - 1))`; do
    echo -n ",${i}" >> ${dest}/${conf}/system.config
done
echo "
" >> ${dest}/${conf}/system.config
echo \
"#The ID of the trust third party (TTP)
system.ttp.id = 7002

#This sets if the system will function in Byzantine or crash-only mode. Set to "true" to support Byzantine faults
system.bft = true" \
    >> ${dest}/${conf}/system.config
}

generate_keys() {
    conf=${1}
    mkdir -p ${dest}/${conf}/keys
    for i in `seq 0 $((${2} - 1))`; do
        cat ${base}/make_scripts/keys/publickey0 > ${dest}/${conf}/keys/publickey${i}
        cat ${base}/make_scripts/keys/privatekey0 > ${dest}/${conf}/keys/privatekey${i}
    done
}

generate_log4j() {
    cat ${base}/make_scripts/log4j.properties >  ${dest}/log4j.properties
}

generate_benign_inst() {
echo \
"init
serve
wait ${1}
stop
quit" > ${dest}/inst/input.inst
}

main(){
    base=${1}
    mkdir -p ${dest}/bbcConfig
    mkdir -p ${dest}/panicRBConfig
    mkdir -p ${dest}/syncRBConfig
    mkdir -p ${dest}/inst

    generate_config_toml ${2} ${3}

    generate_hosts bbcConfig ${2} 12000
    generate_hosts panicRBConfig ${2} 11000
    generate_hosts syncRBConfig ${2} 13000

    generate_system bbcConfig ${2} ${3}
    generate_system panicRBConfig ${2} ${3}
    generate_system syncRBConfig ${2} ${3}

    generate_keys bbcConfig ${2}
    generate_keys panicRBConfig ${2}
    generate_keys syncRBConfig ${2}

    generate_benign_inst 10

    generate_log4j

    cp -r ${base}/make_scripts/sslConfig ${dest}

}

main ${1} ${2} ${3}