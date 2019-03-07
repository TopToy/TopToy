#!/usr/bin/env bash

source /home/yoni/github.com/JToy/definitions.sh

generate_config_toml() {
    echo \
"title = \"configuration\"
[system]
    n = ${C}
    f = ${F}
    c = 20
    testing = true
    txSize = 0
    cutterBatch = 1

[setting]
    tmo = 1000
    tmoInterval = 100
    rmfBbcConfigPath = \"src/main/resources/bbcConfig\"
    RBConfigPath = \"src/main/resources/RBConfig\"
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
> ${1}/config.toml
n=$((${C} - 1))
for i in `seq 0 ${n}`; do
    echo "      [cluster.s${i}]
            id = ${i}
            ip = \"172.18.0.$((${i} + 3))\"
            port = 30000
            publicKey =\"\"\"MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnhUKrcjsCXCbWHHqH1tk3jYO6yQZVi9vBmZ/rZ
                           N9n8/eSalUt/oheiXaDNXJZERS5Ysl0sRJ0tYjuMM0ShkMJg==\"\"\"
    " >> ${1}/config.toml
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
    > ${conf}/hosts.config
    n=$((${C} - 1))
    for i in `seq 0 ${n}`; do
        echo "${i} 172.18.0.$((${i} + 3)) ${2}" >> ${conf}/hosts.config
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
system.servers.num = ${C}

#Maximum number of faulty replicas
system.servers.f = ${F}

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
system.initial.view = 0" > ${conf}/system.config
for i in `seq 1 $((${C} - 1))`; do
    echo -n ",${i}" >> ${conf}/system.config
done
echo "
" >> ${conf}/system.config
echo \
"#The ID of the trust third party (TTP)
system.ttp.id = 7002

#This sets if the system will function in Byzantine or crash-only mode. Set to "true" to support Byzantine faults
system.bft = true" \
    >> ${conf}/system.config
}

generate_keys() {
    conf=${1}
    mkdir -p ${conf}/keys
    for i in `seq 0 $((${C} - 1))`; do
        cat ${make_script_dir}/keys/publickey > ${conf}/keys/publickey${i}
        cat ${make_script_dir}/keys/privatekey > ${conf}/keys/privatekey${i}
    done
}

generate_log4j() {
    cat ${make_script_dir}/log4j.properties >  ${1}/log4j.properties
}

main(){
    mkdir -p ${1}/bbcConfig
#    mkdir -p ${1}/panicRBConfig
#    mkdir -p ${1}/syncRBConfig
    mkdir -p ${1}/RBConfig
    mkdir -p ${1}/inst

    generate_config_toml ${1}

    generate_hosts ${1}/bbcConfig 12000
#    generate_hosts ${1}/panicRBConfig 11000
#    generate_hosts ${1}/syncRBConfig 13000
    generate_hosts ${1}/RBConfig 13000

    generate_system ${1}/bbcConfig
#    generate_system ${1}/panicRBConfig
#    generate_system ${1}/syncRBConfig
    generate_system ${1}/RBConfig

    generate_keys ${1}/bbcConfig
#    generate_keys ${1}/panicRBConfig
#    generate_keys ${1}/syncRBConfig
    generate_keys ${1}/RBConfig

    generate_log4j ${1}

    cp -r ${make_script_dir}/sslConfig ${1}

}

main ${cdest}
main ${fbdest}
main ${asdest}
main ${byzdest}