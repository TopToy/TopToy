#!/usr/bin/env bash
#echo ${1} | sudo -S mkdir -p /var/lib/apt/lists/il.archive.ubuntu.com_ubuntu_dists_xenial_InRelease

echo ${1} | sudo -S apt-get -y update
echo ${1} | sudo apt-get -y install dialog
export DEBIAN_FRONTEND=noninteractive

#install mvn
echo ${1} | sudo -S apt-cache search maven
echo ${1} | sudo -S apt-get -y install maven

#install git
echo ${1} | sudo -S apt-get -y install git

#install java-10
echo ${1} | sudo -S apt-get -y install software-properties-common
echo ${1} | sudo -S add-apt-repository ppa:linuxuprising/java
echo ${1} | sudo -S apt-get -y update
echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections
echo ${1} | sudo -S apt-get -y install oracle-java10-installer
echo ${1} | sudo -S apt-get -y install oracle-java10-set-default
echo ${1} | sudo -S apt-get clean && rm -rf /tmp/JToy

#clone toy repo
rm -r -f ~/JToy
git clone https://yontyon:y8o9ni89@github.com/yontyon/JToy.git
