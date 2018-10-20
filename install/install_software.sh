#!/usr/bin/env bash
#echo ${1} | sudo -S mkdir -p /var/lib/apt/lists/il.archive.ubuntu.com_ubuntu_dists_xenial_InRelease

sudo -S apt-get -y update
sudo apt-get -y install dialog
DEBIAN_FRONTEND=noninteractive

#install mvn
#sudo -S apt-cache search maven
#sudo -S apt-get -y install maven

#install git
#sudo -S apt-get -y install git

#install java-10
sudo -S apt-get -y install software-properties-common
sudo -S add-apt-repository ppa:linuxuprising/java
sudo -S apt-get -y update
echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections
sudo -S apt-get -y install oracle-java10-installer
sudo -S apt-get -y install oracle-java10-set-default
sudo -S apt-get clean && rm -rf /tmp/JToy

#clone toy repo
#rm -r -f ~/JToy
#git clone https://yontyon:y8o9ni89@github.com/yontyon/JToy.git
