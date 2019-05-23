#!/usr/bin/env bash

. utils/defs.sh

# ${1} - out directory
# ${2} - n
# ${3} - f
# ${4} - ips file
generate_config_toml() {
    echo \
 "title = \"configuration\"
[system]
    n = ${2}
    f = ${3}
    c = 1
    testing = true
    txSize = 0

[setting]
    tmo = 1000
    tmoInterval = 100
    ABConfigPath = \"src/main/resources/ABConfig\"
    maxTransactionInBlock = 1000
    caRootPath = \"\"
    fastMode = true

[server]
    privateKey = \"\"\"MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQg/ngTdAL+eZOyh4lilm6djqsl
                    RDHT5C60eLxRcEoNjAGgBwYFK4EEAAqhRANCAASeFQqtyOwJcJtYceofW2TeNg7rJBlW
                    L28GZn+tk32fz95JqVS3+iF6JdoM1clkRFLliyXSxEnS1iO4wzRKGQwm\"\"\"

    TlsPrivKeyPath = \"src/main/resources/sslConfig/server.pem\"
    TlsCertPath = \"src/main/resources/sslConfig/server.crt\"


[[cluster]]" > ${1}/config.toml

    i=0
    while read line
    do
echo "      [cluster.s${i}]
            id = ${i}
            ip = \"${line}\"
            wrbPort = 30000
            commPort = 30010
            obbcPort = 30020
            publicKey =\"\"\"MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnhUKrcjsCXCbWHHqH1tk3jYO6yQZVi9vBmZ/rZ
                               N9n8/eSalUt/oheiXaDNXJZERS5Ysl0sRJ0tYjuMM0ShkMJg==\"\"\"
    " >> ${1}/config.toml
        i=$((i+1))
    done < ${4}
}

# ${1} - outdir
# ${2} - port
# ${3} - ips file
generate_bftSMaRt_hosts() {
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
        > ${1}/hosts.config

    i=0
    while read line
    do
        echo "${i} ${line} ${2}" >> ${1}/hosts.config
    i=$((i+1))
    done < ${3}
}

# ${1} - outDir
# ${2} - n
# ${3} - f
generate_bftSMaRt_system() {
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
system.initial.view = 0" > ${1}/system.config
    for i in `seq 1 $((${2} - 1))`; do
        echo -n ",${i}" >> ${1}/system.config
    done
    echo "
    " >> ${1}/system.config
    echo \
"#The ID of the trust third party (TTP)
system.ttp.id = 7002

#This sets if the system will function in Byzantine or crash-only mode. Set to \"true\" to support Byzantine faults
system.bft = true" \
        >> ${1}/system.config
}

# ${1} - outdir
# ${2} - indir
# ${3} - n
generate_bftSMaRt_keys() {
    for i in `seq 0 $((${3} - 1))`; do
        cat ${2}/publickey > ${1}/publickey${i}
        cat ${2}/privatekey > ${1}/privatekey${i}
    done
}

main() {
    out_dir=${config_dir}/${n}
    rm -r -f ${out_dir}
    mkdir -p ${out_dir}/sslConfig
    mkdir -p ${out_dir}/inst
    mkdir -p ${out_dir}/ABConfig/keys

    generate_config_toml ${out_dir} ${n} ${f} ${data_dir}/ips.txt
    generate_bftSMaRt_hosts ${out_dir}/ABConfig 13000 ${data_dir}/ips.txt
    generate_bftSMaRt_system ${out_dir}/ABConfig ${n} ${f}
    generate_bftSMaRt_keys ${out_dir}/ABConfig/keys ${data_dir} ${n}
    cp -r ${data_dir}/inst/* ${out_dir}/inst
    cp -r ${data_dir}/sslConfig/* ${out_dir}/sslConfig
    cp ${data_dir}/log4j.properties ${out_dir}
}

main