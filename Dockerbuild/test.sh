#!/bin/bash

set -e
set -o pipefail

sudo gpg --keyserver keyserver.ubuntu.com --recv-keys 129E7CDE
sudo gpg --armor --export 129E7CDE | sudo apt-key add -
sudo bash -c "echo 'deb [arch=armhf] http://s3.amazonaws.com/r2cloud r2cloud main' > /etc/apt/sources.list.d/r2cloud.list"

apt-get update
 
apt install ./*.deb

echo "test success"